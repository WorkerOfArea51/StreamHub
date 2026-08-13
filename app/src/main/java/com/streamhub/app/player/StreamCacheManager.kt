package com.streamhub.app.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

@OptIn(UnstableApi::class)
object StreamCacheManager {
    private const val TAG = "StreamCacheManager"
    private var simpleCache: SimpleCache? = null
    private var databaseProvider: StandaloneDatabaseProvider? = null
    private var cacheDir: File? = null
    private var activeReaderCount: Int = 0
    private val cacheLock = ReentrantReadWriteLock()

    fun getCache(context: Context): SimpleCache {
        return cacheLock.write {
            if (simpleCache == null) {
                cacheDir = File(context.cacheDir, "media_stream_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024L) // 500 MB cache
                databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
                simpleCache = SimpleCache(cacheDir!!, evictor, databaseProvider!!)
            }
            simpleCache!!
        }
    }

    fun acquireReader() {
        cacheLock.write {
            activeReaderCount++
        }
    }

    fun releaseReader() {
        cacheLock.write {
            if (activeReaderCount > 0) activeReaderCount--
        }
    }

    fun clearCache(context: Context): Boolean {
        return cacheLock.write {
            if (activeReaderCount > 0) {
                Log.w(TAG, "Cannot clear cache while player is actively reading ($activeReaderCount active readers)")
                return@write false
            }
            val cache = simpleCache
            simpleCache = null
            databaseProvider = null
            try { cache?.release() } catch (e: Exception) { Log.e(TAG, "Failed to release cache", e) }
            try { (cacheDir ?: File(context.cacheDir, "media_stream_cache")).deleteRecursively() } catch (e: Exception) { Log.e(TAG, "Failed to delete cache dir", e) }
            cacheDir = null
            true
        }
    }

    fun release() {
        cacheLock.write {
            val cache = simpleCache
            simpleCache = null
            databaseProvider = null
            try {
                cache?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release simpleCache", e)
            }
        }
    }
}
