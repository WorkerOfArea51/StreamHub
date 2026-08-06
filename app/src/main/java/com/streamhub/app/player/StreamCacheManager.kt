package com.streamhub.app.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
object StreamCacheManager {
    private const val TAG = "StreamCacheManager"
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "media_stream_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024L) // 500 MB cache
            val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    /**
     * FIX #12: Null out simpleCache FIRST to prevent stale references if release() throws.
     */
    @Synchronized
    fun clearCache(context: Context) {
        val cache = simpleCache
        simpleCache = null
        try {
            cache?.release()
            val cacheDir = File(context.cacheDir, "media_stream_cache")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear media stream cache", e)
        }
    }
}
