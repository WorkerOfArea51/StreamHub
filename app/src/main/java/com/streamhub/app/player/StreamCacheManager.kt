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
    private var databaseProvider: StandaloneDatabaseProvider? = null
    private var cacheDir: File? = null
    private var activeReaderCount: Int = 0

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            cacheDir = File(context.cacheDir, "media_stream_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024L) // 500 MB cache
            databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
            simpleCache = SimpleCache(cacheDir!!, evictor, databaseProvider!!)
        }
        return simpleCache!!
    }

    @Synchronized
    fun acquireReader() {
        activeReaderCount++
    }

    @Synchronized
    fun releaseReader() {
        if (activeReaderCount > 0) activeReaderCount--
    }

    @Synchronized
    fun clearCache(context: Context) {
        if (activeReaderCount > 0) {
            Log.w(TAG, "Cannot clear cache while player is actively reading ($activeReaderCount active readers)")
            return
        }
        val cache = simpleCache
        simpleCache = null
        databaseProvider = null
        try { cache?.release() } catch (e: Exception) { Log.e(TAG, "Failed to release cache", e) }
        try { (cacheDir ?: File(context.cacheDir, "media_stream_cache")).deleteRecursively() } catch (e: Exception) { Log.e(TAG, "Failed to delete cache dir", e) }
        cacheDir = null
    }
}
