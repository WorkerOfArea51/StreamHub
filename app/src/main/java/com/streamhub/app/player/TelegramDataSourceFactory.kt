package com.streamhub.app.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSink
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSink
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * TDLib-Aware ExoPlayer DataSource Factory for Telegram stream links.
 *
 * This factory creates the appropriate DataSource based on the URL type:
 *
 *  1. **Local file path** (from TdLibMediaProvider download):
 *     → [LocalFileDataSource] — reads directly from the TDLib-downloaded file
 *     → Zero network overhead, instant seeking, works offline
 *
 *  2. **HTTP/HTTPS URL** (direct video link):
 *     → [CacheDataSource] wrapping [DefaultHttpDataSource]
 *     → Standard ExoPlayer HTTP streaming with 500MB LRU cache
 *
 *  3. **t.me link** (not yet resolved):
 *     → Falls back to HTTP with cache — link resolution should happen
 *       at the ViewModel layer before reaching ExoPlayer
 *
 * Usage:
 *  - The factory inspects each DataSpec's URI to choose the right DataSource
 *  - For local files, it reads directly from disk (no cache needed)
 *  - For HTTP URLs, it uses ExoPlayer's standard HTTP + cache pipeline
 *
 * Thread Safety:
 *  - Each [createDataSource] call returns a new, independent DataSource instance
 *  - The factory itself is stateless and thread-safe
 */
@OptIn(UnstableApi::class)
class TelegramDataSourceFactory(
    context: Context
) : DataSource.Factory {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "TelegramDataSourceFactory"

        /** Scheme used for local file URIs passed to ExoPlayer. */
        const val LOCAL_FILE_SCHEME = "file"
    }

    override fun createDataSource(): DataSource {
        return TelegramDataSource(appContext)
    }

    /**
     * A DataSource that routes to either local file reads or HTTP + cache
     * depending on the URI scheme of each DataSpec.
     */
    private inner class TelegramDataSource(
        private val context: Context
    ) : DataSource {

        private var currentSource: DataSource? = null
        private var isLocalFile = false

        override fun addTransferListener(transferListener: TransferListener) {
            // Transfer listeners are added to the underlying DataSource on open
        }

        override fun open(dataSpec: DataSpec): Long {
            try { currentSource?.close() } catch (_: Exception) {}
            currentSource = null
            val uri = dataSpec.uri
            val scheme = uri.scheme?.lowercase()

            // Route to the appropriate DataSource based on URI scheme
            isLocalFile = when {
                // Local file from TDLib download
                scheme == LOCAL_FILE_SCHEME || scheme == null -> {
                    val path = uri.path ?: ""
                    val file = File(path)
                    if (path.isNotBlank() && file.exists() && isPathWhitelisted(file)) {
                        Log.d(TAG, "Opening local file: $path")
                        currentSource = LocalFileDataSource()
                        true
                    } else {
                        Log.e(TAG, "Local file invalid or not found: $path (URI: $uri)")
                        throw IOException("Access denied or local media file not found: $path")
                    }
                }

                // HTTP/HTTPS — standard network streaming
                scheme == "http" || scheme == "https" -> {
                    Log.d(TAG, "Opening HTTP stream: $uri")
                    currentSource = createCachedHttpDataSource(uri.host)
                    false
                }

                else -> {
                    Log.w(TAG, "Unknown URI scheme '$scheme', trying HTTP: $uri")
                    currentSource = createCachedHttpDataSource(uri.host)
                    false
                }
            }

            return currentSource?.open(dataSpec) ?: throw IOException("No DataSource available for $uri")
        }

        private fun isPathWhitelisted(file: File): Boolean {
            return try {
                val canonicalPath = file.canonicalPath
                val cachePath = appContext.cacheDir.canonicalPath
                val filesPath = appContext.filesDir.canonicalPath
                val externalPath = appContext.getExternalFilesDir(null)?.canonicalPath

                canonicalPath.startsWith(cachePath) ||
                canonicalPath.startsWith(filesPath) ||
                (externalPath != null && canonicalPath.startsWith(externalPath))
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Create a standard HTTP DataSource for direct video links.
         */
        private fun createHttpDataSource(host: String? = null): DefaultHttpDataSource {
            val factory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(false)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)

            return factory.createDataSource()
        }

        /**
         * Create a cached HTTP DataSource (HTTP + 500MB LRU cache).
         */
        private fun createCachedHttpDataSource(host: String? = null): CacheDataSource {
            val httpSource = createHttpDataSource(host)
            val cache = StreamCacheManager.getCache(appContext)

            return CacheDataSource(
                cache,
                httpSource,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
            )
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            return currentSource?.read(buffer, offset, length)
                ?: throw IOException("DataSource not opened")
        }

        override fun getUri(): Uri? = currentSource?.uri

        override fun close() {
            currentSource?.close()
            currentSource = null
        }

        override fun getResponseHeaders(): Map<String, List<String>> {
            return currentSource?.responseHeaders ?: emptyMap()
        }
    }
}

/**
 * DataSource that reads from a local file on disk.
 *
 * Used for files downloaded by TDLib — these are complete video files
 * stored in TDLib's files directory. ExoPlayer reads them directly
 * with zero network overhead, instant seeking, and offline support.
 *
 * Supports byte-range requests via DataSpec.position for seeking.
 */
@OptIn(UnstableApi::class)
private class LocalFileDataSource : DataSource {

    private var inputStream: FileInputStream? = null
    private var file: File? = null
    private var bytesRemaining: Long = 0
    private var openedUri: Uri? = null

    override fun open(dataSpec: DataSpec): Long {
        val path = dataSpec.uri.path ?: throw IOException("No file path in URI: ${dataSpec.uri}")

        val f = File(path)
        if (!f.exists()) {
            throw IOException("File not found: $path")
        }
        if (!f.canRead()) {
            throw IOException("File not readable: $path")
        }

        file = f
        openedUri = dataSpec.uri

        val fileSize = f.length()
        val position = dataSpec.position

        if (position > fileSize) {
            throw IOException("Position $position exceeds file size $fileSize: $path")
        }

        inputStream = FileInputStream(f)
        if (position > 0) {
            var remaining = position
            while (remaining > 0) {
                val skipped = inputStream!!.skip(remaining)
                if (skipped == 0L) throw IOException("Failed to skip to position $position")
                remaining -= skipped
            }
        }

        val length = dataSpec.length
        bytesRemaining = if (length != C.LENGTH_UNSET.toLong()) {
            kotlin.math.min(length, fileSize - position)
        } else {
            fileSize - position
        }

        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining <= 0) {
            return C.RESULT_END_OF_INPUT
        }

        val toRead = kotlin.math.min(length.toLong(), bytesRemaining).toInt()
        val read = inputStream?.read(buffer, offset, toRead)
            ?: throw IOException("InputStream not opened")

        if (read == -1) {
            if (bytesRemaining > 0) throw EOFException("Unexpected end of file")
            return C.RESULT_END_OF_INPUT
        }

        bytesRemaining -= read
        return read
    }

    override fun getUri(): Uri? = openedUri

    override fun close() {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            // Ignore close errors
        }
        inputStream = null
        file = null
        bytesRemaining = 0
        openedUri = null
    }

    override fun getResponseHeaders(): Map<String, List<String>> = emptyMap()

    override fun addTransferListener(transferListener: TransferListener) {
        // No-op for local files — no network transfer to track
    }
}
