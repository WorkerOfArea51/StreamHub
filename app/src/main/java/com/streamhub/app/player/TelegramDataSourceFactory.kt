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
import kotlinx.coroutines.runBlocking
import org.drinkless.tdlib.TdApi
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * TDLib-Aware ExoPlayer DataSource Factory for Telegram stream links.
 *
 * This factory creates the appropriate DataSource based on the URL type:
 *
 *  1. **Local file path / TDLib Stream** (from TdLibMediaProvider):
 *     → [TdLibStreamingDataSource] — reads directly from disk using [RandomAccessFile]
 *       and seamlessly requests missing byte-range chunks from TDLib when seeking!
 *
 *  2. **HTTP/HTTPS URL** (direct video link):
 *     → [CacheDataSource] wrapping [DefaultHttpDataSource] with Range request support.
 *
 * Thread Safety:
 *  - Each [createDataSource] call returns a new, independent DataSource instance.
 */
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
                val isLocal = scheme == LOCAL_FILE_SCHEME || scheme == null || uri.path?.startsWith("/") == true

                currentSource = if (isLocal) {
                    tdlibSource
                } else {
                    httpSource
                }
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
 * Supports on-demand byte seeking: if ExoPlayer requests a byte offset beyond the current file
 * on disk, this DataSource triggers [TdApi.DownloadFile] with the sought offset to download that
 * chunk immediately, enabling flawless seeking across large Telegram videos!
 */
@OptIn(UnstableApi::class)
class TdLibStreamingDataSource : DataSource {

    companion object {
        private const val TAG = "TdLibStreamingDS"
    }

    private var randomAccessFile: RandomAccessFile? = null
    private var file: File? = null
    private var fileId: Int? = null
    private var totalFileSize: Long = 0L
    private var currentPosition: Long = 0L
    private var bytesRemaining: Long = 0L
    private var openedUri: Uri? = null

    override fun addTransferListener(transferListener: TransferListener) {}

    override fun open(dataSpec: DataSpec): Long {
        val path = dataSpec.uri.path ?: throw IOException("No file path in URI: ${dataSpec.uri}")
        val f = File(path)
        file = f
        openedUri = dataSpec.uri

        fileId = TdLibMediaProvider.getFileIdForPath(path)
        totalFileSize = TdLibMediaProvider.getTotalSizeForPath(path) ?: f.length().coerceAtLeast(1L)
        val position = dataSpec.position
        currentPosition = position

        val fId = fileId
        // If seeking beyond currently downloaded bytes on disk, request TDLib download chunk from that offset!
        if (fId != null && position >= f.length() && position < totalFileSize) {
            Log.i(TAG, "Seeking to unbuffered offset $position (disk: ${f.length()}, total: $totalFileSize)")
            try {
                runBlocking {
                    TdLibManager.send(TdApi.DownloadFile(fId, 32, position, 0, false))
                    var waitMs = 0
                    while (waitMs < 6_000 && (!f.exists() || f.length() <= position)) {
                        kotlinx.coroutines.delay(80)
                        waitMs += 80
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error requesting TDLib offset chunk: ${e.message}")
            }
        }

        if (!f.exists()) {
            throw IOException("File does not exist: $path")
        }

        randomAccessFile = RandomAccessFile(f, "r")
        if (position > 0 && position <= f.length()) {
            randomAccessFile!!.seek(position)
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

        // If reading ahead of downloaded bytes, wait up to 8s for TDLib download chunks to arrive
        var currentFileLen = f.length()
        var waitCount = 0
        while (currentPosition + toRead > currentFileLen && currentPosition < totalFileSize && waitCount < 100) {
            val fId = fileId
            if (fId == null) break
            try {
                Thread.sleep(60)
            } catch (_: InterruptedException) {
                break
            }
            waitCount++
            currentFileLen = f.length()
        }

        if (currentPosition >= currentFileLen) {
            return if (currentPosition >= totalFileSize) C.RESULT_END_OF_INPUT else 0
        }

        val available = (currentFileLen - currentPosition).coerceAtLeast(0L).toInt()
        if (available <= 0) {
            return if (currentPosition >= totalFileSize) C.RESULT_END_OF_INPUT else 0
        }

        val actualReadSize = kotlin.math.min(toRead, available)
        raf.seek(currentPosition) // Always synchronize exact file pointer with currentPosition
        val bytesRead = raf.read(buffer, offset, actualReadSize)
        if (bytesRead > 0) {
            currentPosition += bytesRead
            bytesRemaining -= bytesRead
            return bytesRead
        }

        return if (currentPosition >= totalFileSize) C.RESULT_END_OF_INPUT else 0
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
    }

    override fun getResponseHeaders(): Map<String, List<String>> = emptyMap()
}
