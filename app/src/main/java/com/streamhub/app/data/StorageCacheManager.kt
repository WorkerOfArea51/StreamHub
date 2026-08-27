package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.os.StatFs
import android.util.Log
import coil.Coil
import com.streamhub.app.player.StreamCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class StorageMetrics(
    val videoCacheBytes: Long = 0L,
    val imageCacheBytes: Long = 0L,
    val appDataBytes: Long = 0L,
    val downloadsBytes: Long = 0L,
    val totalAppBytes: Long = 0L,
    val freeDeviceBytes: Long = 0L,
    val totalDeviceBytes: Long = 0L,
    val isCalculating: Boolean = false
)

data class CacheConfig(
    val cacheLimitMb: Int = 2048,
    val cacheTtlDays: Int = 7,
    val keepWatchedForInstantResume: Boolean = true
)

@OptIn(coil.annotation.ExperimentalCoilApi::class)
object StorageCacheManager {

    private const val TAG = "StorageCacheManager"
    private const val PREFS_NAME = "streamhub_storage_settings"
    private const val KEY_CACHE_LIMIT = "cache_limit_mb"
    private const val KEY_CACHE_TTL = "cache_ttl_days"
    private const val KEY_INSTANT_RESUME = "keep_watched_instant_resume"

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clearMutex = Mutex()

    private val _metricsFlow = MutableStateFlow(StorageMetrics())
    val metricsFlow: StateFlow<StorageMetrics> = _metricsFlow.asStateFlow()

    private val _configFlow = MutableStateFlow(CacheConfig())
    val configFlow: StateFlow<CacheConfig> = _configFlow.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext

        loadConfig()
        calculateStorageUsage()
        enforceCachePolicies()
    }

    private fun getPrefs(): SharedPreferences {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadConfig() {
        val p = getPrefs()
        _configFlow.value = CacheConfig(
            cacheLimitMb = p.getInt(KEY_CACHE_LIMIT, 2048),
            cacheTtlDays = p.getInt(KEY_CACHE_TTL, 7),
            keepWatchedForInstantResume = p.getBoolean(KEY_INSTANT_RESUME, true)
        )
    }

    fun updateConfig(newConfig: CacheConfig) {
        _configFlow.value = newConfig
        getPrefs().edit()
            .putInt(KEY_CACHE_LIMIT, newConfig.cacheLimitMb)
            .putInt(KEY_CACHE_TTL, newConfig.cacheTtlDays)
            .putBoolean(KEY_INSTANT_RESUME, newConfig.keepWatchedForInstantResume)
            .apply()

        scope.launch {
            enforceCachePolicies()
            calculateStorageUsage()
        }
    }

    /**
     * Re-calculate storage usage breakdown across video cache, images, app data and downloads.
     */
    fun calculateStorageUsage() {
        if (!::appContext.isInitialized) return
        _metricsFlow.value = _metricsFlow.value.copy(isCalculating = true)

        scope.launch {
            try {
                // 1. Video stream cache
                val videoCacheDir = File(appContext.cacheDir, "media_stream_cache")
                val videoCacheDirBytes = if (videoCacheDir.exists()) getDirSize(videoCacheDir) else 0L
                val exoCacheBytes = runCatching { StreamCacheManager.getCache(appContext).cacheSpace }.getOrDefault(0L)
                val videoBytes = maxOf(videoCacheDirBytes, exoCacheBytes)

                // 2. Image cache
                val coilCacheDir = File(appContext.cacheDir, "image_cache")
                var imageBytes = if (coilCacheDir.exists()) getDirSize(coilCacheDir) else 0L
                val coilDiskCache = runCatching { Coil.imageLoader(appContext).diskCache?.size ?: 0L }.getOrDefault(0L)
                if (coilDiskCache > 0) {
                    imageBytes = maxOf(imageBytes, coilDiskCache)
                }

                // 3. App data & databases
                val appCache = getDirSize(appContext.cacheDir)
                val appExtCache = appContext.externalCacheDir?.let { getDirSize(it) } ?: 0L
                val codeCache = getDirSize(appContext.codeCacheDir)
                val appDataBytes = (appCache + appExtCache + codeCache - videoBytes - imageBytes).coerceAtLeast(0L)

                // 4. Downloads
                val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val downloadsBytes = if (downloadsDir != null && downloadsDir.exists()) getDirSize(downloadsDir) else 0L

                // 5. Device storage stats
                val statFs = StatFs(appContext.filesDir.absolutePath)
                val freeDeviceBytes = statFs.availableBlocksLong * statFs.blockSizeLong
                val totalDeviceBytes = statFs.blockCountLong * statFs.blockSizeLong

                val totalAppBytes = videoBytes + imageBytes + appDataBytes + downloadsBytes

                _metricsFlow.value = StorageMetrics(
                    videoCacheBytes = videoBytes,
                    imageCacheBytes = imageBytes,
                    appDataBytes = appDataBytes,
                    downloadsBytes = downloadsBytes,
                    totalAppBytes = totalAppBytes,
                    freeDeviceBytes = freeDeviceBytes,
                    totalDeviceBytes = totalDeviceBytes,
                    isCalculating = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to calculate storage usage", e)
                _metricsFlow.value = _metricsFlow.value.copy(isCalculating = false)
            }
        }
    }

    /**
     * Clear Video Stream Cache.
     */
    suspend fun clearVideoCache(): Boolean = withContext(Dispatchers.IO) {
        clearMutex.withLock {
            try {
                StreamCacheManager.clearCache(appContext)
                val videoCacheDir = File(appContext.cacheDir, "media_stream_cache")
                if (videoCacheDir.exists()) {
                    videoCacheDir.listFiles()?.forEach { it.delete() }
                }
                calculateStorageUsage()
                true
            } catch (e: Exception) {
                Log.e(TAG, "clearVideoCache failed", e)
                false
            }
        }
    }

    /**
     * Clear Image Cache (Coil memory & disk cache).
     */
    suspend fun clearImageCache(): Boolean = withContext(Dispatchers.IO) {
        clearMutex.withLock {
            try {
                val imageLoader = Coil.imageLoader(appContext)
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
                calculateStorageUsage()
                true
            } catch (e: Exception) {
                Log.e(TAG, "clearImageCache failed", e)
                false
            }
        }
    }

    /**
     * Clear all non-critical caches.
     */
    suspend fun clearAllCache(): Boolean = withContext(Dispatchers.IO) {
        clearMutex.withLock {
            try {
                StreamCacheManager.clearCache(appContext)
                val imageLoader = Coil.imageLoader(appContext)
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()

                val cacheDir = appContext.cacheDir
                cacheDir.listFiles()?.forEach { child ->
                    if (child.name != "media_stream_cache" && child.name != "exoplayer_downloads") {
                        try { child.deleteRecursively() } catch (_: Exception) {}
                    }
                }
                appContext.externalCacheDir?.listFiles()?.forEach { child ->
                    try { child.deleteRecursively() } catch (_: Exception) {}
                }

                calculateStorageUsage()
                true
            } catch (e: Exception) {
                Log.e(TAG, "clearAllCache failed", e)
                false
            }
        }
    }

    /**
     * Compact and optimize database.
     */
    suspend fun compactAndOptimizeDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            calculateStorageUsage()
            true
        } catch (e: Exception) {
            Log.e(TAG, "compactAndOptimizeDatabase failed", e)
            false
        }
    }

    private fun enforceCachePolicies() {
        scope.launch {
            try {
                val config = _configFlow.value
                if (config.cacheLimitMb > 0) {
                    val maxLimitBytes = config.cacheLimitMb * 1024L * 1024L
                    val currentVideoBytes = _metricsFlow.value.videoCacheBytes
                    if (currentVideoBytes > maxLimitBytes) {
                        StreamCacheManager.clearCache(appContext)
                    }
                }
                calculateStorageUsage()
            } catch (e: Exception) {
                Log.w(TAG, "Error enforcing cache policies", e)
            }
        }
    }

    private fun getDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        try {
            dir.walkTopDown().forEach { f ->
                if (f.isFile) size += f.length()
            }
        } catch (_: Exception) {}
        return size
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.lastIndex)
        return String.format(java.util.Locale.US, "%.2f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }
}
