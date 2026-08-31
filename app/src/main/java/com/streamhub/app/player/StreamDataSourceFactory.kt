package com.streamhub.app.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.streamhub.app.data.api.SharedHttpClient
import java.io.IOException

/**
 * High-Performance Resilient Media DataSource Factory for Media3 ExoPlayer.
 *
 * Wraps ExoPlayer's [DefaultDataSource.Factory] with a dedicated, fault-tolerant
 * HTTP streaming pipeline:
 * 1. Primary: Zero-timeout streaming via [SharedHttpClient.streamingClient] (OkHttp).
 * 2. Failover: Automatic fallback to [DefaultHttpDataSource] with cross-protocol redirect support.
 * 3. In-flight byte-range auto-recovery: Transparent reconnection when sockets drop during progressive streaming.
 * 4. Local files & content providers: Automatically handled via Media3's [DefaultDataSource] (file://, content://, rawresource://).
 */
@OptIn(UnstableApi::class)
class StreamDataSourceFactory(
    context: Context
) : DataSource.Factory {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "StreamDataSourceFactory"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private val DEFAULT_HEADERS = mapOf(
            "Accept" to "*/*",
            "Accept-Encoding" to "identity",
            "Connection" to "keep-alive"
        )
    }

    private val okHttpDataSourceFactory = OkHttpDataSource.Factory(SharedHttpClient.streamingClient)
        .setUserAgent(USER_AGENT)
        .setDefaultRequestProperties(DEFAULT_HEADERS)

    private val defaultHttpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(USER_AGENT)
        .setConnectTimeoutMs(45_000)
        .setReadTimeoutMs(0) // Infinite read timeout for progressive media streams
        .setAllowCrossProtocolRedirects(true)
        .setKeepPostFor302Redirects(true)
        .setDefaultRequestProperties(DEFAULT_HEADERS)

    private val resilientHttpDataSourceFactory = DataSource.Factory {
        object : HttpDataSource {
            private var currentSource: DataSource? = null
            private var transferListener: TransferListener? = null
            private var activeDataSpec: DataSpec? = null
            private var bytesReadTotal: Long = 0L

            override fun addTransferListener(transferListener: TransferListener) {
                this.transferListener = transferListener
            }

            override fun open(dataSpec: DataSpec): Long {
                try { currentSource?.close() } catch (_: Exception) {}
                activeDataSpec = dataSpec
                bytesReadTotal = 0L

                // Remote HTTP/HTTPS Stream: Primary OkHttp streaming (async socket pipeline), fallback to DefaultHttpDataSource
                val okHttpSource = okHttpDataSourceFactory.createDataSource()
                currentSource = okHttpSource
                transferListener?.let { currentSource?.addTransferListener(it) }
                return try {
                    currentSource!!.open(dataSpec)
                } catch (e: Exception) {
                    Log.w(TAG, "OkHttp open failed for ${dataSpec.uri}, failing over to DefaultHttpDataSource: ${e.message}")
                    val defaultHttpSource = defaultHttpDataSourceFactory.createDataSource()
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
                    if (activeDataSpec != null) {
                        val resumeOffset = activeDataSpec!!.position + bytesReadTotal
                        Log.w(TAG, "Remote stream read dropped after $bytesReadTotal bytes at offset $resumeOffset. Attempting auto-reconnect: ${e.message}")
                        try {
                            try { source.close() } catch (_: Exception) {}
                            val fallbackSource = defaultHttpDataSourceFactory.createDataSource()
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

            override fun getResponseHeaders(): Map<String, List<String>> {
                return (currentSource as? HttpDataSource)?.responseHeaders ?: emptyMap()
            }

            override fun getResponseCode(): Int {
                return (currentSource as? HttpDataSource)?.responseCode ?: 200
            }

            private val requestProperties = mutableMapOf<String, String>()

            override fun setRequestProperty(name: String, value: String) {
                requestProperties[name] = value
                (currentSource as? HttpDataSource)?.setRequestProperty(name, value)
            }

            override fun clearRequestProperty(name: String) {
                requestProperties.remove(name)
                (currentSource as? HttpDataSource)?.clearRequestProperty(name)
            }

            override fun clearAllRequestProperties() {
                requestProperties.clear()
                (currentSource as? HttpDataSource)?.clearAllRequestProperties()
            }

            override fun close() {
                try {
                    currentSource?.close()
                } finally {
                    currentSource = null
                    activeDataSpec = null
                    bytesReadTotal = 0L
                }
            }
        }
    }

    private val simpleCache by lazy { StreamCacheManager.getCache(appContext) }

    private val cacheDataSinkFactory by lazy {
        CacheDataSink.Factory()
            .setCache(simpleCache)
            .setFragmentSize(4 * 1024 * 1024L) // 4 MB fine-grained chunk fragments for fast seeking & cache persistence
    }

    private val cachedHttpDataSourceFactory by lazy {
        CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(resilientHttpDataSourceFactory)
            .setCacheWriteDataSinkFactory(cacheDataSinkFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private val defaultDataSourceFactory by lazy {
        DefaultDataSource.Factory(appContext, cachedHttpDataSourceFactory)
    }

    override fun createDataSource(): DataSource {
        return defaultDataSourceFactory.createDataSource()
    }
}
