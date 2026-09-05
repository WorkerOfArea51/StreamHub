package com.streamhub.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
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

    private val cachedHttpDataSourceFactory by lazy {
        CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(okHttpDataSourceFactory)
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
