package com.streamhub.app.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.streamhub.app.data.api.SharedHttpClient
import java.io.IOException

/**
 * High-Performance Resilient HTTP & Local File DataSource Factory for Media3 ExoPlayer.
 *
 * Key Capabilities:
 * 1. Zero-timeout streaming via dedicated [SharedHttpClient.streamingClient] (prevents socket drops on buffer stall).
 * 2. Secondary failover to [DefaultHttpDataSource] with cross-protocol redirect support.
 * 3. In-flight transparent byte-range auto-recovery on socket disconnection during read operations.
 * 4. Zero disk contention progressive streaming for multi-gigabyte media files.
 * 5. Full local offline file playback via [FileDataSource].
 */
@OptIn(UnstableApi::class)
class StreamDataSourceFactory(
    context: Context
) : DataSource.Factory {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "StreamDataSourceFactory"
        const val LOCAL_FILE_SCHEME = "file"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private val DEFAULT_HEADERS = mapOf(
            "Accept" to "*/*",
            "Connection" to "keep-alive"
        )
    }

    private val okHttpDataSourceFactory = OkHttpDataSource.Factory(SharedHttpClient.streamingClient)
        .setUserAgent(USER_AGENT)
        .setDefaultRequestProperties(DEFAULT_HEADERS)

    private val defaultHttpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(USER_AGENT)
        .setConnectTimeoutMs(25_000)
        .setReadTimeoutMs(0) // Infinite read timeout for progressive media streams
        .setAllowCrossProtocolRedirects(true)
        .setKeepPostFor302Redirects(true)
        .setDefaultRequestProperties(DEFAULT_HEADERS)

    private val fileDataSourceFactory = FileDataSource.Factory()

    override fun createDataSource(): DataSource {
        val okHttpSource = okHttpDataSourceFactory.createDataSource()
        val defaultHttpSource = defaultHttpDataSourceFactory.createDataSource()
        val fileSource = fileDataSourceFactory.createDataSource()

        return object : DataSource {
            private var currentSource: DataSource? = null
            private var transferListener: TransferListener? = null
            private var activeDataSpec: DataSpec? = null
            private var bytesReadTotal: Long = 0L
            private var isLocal: Boolean = false

            override fun addTransferListener(transferListener: TransferListener) {
                this.transferListener = transferListener
                okHttpSource.addTransferListener(transferListener)
                defaultHttpSource.addTransferListener(transferListener)
                fileSource.addTransferListener(transferListener)
            }

            override fun open(dataSpec: DataSpec): Long {
                try { currentSource?.close() } catch (_: Exception) {}
                activeDataSpec = dataSpec
                bytesReadTotal = 0L

                val uri = dataSpec.uri
                val scheme = uri.scheme?.lowercase()
                isLocal = scheme == LOCAL_FILE_SCHEME || scheme == null || uri.path?.startsWith("/") == true

                if (isLocal) {
                    currentSource = fileSource
                    transferListener?.let { currentSource?.addTransferListener(it) }
                    return currentSource!!.open(dataSpec)
                }

                // Remote HTTP/HTTPS Stream: Try OkHttp streaming first, fallback to DefaultHttpDataSource
                currentSource = okHttpSource
                transferListener?.let { currentSource?.addTransferListener(it) }
                return try {
                    currentSource!!.open(dataSpec)
                } catch (e: Exception) {
                    Log.w(TAG, "OkHttp stream open failed for ${dataSpec.uri}, failing over to DefaultHttpDataSource: ${e.message}")
                    currentSource = defaultHttpSource
                    transferListener?.let { currentSource?.addTransferListener(it) }
                    currentSource!!.open(dataSpec)
                }
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                val source = currentSource ?: return C.RESULT_END_OF_INPUT
                return try {
                    val readBytes = source.read(buffer, offset, length)
                    if (readBytes > 0) {
                        bytesReadTotal += readBytes
                    }
                    readBytes
                } catch (e: IOException) {
                    // Transparent in-flight reconnection for remote HTTP media streams
                    if (!isLocal && activeDataSpec != null) {
                        val resumeOffset = activeDataSpec!!.position + bytesReadTotal
                        Log.w(TAG, "Remote stream read dropped after $bytesReadTotal bytes at offset $resumeOffset. Attempting auto-reconnect: ${e.message}")
                        try {
                            try { source.close() } catch (_: Exception) {}
                            val fallbackSource = if (source === okHttpSource) defaultHttpSource else okHttpSource
                            val resumeSpec = activeDataSpec!!.buildUpon().setPosition(resumeOffset).build()
                            fallbackSource.open(resumeSpec)
                            currentSource = fallbackSource
                            transferListener?.let { currentSource?.addTransferListener(it) }
                            val readBytes = fallbackSource.read(buffer, offset, length)
                            if (readBytes > 0) {
                                bytesReadTotal += readBytes
                            }
                            return readBytes
                        } catch (reconnectEx: Exception) {
                            Log.e(TAG, "In-flight stream auto-recovery failed", reconnectEx)
                            throw e
                        }
                    }
                    throw e
                }
            }

            override fun getUri(): Uri? = currentSource?.uri ?: activeDataSpec?.uri

            override fun close() {
                try {
                    currentSource?.close()
                } finally {
                    currentSource = null
                    activeDataSpec = null
                    bytesReadTotal = 0L
                    isLocal = false
                }
            }
        }
    }
}
