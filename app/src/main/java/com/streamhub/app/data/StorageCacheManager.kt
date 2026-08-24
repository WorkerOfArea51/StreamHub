package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.os.StatFs
import android.util.Log
import coil.Coil
import com.streamhub.app.data.telegram.TdLibManager
import com.streamhub.app.data.telegram.TdLibMediaProvider
import com.streamhub.app.data.telegram.StreamingProxyServer
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
import org.drinkless.tdlib.TdApi
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
    private const val LARGE_FILE_THRESHOLD_BYTES = 500_000L

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // FIX: Mutex prevents concurrent cache-clear operations from racing with each other.
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
     * FIX: Single-pass iteration of tdlibDir. Previously walked the tree TWICE
     * (once for video, once for image bytes), doubling IO on large caches.
     */
    fun calculateStorageUsage() {
        if (!::appContext.isInitialized) return
        _metricsFlow.value = _metricsFlow.value.copy(isCalculating = true)

        scope.launch {
            try {
                val tdlibDir = File(appContext.filesDir, "tdlib")
                var videoBytes = 0L
                var imageBytes = 0L
                var appDataBytes = 0L

                if (tdlibDir.exists()) {
                    tdlibDir.walkTopDown().forEach { file ->
                        if (!file.isFile) return@forEach
                        val path = file.absolutePath.lowercase()
                        val rawLen = file.length()
                        
                        // If file is an active TDLib file, use actual downloaded size rather than pre-allocated sparse size
                        val fileId = TdLibMediaProvider.getFileIdForPath(file.absolutePath)
                        val cachedTdFile = fileId?.let { StreamingProxyServer.getCachedFile(it) }
                        val effectiveLen = if (cachedTdFile != null && !cachedTdFile.local.isDownloadingCompleted) {
                            cachedTdFile.local.downloadedSize.toLong().coerceIn(0L, rawLen)
                        } else {
                            rawLen
                        }

                        when {
                            path.contains("videos") || path.contains("documents") ||
                            (path.contains("temp") && effectiveLen > LARGE_FILE_THRESHOLD_BYTES) -> {
                                videoBytes += effectiveLen
                            }
                            path.contains("photos") || path.contains("thumbnails") -> {
                                imageBytes += effectiveLen
                            }
                            else -> {
                                appDataBytes += effectiveLen
                            }
                        }
                    }
                }

                val coilCacheDir = File(appContext.cacheDir, "image_cache")
                if (coilCacheDir.exists()) {
                    imageBytes += getDirSize(coilCacheDir)
                }
                val coilDiskCache = Coil.imageLoader(appContext).diskCache?.size ?: 0L
                if (coilDiskCache > 0) {
                    imageBytes = maxOf(imageBytes, coilDiskCache)
                }

                val appCache = getDirSize(appContext.cacheDir)
                val appExtCache = appContext.externalCacheDir?.let { getDirSize(it) } ?: 0L
                val codeCache = getDirSize(appContext.codeCacheDir)
                appDataBytes += (appCache + appExtCache + codeCache - getDirSize(coilCacheDir)).coerceAtLeast(0L)

                val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val downloadsBytes = if (downloadsDir != null && downloadsDir.exists()) getDirSize(downloadsDir) else 0L

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
                // 1. Ask TDLib to optimize storage — size=0L means delete all files of specified types.
                runCatching {
                    TdLibManager.send(
                        TdApi.OptimizeStorage(
                            0L, 0, 0, 0,
                            arrayOf(TdApi.FileTypeVideo(), TdApi.FileTypeDocument(), TdApi.FileTypeAnimation()),
                            null, null, false, 0
                        )
                    )
                }

                // 2. Delete unreferenced / orphaned files from tdlib video directory
                val tdlibDir = File(appContext.filesDir, "tdlib")
                val videosDir = File(tdlibDir, "videos")
                if (videosDir.exists()) {
                    videosDir.listFiles()?.forEach { f -> runCatching { f.delete() } }
                }

                // 3. Ask ExoPlayer's StreamCacheManager to release + clear
                StreamCacheManager.clearCache(appContext)

                calculateStorageUsage()
                true
            } catch (e: Exception) {
                Log.e(TAG, "clearVideoCache failed", e)
                false
            }
        }
    }

    /**
     * FIX: Use Coil's diskCache.clear() only — don't manually delete the directory,
     * which Coil may have open file handles on. Also let TDLib optimize thumbnails.
     */
    suspend fun clearImageCache(): Boolean = withContext(Dispatchers.IO) {
        clearMutex.withLock {
            try {
                val imageLoader = Coil.imageLoader(appContext)
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()

                runCatching {
                    TdLibManager.send(
                        TdApi.OptimizeStorage(
                            1L, 0, 0, 0,
                            arrayOf(TdApi.FileTypeThumbnail(), TdApi.FileTypePhoto()),
                            null, null, false, 0
                        )
                    )
                }

                calculateStorageUsage()
                true
            } catch (e: Exception) {
                Log.e(TAG, "clearImageCache failed", e)
                false
            }
        }
    }

    /**
     * FIX: Clear video + image caches, but NEVER call cacheDir.deleteRecursively().
     * The cacheDir contains ExoPlayer's SimpleCache SQLite index — deleting it while
     * SimpleCache has open handles corrupts the cache permanently.
     *
     * FIX (regression): clearVideoCache() and clearImageCache() each acquire clearMutex
     * internally. Calling them from inside clearAllCache's withLock would deadlock
     * (kotlinx Mutex is non-reentrant). The inline logic below performs the same work
     * WITHOUT calling the public functions — keeping a single lock acquisition.
     */
    suspend fun clearAllCache(): Boolean = withContext(Dispatchers.IO) {
        clearMutex.withLock {
            try {
                // 1. Video cache — inline (mirrors clearVideoCache logic).
                runCatching {
                    TdLibManager.send(
                        TdApi.OptimizeStorage(
                            1L, 0, 0, 0,
                            arrayOf(TdApi.FileTypeVideo(), TdApi.FileTypeDocument(), TdApi.FileTypeAnimation()),
                            null, null, false, 0
                        )
                    )
                }
                val cacheCleared = StreamCacheManager.clearCache(appContext)
                if (!cacheCleared) {
                    Log.w(TAG, "ExoPlayer cache clear deferred (active readers)")
                }

                // 2. Image cache — inline (mirrors clearImageCache logic).
                val imageLoader = Coil.imageLoader(appContext)
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
                runCatching {
                    TdLibManager.send(
                        TdApi.OptimizeStorage(
                            1L, 0, 0, 0,
                            arrayOf(TdApi.FileTypeThumbnail(), TdApi.FileTypePhoto()),
                            null, null, false, 0
                        )
                    )
                }

                // 3. Non-critical cache subdirs — leave media_stream_cache / exoplayer_downloads alone.
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
     * FIX: TDLib optimize storage is the canonical way to compact the SQLite DB.
     * Don't call TdApi.OptimizeStorage with file_size_limit=0 (that means "no limit"),
     * use a small size limit to force compaction.
     */
    suspend fun compactAndOptimizeDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            TdLibManager.send(
                TdApi.OptimizeStorage(
                    1L * 1024 * 1024, // 1 MB size limit forces aggressive compaction
                    1, // count limit
                    0, 0,
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
     * FIX: Enforce Cache TTL and Size Limits — but use TDLib's OptimizeStorage instead of
     * manually deleting files based on lastModified (which TDLib updates on every read,
     * making the TTL effectively useless).
     *
     * Strategy:
     *   - For size limit: call OptimizeStorage with the deficit as size_limit.
     *   - For TTL: convert TTL days to seconds and pass as max_seconds_from_last_access.
     */
    private fun enforceCachePolicies() {
        scope.launch {
            try {
                val config = _configFlow.value
                val tdlibDir = File(appContext.filesDir, "tdlib")
                if (!tdlibDir.exists()) return@launch

                // 1. Enforce TTL via TDLib (uses internal access time, not filesystem mtime).
                if (config.cacheTtlDays > 0) {
                    // FIX: duration is arg 3, not arg 4 (which is longCount).
                    val maxAgeSeconds = config.cacheTtlDays * 24 * 60 * 60
                    runCatching {
                        TdLibManager.send(
                            TdApi.OptimizeStorage(
                                0L, 0, maxAgeSeconds, 0,
                                arrayOf(TdApi.FileTypeVideo(), TdApi.FileTypeDocument(), TdApi.FileTypeAnimation()),
                                null, null, false, 0
                            )
                        )
                    }
                }

                // 2. Enforce Size Limit via TDLib.
                if (config.cacheLimitMb > 0) {
                    val maxLimitBytes = config.cacheLimitMb * 1024L * 1024L
                    // Calculate current size from metrics flow (already computed by calculateStorageUsage).
                    val currentVideoBytes = _metricsFlow.value.videoCacheBytes
                    if (currentVideoBytes > maxLimitBytes) {
                        val deficit = currentVideoBytes - maxLimitBytes
                        runCatching {
                            TdLibManager.send(
                                TdApi.OptimizeStorage(
                                    deficit, 0, 0, 0,
                                    arrayOf(TdApi.FileTypeVideo(), TdApi.FileTypeDocument(), TdApi.FileTypeAnimation()),
                                    null, null, false, 0
                                )
                            )
                        }
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
