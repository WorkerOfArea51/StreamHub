package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.os.StatFs
import android.util.Log
import coil.Coil
import com.streamhub.app.data.telegram.TdLibManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File

/**
 * Storage metrics breakdown data class.
 */
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

/**
 * Cache policy configuration.
 */
data class CacheConfig(
    val cacheLimitMb: Int = 2048,       // 500, 1024, 2048, 5120, -1 (unlimited)
    val cacheTtlDays: Int = 7,         // 3, 7, 14, -1 (never)
    val keepWatchedForInstantResume: Boolean = true
)

/**
 * Advanced Storage & Cache Manager for StreamHub.
 *
 * Provides:
 *  - Real-time disk size calculation for video stream buffers, Coil image cache, and app data.
 *  - Granular in-app purge operations (Clear Video Cache, Clear Image Cache, Master Clear).
 *  - TDLib SQLite database defragmentation and optimization.
 *  - Automated background eviction enforcing size limits and TTL on app startup.
 */
object StorageCacheManager {

    private const val TAG = "StorageCacheManager"
    private const val PREFS_NAME = "streamhub_storage_settings"
    private const val KEY_CACHE_LIMIT = "cache_limit_mb"
    private const val KEY_CACHE_TTL = "cache_ttl_days"
    private const val KEY_INSTANT_RESUME = "keep_watched_instant_resume"

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
     * Calculates storage breakdown across all subsystems asynchronously.
     */
    fun calculateStorageUsage() {
        if (!::appContext.isInitialized) return
        _metricsFlow.value = _metricsFlow.value.copy(isCalculating = true)

        scope.launch {
            try {
                // 1. Video Cache (TDLib temp & videos & documents directories)
                val tdlibDir = File(appContext.filesDir, "tdlib")
                var videoBytes = 0L
                var appDataBytes = 0L

                if (tdlibDir.exists()) {
                    tdlibDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val path = file.absolutePath.lowercase()
                            val len = file.length()
                            if (path.contains("videos") || path.contains("documents") || (path.contains("temp") && len > 500_000L)) {
                                videoBytes += len
                            } else if (!path.contains("photos") && !path.contains("thumbnails")) {
                                appDataBytes += len
                            }
                        }
                    }
                }

                // 2. Image Cache (Coil disk cache & thumbnails/photos)
                var imageBytes = 0L
                val coilCacheDir = File(appContext.cacheDir, "image_cache")
                if (coilCacheDir.exists()) {
                    imageBytes += getDirSize(coilCacheDir)
                }
                val coilDiskCache = Coil.imageLoader(appContext).diskCache?.size ?: 0L
                if (coilDiskCache > 0) {
                    imageBytes = maxOf(imageBytes, coilDiskCache)
                }

                if (tdlibDir.exists()) {
                    tdlibDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val path = file.absolutePath.lowercase()
                            if (path.contains("photos") || path.contains("thumbnails")) {
                                imageBytes += file.length()
                            }
                        }
                    }
                }

                // 3. App Data & Temp Cache
                val appCache = getDirSize(appContext.cacheDir)
                val appExtCache = appContext.externalCacheDir?.let { getDirSize(it) } ?: 0L
                val codeCache = getDirSize(appContext.codeCacheDir)
                appDataBytes += (appCache + appExtCache + codeCache - getDirSize(coilCacheDir)).coerceAtLeast(0L)

                // 4. Offline Downloads
                val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val downloadsBytes = if (downloadsDir != null && downloadsDir.exists()) getDirSize(downloadsDir) else 0L

                // 5. Device Free & Total Space
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
     * Clears all temporary video stream cache without affecting saved history or downloads.
     */
    suspend fun clearVideoCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            val tdlibDir = File(appContext.filesDir, "tdlib")
            if (tdlibDir.exists()) {
                tdlibDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val path = file.absolutePath.lowercase()
                        val len = file.length()
                        if (path.contains("videos") || path.contains("documents") || (path.contains("temp") && len > 500_000L)) {
                            file.delete()
                        }
                    }
                }
            }

            // Call TDLib optimize storage for video files
            runCatching {
                TdLibManager.send(
                    TdApi.OptimizeStorage(
                        0, 0, 0, 0,
                        arrayOf(TdApi.FileTypeVideo(), TdApi.FileTypeDocument(), TdApi.FileTypeAnimation()),
                        null, null, false, 0
                    )
                )
            }

            calculateStorageUsage()
            true
        } catch (e: Exception) {
            Log.e(TAG, "clearVideoCache failed", e)
            false
        }
    }

    /**
     * Clears image & thumbnail cache (Coil & TDLib thumbnails).
     */
    suspend fun clearImageCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Clear Coil memory and disk caches
            val imageLoader = Coil.imageLoader(appContext)
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()

            val coilCacheDir = File(appContext.cacheDir, "image_cache")
            if (coilCacheDir.exists()) {
                coilCacheDir.deleteRecursively()
            }

            val tdlibDir = File(appContext.filesDir, "tdlib")
            if (tdlibDir.exists()) {
                tdlibDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val path = file.absolutePath.lowercase()
                        if (path.contains("photos") || path.contains("thumbnails")) {
                            file.delete()
                        }
                    }
                }
            }

            calculateStorageUsage()
            true
        } catch (e: Exception) {
            Log.e(TAG, "clearImageCache failed", e)
            false
        }
    }

    /**
     * One-tap master purge for all temporary cache files.
     */
    suspend fun clearAllCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            clearVideoCache()
            clearImageCache()

            appContext.cacheDir.deleteRecursively()
            appContext.externalCacheDir?.deleteRecursively()

            calculateStorageUsage()
            true
        } catch (e: Exception) {
            Log.e(TAG, "clearAllCache failed", e)
            false
        }
    }

    /**
     * Compacts SQLite database and vacuums TDLib index files.
     */
    suspend fun compactAndOptimizeDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            TdLibManager.send(
                TdApi.OptimizeStorage(
                    0, 0, 0, 0,
                    arrayOf(
                        TdApi.FileTypeVideo(),
                        TdApi.FileTypeThumbnail(),
                        TdApi.FileTypeDocument(),
                        TdApi.FileTypeAnimation(),
                        TdApi.FileTypePhoto()
                    ),
                    null, null, false, 0
                )
            )
            calculateStorageUsage()
            true
        } catch (e: Exception) {
            Log.e(TAG, "compactAndOptimizeDatabase failed", e)
            false
        }
    }

    /**
     * Enforces Cache TTL and Cache Size Limits automatically.
     */
    private fun enforceCachePolicies() {
        scope.launch {
            try {
                val config = _configFlow.value
                val tdlibDir = File(appContext.filesDir, "tdlib")
                if (!tdlibDir.exists()) return@launch

                val now = System.currentTimeMillis()

                // 1. Enforce TTL
                if (config.cacheTtlDays > 0) {
                    val maxAgeMs = config.cacheTtlDays * 24 * 60 * 60 * 1000L
                    tdlibDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val path = file.absolutePath.lowercase()
                            if (path.contains("videos") || path.contains("documents") || path.contains("temp")) {
                                val age = now - file.lastModified()
                                if (age > maxAgeMs) {
                                    file.delete()
                                }
                            }
                        }
                    }
                }

                // 2. Enforce Size Limit
                if (config.cacheLimitMb > 0) {
                    val maxLimitBytes = config.cacheLimitMb * 1024L * 1024L
                    val videoFiles = mutableListOf<File>()
                    var totalSize = 0L

                    tdlibDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val path = file.absolutePath.lowercase()
                            if (path.contains("videos") || path.contains("documents") || (path.contains("temp") && file.length() > 500_000L)) {
                                videoFiles.add(file)
                                totalSize += file.length()
                            }
                        }
                    }

                    if (totalSize > maxLimitBytes) {
                        // Sort oldest first
                        videoFiles.sortBy { it.lastModified() }
                        for (file in videoFiles) {
                            if (totalSize <= maxLimitBytes) break
                            val len = file.length()
                            if (file.delete()) {
                                totalSize -= len
                            }
                        }
                    }
                }
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
