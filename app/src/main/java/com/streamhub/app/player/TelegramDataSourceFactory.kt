package com.streamhub.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource

@OptIn(UnstableApi::class)
class TelegramDataSourceFactory(
    private val context: Context,
    private val botToken: String? = null
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        if (!botToken.isNull_Empty()) {
            httpDataSourceFactory.setDefaultRequestProperties(
                mapOf("Authorization" to "Bearer $botToken")
            )
        }

        val upstreamFactory = httpDataSourceFactory.createDataSource()
        val cache = StreamCacheManager.getCache(context)

        return CacheDataSource(
            cache,
            upstreamFactory,
            CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
        )
    }
}

private fun String?.isNull_Empty(): Boolean = this == null || this.isEmpty()
