package com.streamhub.app.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@OptIn(UnstableApi::class)
object StreamCacheManager {
    private const val TAG = "StreamCacheManager"
    private const val CACHE_SIZE_BYTES = 2048L * 1024 * 1024 // 2 GB

    private var simpleCache: SimpleCache? = null
    private var databaseProvider: StandaloneDatabaseProvider? = null
    private var cacheDir: File? = null
    private var activeReaderCount: Int = 0
    private var pendingClearRequest: Boolean = false
    private val cacheLock = ReentrantReadWriteLock()

    /** Observable cache state so UI can show "cache locked" indicators. */
    private val _cacheStateFlow = MutableStateFlow(CacheState.IDLE)
    val cacheStateFlow: StateFlow<CacheState> = _cacheStateFlow.asStateFlow()

    enum class CacheState { IDLE, ACTIVE_READERS, PENDING_CLEAR }

    fun getCache(context: Context): SimpleCache {
        cachedContext = context.applicationContext
        // FIX: Use READ lock for the hot path — only upgrade to WRITE if creation needed.
        cacheLock.read {
            simpleCache?.let { return it }
        }
        // Upgrade to write lock for creation.
        return cacheLock.write {
            // Double-check after acquiring write lock.
            simpleCache?.let { return it }
            cacheDir = File(context.cacheDir, "media_stream_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
            databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
            simpleCache = SimpleCache(cacheDir!!, evictor, databaseProvider!!)
            simpleCache!!
        }
    }

    fun acquireReader() {
        cacheLock.write {
            activeReaderCount++
            _cacheStateFlow.value =
                if (pendingClearRequest) CacheState.PENDING_CLEAR else CacheState.ACTIVE_READERS
        }
    }

    fun releaseReader() {
        cacheLock.write {
            if (activeReaderCount > 0) activeReaderCount--
            // FIX: If a clear was requested while readers were active, execute it now.
            if (activeReaderCount == 0 && pendingClearRequest) {
                Log.i(TAG, "All readers released — executing deferred cache clear")
                pendingClearRequest = false
                _cacheStateFlow.value = CacheState.IDLE
                // Don't call clearCache here to avoid recursive locking — run inline.
                val cache = simpleCache
                simpleCache = null
                databaseProvider = null
                try { cache?.release() } catch (e: Exception) { Log.e(TAG, "Failed to release cache", e) }
                try {
                    (cacheDir ?: File(context_safe().cacheDir, "media_stream_cache")).deleteRecursively()
                } catch (e: Exception) { Log.e(TAG, "Failed to delete cache dir", e) }
                cacheDir = null
            } else if (activeReaderCount == 0) {
                _cacheStateFlow.value = CacheState.IDLE
            }
        }
    }

    /** Safe context access for deferred operations (cached at init/call). */
    @Volatile private var cachedContext: Context? = null
    private fun context_safe(): Context =
        cachedContext ?: throw IllegalStateException("StreamCacheManager not initialized")

    fun clearCache(context: Context): Boolean {
        cachedContext = context.applicationContext
        return cacheLock.write {
            if (activeReaderCount > 0) {
                Log.w(TAG, "Cannot clear cache — $activeReaderCount active readers. Deferring clear.")
                pendingClearRequest = true
                _cacheStateFlow.value = CacheState.PENDING_CLEAR
                return@write false
            }
            val cache = simpleCache
            simpleCache = null
            databaseProvider = null
            try { cache?.release() } catch (e: Exception) { Log.e(TAG, "Failed to release cache", e) }
            try {
                (cacheDir ?: File(context.cacheDir, "media_stream_cache")).deleteRecursively()
            } catch (e: Exception) { Log.e(TAG, "Failed to delete cache dir", e) }
            cacheDir = null
            _cacheStateFlow.value = CacheState.IDLE
            true
        }
    }

    fun release() {
        cacheLock.write {
            if (activeReaderCount > 0) {
                Log.w(TAG, "release() called with $activeReaderCount active readers — deferring")
                pendingClearRequest = true
                _cacheStateFlow.value = CacheState.PENDING_CLEAR
                return@write
            }
            val cache = simpleCache
            simpleCache = null
            databaseProvider = null
            cacheDir = null
            try { cache?.release() } catch (e: Exception) { Log.e(TAG, "Failed to release simpleCache", e) }
            _cacheStateFlow.value = CacheState.IDLE
        }
    }
}
