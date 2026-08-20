package com.streamhub.app.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import com.streamhub.app.data.telegram.TdLibManager
import com.streamhub.app.data.telegram.TdLibMediaProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.drinkless.tdlib.TdApi
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

@OptIn(UnstableApi::class)
class TelegramDataSourceFactory(
    context: Context
) : DataSource.Factory {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "TelegramDataSourceFactory"
        const val LOCAL_FILE_SCHEME = "file"
    }

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setKeepPostFor302Redirects(true)
        .setConnectTimeoutMs(25_000)
        .setReadTimeoutMs(25_000)
        .setDefaultRequestProperties(mapOf("Accept-Ranges" to "bytes"))
        .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

    private val cacheDataSourceFactory: CacheDataSource.Factory by lazy {
        val cache = StreamCacheManager.getCache(appContext)
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    override fun createDataSource(): DataSource {
        val httpSource = cacheDataSourceFactory.createDataSource()
        val tdlibSource = TdLibStreamingDataSource()

        return object : DataSource {
            private var currentSource: DataSource? = null
            private var transferListener: TransferListener? = null

            override fun addTransferListener(transferListener: TransferListener) {
                this.transferListener = transferListener
                httpSource.addTransferListener(transferListener)
                tdlibSource.addTransferListener(transferListener)
            }

            override fun open(dataSpec: DataSpec): Long {
                try { currentSource?.close() } catch (_: Exception) {}
                val uri = dataSpec.uri
                val scheme = uri.scheme?.lowercase()

                // FIX: Use scheme-based routing instead of path-prefix check.
                // HTTP URLs like https://host/path.mp4 have path "/path.mp4" which starts with "/",
                // which previously misrouted them to the TDLib local-file source.
                val isLocal = scheme == LOCAL_FILE_SCHEME || scheme == null

                currentSource = if (isLocal) tdlibSource else httpSource
                transferListener?.let { currentSource?.addTransferListener(it) }
                return currentSource!!.open(dataSpec)
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                return currentSource?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
            }

            override fun getUri(): Uri? = currentSource?.uri

            override fun close() {
                try { currentSource?.close() } catch (_: Exception) {}
                currentSource = null
            }

            override fun getResponseHeaders(): Map<String, List<String>> {
                return currentSource?.responseHeaders ?: emptyMap()
            }
        }
    }
}

/**
 * High-performance streaming DataSource that reads from a local or actively downloading TDLib file.
 *
 * FIX: Replaced runBlocking+Thread.sleep polling with a coroutine-based async chunk fetcher.
 * The read() call now blocks the ExoPlayer load thread for at most a bounded wait time
 * using Object.wait()/notifyAll() — never runBlocking.
 */
@OptIn(UnstableApi::class)
class TdLibStreamingDataSource : DataSource {

    companion object {
        private const val TAG = "TdLibStreamingDS"
        private const val CHUNK_POLL_INTERVAL_MS = 50L
        private const val MAX_WAIT_FOR_CHUNK_MS = 30_000L // 30s hard ceiling per read
        private const val DOWNLOAD_CHUNK_PRIORITY = 32
        private const val DOWNLOAD_CHUNK_SIZE = 0L // 0L = TDLib default (1 MB)
    }

    private val monitorLock = Any()
    private var randomAccessFile: RandomAccessFile? = null
    private var file: File? = null
    private var fileId: Int? = null
    private var totalFileSize: Long = 0L
    private var currentPosition: Long = 0L
    private var bytesRemaining: Long = 0L
    private var openedUri: Uri? = null
    private val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var fetchJob: Job? = null

    override fun addTransferListener(transferListener: TransferListener) {}

    override fun open(dataSpec: DataSpec): Long {
        val path = dataSpec.uri.path ?: throw IOException("No file path in URI: ${dataSpec.uri}")
        val f = File(path)
        file = f
        openedUri = dataSpec.uri

        fileId = TdLibMediaProvider.getFileIdForPath(path)
        // FIX: If file doesn't exist yet, don't coerce to 1L — get the real size from TDLib or fail fast.
        val tdlibSize = TdLibMediaProvider.getTotalSizeForPath(path)
        totalFileSize = tdlibSize ?: if (f.exists()) f.length() else 0L
        if (totalFileSize <= 0L) {
            throw IOException("Cannot determine file size for $path — TDLib file metadata not available yet")
        }

        val position = dataSpec.position
        currentPosition = position

        val fId = fileId
        // FIX: When opening/seeking, request TDLib download continuously from that offset (limit=0L).
        if (fId != null && position < totalFileSize) {
            Log.i(TAG, "Opening TDLib stream at offset $position (disk: ${f.length()}, total: $totalFileSize)")
            try {
                fetchScope.launch {
                    TdLibManager.send(
                        TdApi.DownloadFile(fId, DOWNLOAD_CHUNK_PRIORITY, position, 0L, false)
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error requesting TDLib offset stream: ${e.message}")
            }
        }

        if (!f.exists()) {
            throw IOException("File does not exist: $path")
        }

        randomAccessFile = RandomAccessFile(f, "r")
        if (position > 0) {
            try {
                randomAccessFile!!.seek(position)
            } catch (e: Exception) {
                Log.w(TAG, "Initial seek to $position: ${e.message}")
            }
        }

        val length = dataSpec.length
        bytesRemaining = if (length != C.LENGTH_UNSET.toLong()) {
            length
        } else {
            (totalFileSize - position).coerceAtLeast(0L)
        }

        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining <= 0L) return C.RESULT_END_OF_INPUT
        val raf = randomAccessFile ?: throw IOException("DataSource not open")
        val f = file ?: throw IOException("File reference lost")

        val toRead = kotlin.math.min(length.toLong(), bytesRemaining).toInt()
        val fId = fileId
        var currentFileLen = f.length()
        val waitDeadlineMs = System.currentTimeMillis() + MAX_WAIT_FOR_CHUNK_MS

        while (currentPosition + toRead > currentFileLen && currentPosition < totalFileSize) {
            if (fId == null) break
            val remainingWait = waitDeadlineMs - System.currentTimeMillis()
            if (remainingWait <= 0L) {
                Log.w(TAG, "Chunk wait exceeded ${MAX_WAIT_FOR_CHUNK_MS}ms at position $currentPosition — aborting read")
                throw IOException("TDLib chunk wait timed out at position $currentPosition")
            }

            // Kick off an async fetch to keep downloading forward from currentPosition
            ensureFetchRunning(fId, currentPosition)

            synchronized(monitorLock) {
                val smallWait = kotlin.math.min(remainingWait, 100L)
                (monitorLock as java.lang.Object).wait(smallWait)
            }
            currentFileLen = f.length()
        }

        if (currentPosition >= currentFileLen) {
            return if (currentPosition >= totalFileSize) C.RESULT_END_OF_INPUT
            else throw IOException("Unexpected end of downloaded chunk at $currentPosition")
        }

        val available = (currentFileLen - currentPosition).coerceAtLeast(0L).toInt()
        if (available <= 0) {
            return if (currentPosition >= totalFileSize) C.RESULT_END_OF_INPUT
            else throw IOException("No bytes available at position $currentPosition")
        }

        val actualReadSize = kotlin.math.min(toRead, available)
        raf.seek(currentPosition)
        val bytesRead = raf.read(buffer, offset, actualReadSize)
        if (bytesRead > 0) {
            currentPosition += bytesRead
            bytesRemaining -= bytesRead
            return bytesRead
        }

        return if (currentPosition >= totalFileSize) C.RESULT_END_OF_INPUT
        else throw IOException("RAF read returned 0 at position $currentPosition")
    }

    /**
     * Ensure TDLib has an active DownloadFile request for the current read position.
     */
    private fun ensureFetchRunning(fId: Int, position: Long) {
        if (fetchJob?.isActive == true) return
        fetchJob = fetchScope.launch {
            try {
                TdLibManager.send(
                    TdApi.DownloadFile(fId, DOWNLOAD_CHUNK_PRIORITY, position, 0L, false)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Async TDLib stream fetch failed at $position: ${e.message}")
            }
        }
    }

    override fun getUri(): Uri? = openedUri

    override fun close() {
        try {
            randomAccessFile?.close()
        } catch (_: Exception) {}
        randomAccessFile = null
        file = null
        openedUri = null
        bytesRemaining = 0L
        fetchJob?.cancel()
        fetchJob = null
    }

    override fun getResponseHeaders(): Map<String, List<String>> = emptyMap()
}
