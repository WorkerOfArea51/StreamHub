package com.streamhub.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource

/**
 * Custom ExoPlayer DataSource Factory for Telegram stream links.
 *
 * FIX #11: Stores appContext (applicationContext) to prevent Activity context leaks.
 */
@OptIn(UnstableApi::class)
class TelegramDataSourceFactory(
    context: Context,
    private val botToken: String? = null
) : DataSource.Factory {

    private val appContext: Context = context.applicationContext

    override fun createDataSource(): DataSource {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        // FIX #22: Use stdlib isNullOrEmpty() instead of custom shadowing extension
        // FIX #21: Document authorization header redirect behavior
        if (!botToken.isNullOrEmpty()) {
            // Note: setDefaultRequestProperties applies to initial requests and cross-protocol redirects.
            httpDataSourceFactory.setDefaultRequestProperties(
                mapOf("Authorization" to "Bearer $botToken")
            )
        }

        val upstreamFactory = httpDataSourceFactory.createDataSource()
        val cache = StreamCacheManager.getCache(appContext)

        return CacheDataSource(
            cache,
            upstreamFactory,
            CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
        )
    }
}
