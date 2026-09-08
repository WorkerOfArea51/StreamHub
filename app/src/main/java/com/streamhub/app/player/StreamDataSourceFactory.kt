package com.streamhub.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.streamhub.app.data.api.SharedHttpClient

/**
 * High-Performance Persistent Media DataSource Factory for Media3 ExoPlayer.
 *
 * Implements continuous full-file disk caching:
 * - Direct HTTP 206 streaming via [SharedHttpClient.streamingClient] (OkHttp).
 * - Automatic persistence to [StreamCacheManager] disk cache (media_stream_cache) in 4MB chunk fragments.
 * - Resilient fallback via [CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR].
 * - Local offline downloads and resource URIs handled seamlessly by [DefaultDataSource].
 */
@OptIn(UnstableApi::class)
class StreamDataSourceFactory(
    context: Context,
    private val transferListener: androidx.media3.datasource.TransferListener? = null
) : DataSource.Factory {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val USER_AGENT = "StreamHub/4.8 (Linux; Android 14; Mobile)"
        private val VOLATILE_QUERY_PARAMS = setOf(
            "token", "sign", "signature", "sig", "expires", "expiry", "exp",
            "key", "auth", "timestamp", "ts", "hash"
        )

        fun sanitizeCacheKey(uri: android.net.Uri): String {
            val scheme = uri.scheme ?: "https"
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            if (host.isEmpty()) return uri.toString()

            val queryParameterNames = try {
                uri.queryParameterNames
            } catch (_: Exception) {
                emptySet<String>()
            }

            if (queryParameterNames.isEmpty()) {
                return "$scheme://$host$path"
            }

            // Strip only volatile params (tokens, signatures, expiration) to avoid cache pollution,
            // while preserving content-identifying parameters (e.g. video id, filename, resolution)
            val stableParams = queryParameterNames
                .filter { param -> !VOLATILE_QUERY_PARAMS.contains(param.lowercase()) }
                .sorted()

            if (stableParams.isEmpty()) {
                return "$scheme://$host$path"
            }

            val queryBuilder = StringBuilder()
            for (param in stableParams) {
                val values = uri.getQueryParameters(param)
                for (value in values) {
                    if (queryBuilder.isNotEmpty()) queryBuilder.append("&")
                    queryBuilder.append(android.net.Uri.encode(param))
                    queryBuilder.append("=")
                    queryBuilder.append(android.net.Uri.encode(value))
                }
            }

            return "$scheme://$host$path?$queryBuilder"
        }
    }

    private val okHttpDataSourceFactory = OkHttpDataSource.Factory(SharedHttpClient.streamingClient)
        .setUserAgent(USER_AGENT)
        .apply {
            transferListener?.let { setTransferListener(it) }
        }

    private val simpleCache by lazy { StreamCacheManager.getCache(appContext) }

    private val cacheDataSinkFactory by lazy {
        CacheDataSink.Factory()
            .setCache(simpleCache)
            .setFragmentSize(4 * 1024 * 1024L) // 4 MB fine-grained chunk fragments for fast seeking & cache persistence
    }

    private val cacheKeyFactory = CacheKeyFactory { dataSpec ->
        dataSpec.key ?: sanitizeCacheKey(dataSpec.uri)
    }

    private val cachedHttpDataSourceFactory by lazy {
        CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(okHttpDataSourceFactory)
            .setCacheWriteDataSinkFactory(cacheDataSinkFactory)
            .setCacheKeyFactory(cacheKeyFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private val defaultDataSourceFactory by lazy {
        DefaultDataSource.Factory(appContext, cachedHttpDataSourceFactory).apply {
            transferListener?.let { setTransferListener(it) }
        }
    }

    override fun createDataSource(): DataSource {
        return defaultDataSourceFactory.createDataSource()
    }
}
