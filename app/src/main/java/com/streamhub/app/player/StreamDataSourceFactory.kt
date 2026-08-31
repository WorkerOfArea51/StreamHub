package com.streamhub.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.streamhub.app.data.api.SharedHttpClient

/**
 * High-Performance Media DataSource Factory for Media3 ExoPlayer.
 *
 * Uses [SharedHttpClient.streamingClient] (OkHttp) with optimized socket connection pooling,
 * HTTP/1.1 / HTTP/2 pipelining, and resilient TLS connection management.
 */
@OptIn(UnstableApi::class)
class StreamDataSourceFactory(
    context: Context
) : DataSource.Factory {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val USER_AGENT = "StreamHub/4.8 (Linux; Android 14; Mobile)"
    }

    private val okHttpDataSourceFactory = OkHttpDataSource.Factory(SharedHttpClient.streamingClient)
        .setUserAgent(USER_AGENT)

    private val defaultDataSourceFactory by lazy {
        DefaultDataSource.Factory(appContext, okHttpDataSourceFactory)
    }

    override fun createDataSource(): DataSource {
        return defaultDataSourceFactory.createDataSource()
    }
}
