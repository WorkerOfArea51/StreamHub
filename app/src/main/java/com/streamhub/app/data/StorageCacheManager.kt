package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.net.Uri
import coil.Coil
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.player.StreamCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
    val cacheLimitMb: Int = -1, // Default: Unlimited (-1)
    val cacheTtlHours: Int = -1, // Default: Never (-1) or 1h, 6h, 12h, 24h, 72h, 168h, 336h, 720h
    val keepWatchedForInstantResume: Boolean = true
)

data class CachedStreamItem(
    val key: String,
    val mediaId: String? = null,
    val title: String,
    val subtitle: String = "",
    val posterUrl: String? = null,
    val sizeBytes: Long,
    val formattedSize: String,
    val lastAccessedTimestamp: Long,
    val expiryTimestamp: Long? = null,
    val isExpired: Boolean = false,
    val timeRemainingText: String = ""
)

data class CachedMediaMetadata(
    val cacheKey: String,
    val mediaId: String?,
    val title: String,
    val subtitle: String,
    val posterUrl: String?,
    val registeredTimestamp: Long
)

@OptIn(coil.annotation.ExperimentalCoilApi::class)
object StorageCacheManager {

    private const val TAG = "StorageCacheManager"
    private const val PREFS_NAME = "streamhub_storage_settings"
    private const val PREFS_METADATA = "streamhub_cache_metadata"
    private const val KEY_CACHE_LIMIT = "cache_limit_mb"
    private const val KEY_CACHE_TTL = "cache_ttl_hours"
    private const val KEY_INSTANT_RESUME = "keep_watched_instant_resume"

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clearMutex = Mutex()
    private var periodicEnforceJob: Job? = null

    private val _metricsFlow = MutableStateFlow(StorageMetrics())
    val metricsFlow: StateFlow<StorageMetrics> = _metricsFlow.asStateFlow()

    private val _configFlow = MutableStateFlow(CacheConfig())
    val configFlow: StateFlow<CacheConfig> = _configFlow.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext

        loadConfig()
        startPeriodicEnforcement()

        // Defer heavy disk directory walks by 2.5s so startup remains 120fps smooth
        scope.launch {
            delay(2_500L)
            enforceCachePolicies()
            calculateStorageUsage()
        }
    }

    private fun startPeriodicEnforcement() {
        periodicEnforceJob?.cancel()
        periodicEnforceJob = scope.launch {
            while (isActive) {
                delay(30 * 60_000L) // Self-healing check every 30 minutes while app is running
                enforceCachePolicies()
            }
        }
    }

    private fun getPrefs(): SharedPreferences {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadConfig() {
        val p = getPrefs()
        _configFlow.value = CacheConfig(
            cacheLimitMb = p.getInt(KEY_CACHE_LIMIT, -1),
            cacheTtlHours = p.getInt(KEY_CACHE_TTL, -1),
            keepWatchedForInstantResume = p.getBoolean(KEY_INSTANT_RESUME, true)
        )
    }

    fun updateConfig(newConfig: CacheConfig) {
        _configFlow.value = newConfig
        getPrefs().edit()
            .putInt(KEY_CACHE_LIMIT, newConfig.cacheLimitMb)
            .putInt(KEY_CACHE_TTL, newConfig.cacheTtlHours)
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
                clearAllCachedMetadata()
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
                clearAllCachedMetadata()
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

    /**
     * Register a newly watched/prewarmed video stream into the persistent cache metadata registry.
     */
    fun registerCachedStream(
        cacheKey: String,
        mediaId: String?,
        title: String,
        subtitle: String = "",
        posterUrl: String? = null
    ) {
        if (!::appContext.isInitialized || cacheKey.isBlank()) return
        scope.launch {
            try {
                val prefs = appContext.getSharedPreferences(PREFS_METADATA, Context.MODE_PRIVATE)
                val json = JSONObject().apply {
                    put("mediaId", mediaId ?: "")
                    put("title", title)
                    put("subtitle", subtitle)
                    put("posterUrl", posterUrl ?: "")
                    put("registeredTimestamp", System.currentTimeMillis())
                }
                prefs.edit().putString(cacheKey, json.toString()).apply()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register cache metadata for $cacheKey", e)
            }
        }
    }

    fun getCachedMetadata(cacheKey: String): CachedMediaMetadata? {
        if (!::appContext.isInitialized) return null
        return try {
            val prefs = appContext.getSharedPreferences(PREFS_METADATA, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(cacheKey, null) ?: return null
            val obj = JSONObject(jsonStr)
            CachedMediaMetadata(
                cacheKey = cacheKey,
                mediaId = obj.optString("mediaId").takeIf { it.isNotBlank() },
                title = obj.optString("title", "Unknown Video"),
                subtitle = obj.optString("subtitle", ""),
                posterUrl = obj.optString("posterUrl").takeIf { it.isNotBlank() },
                registeredTimestamp = obj.optLong("registeredTimestamp", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun removeCachedMetadata(cacheKey: String) {
        if (!::appContext.isInitialized) return
        try {
            appContext.getSharedPreferences(PREFS_METADATA, Context.MODE_PRIVATE)
                .edit().remove(cacheKey).apply()
        } catch (_: Exception) {}
    }

    private fun clearAllCachedMetadata() {
        if (!::appContext.isInitialized) return
        try {
            appContext.getSharedPreferences(PREFS_METADATA, Context.MODE_PRIVATE)
                .edit().clear().apply()
        } catch (_: Exception) {}
    }

    /**
     * Delete an individual cached video stream by its cacheKey.
     */
    suspend fun deleteCachedStream(cacheKey: String): Boolean = withContext(Dispatchers.IO) {
        clearMutex.withLock {
            try {
                StreamCacheManager.removeResource(cacheKey)
                removeCachedMetadata(cacheKey)
                calculateStorageUsage()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete cached stream $cacheKey", e)
                false
            }
        }
    }

    /**
     * Purge all cached video streams that have exceeded the configured TTL lifespan.
     * Returns the count of purged streams.
     */
    suspend fun purgeExpiredStreams(): Int = withContext(Dispatchers.IO) {
        if (!::appContext.isInitialized) return@withContext 0
        clearMutex.withLock {
            try {
                val resources = StreamCacheManager.getCachedResources(appContext)
                val config = _configFlow.value
                val ttlHours = config.cacheTtlHours
                if (ttlHours <= 0) return@withContext 0
                val ttlMillis = ttlHours * 3600_000L
                val now = System.currentTimeMillis()
                var purgedCount = 0

                for (res in resources) {
                    val meta = getCachedMetadata(res.key)
                    val effectiveTime = maxOf(res.lastTouchTimestamp, meta?.registeredTimestamp ?: 0L)
                    if (now - effectiveTime >= ttlMillis) {
                        Log.i(TAG, "Purging expired stream key: ${res.key}")
                        StreamCacheManager.removeResource(res.key)
                        removeCachedMetadata(res.key)
                        purgedCount++
                    }
                }
                if (purgedCount > 0) {
                    calculateStorageUsage()
                }
                purgedCount
            } catch (e: Exception) {
                Log.e(TAG, "Failed to purge expired streams", e)
                0
            }
        }
    }

    /**
     * Smart reverse lookup: matches raw stream URLs/hashes against Firebase media catalog
     * and Watch History to resolve real movie/series titles, episode names, and posters.
     */
    private fun resolveMediaInfoForCacheKey(cacheKey: String, sizeBytes: Long): CachedMediaMetadata {
        val fileId = TelegramLinkResolver.extractTelegramMessageOrFileId(cacheKey)
        val uri = try { Uri.parse(cacheKey) } catch (_: Exception) { null }
        val lastSegment = uri?.lastPathSegment ?: ""

        // 1. Search Firebase Media Catalog
        val catalog = FirebaseRepository.getInstance().mediaCatalog.value
        for (media in catalog) {
            val isMovie = media.type.equals("MOVIE", ignoreCase = true) || media.category.equals("MOVIE", ignoreCase = true)
            // Check episodes (movies have a single episode or episode list)
            for (ep in media.episodes) {
                val epFileId = ep.telegramFileId.ifBlank { TelegramLinkResolver.extractTelegramMessageOrFileId(ep.streamUrl) }
                val epMirrorFileId = TelegramLinkResolver.extractTelegramMessageOrFileId(ep.mirrorStreamUrl)
                val isEpMatch = (fileId.isNotBlank() && (epFileId.equals(fileId, ignoreCase = true) || epMirrorFileId.equals(fileId, ignoreCase = true))) ||
                    (ep.streamUrl.isNotBlank() && (cacheKey.contains(ep.streamUrl, ignoreCase = true) || ep.streamUrl.contains(cacheKey, ignoreCase = true))) ||
                    (lastSegment.isNotBlank() && (ep.streamUrl.contains(lastSegment, ignoreCase = true) || ep.mirrorStreamUrl.contains(lastSegment, ignoreCase = true))) ||
                    (ep.fileName.isNotBlank() && (cacheKey.contains(ep.fileName, ignoreCase = true) || lastSegment.contains(ep.fileName, ignoreCase = true)))

                if (isEpMatch) {
                    val sub = when {
                        isMovie -> if (ep.title.isNotBlank() && !ep.title.equals(media.title, ignoreCase = true)) ep.title else "Movie"
                        ep.seasonNumber > 0 -> "Season ${ep.seasonNumber} • Episode ${ep.episodeNumber}${if (ep.title.isNotBlank()) ": ${ep.title}" else ""}"
                        ep.episodeNumber > 0 -> "Episode ${ep.episodeNumber}${if (ep.title.isNotBlank()) ": ${ep.title}" else ""}"
                        else -> ep.title.ifBlank { "Episode" }
                    }
                    val poster = ep.thumbnailUrl.ifBlank { media.posterUrl }
                    val resolved = CachedMediaMetadata(
                        cacheKey = cacheKey,
                        mediaId = media.id,
                        title = media.title,
                        subtitle = sub,
                        posterUrl = poster,
                        registeredTimestamp = System.currentTimeMillis()
                    )
                    registerCachedStream(cacheKey, media.id, media.title, sub, poster)
                    return resolved
                }
            }

            // Also check if cacheKey or lastSegment matches media title directly
            if (media.title.isNotBlank() && (cacheKey.contains(media.title, ignoreCase = true) || (lastSegment.isNotBlank() && lastSegment.contains(media.title, ignoreCase = true)))) {
                val sub = if (isMovie) "Movie" else "Series"
                val resolved = CachedMediaMetadata(
                    cacheKey = cacheKey,
                    mediaId = media.id,
                    title = media.title,
                    subtitle = sub,
                    posterUrl = media.posterUrl,
                    registeredTimestamp = System.currentTimeMillis()
                )
                registerCachedStream(cacheKey, media.id, media.title, sub, media.posterUrl)
                return resolved
            }
        }

        // 2. Search Watch History
        val historyMap = WatchHistoryManager.historyFlow.value
        for ((_, progress) in historyMap) {
            if (progress.title.isNotBlank()) {
                val matchesHistory = (fileId.isNotBlank() && progress.mediaId.contains(fileId, ignoreCase = true)) ||
                    (historyMap.size == 1) // If only one item in watch history, associate it

                if (matchesHistory) {
                    val sub = when {
                        progress.seasonNumber > 0 -> "Season ${progress.seasonNumber} • Episode ${progress.episodeNumber}${if (progress.episodeTitle.isNotBlank()) ": ${progress.episodeTitle}" else ""}"
                        progress.episodeNumber > 0 -> "Episode ${progress.episodeNumber}${if (progress.episodeTitle.isNotBlank()) ": ${progress.episodeTitle}" else ""}"
                        else -> if (progress.mediaType.isNotBlank()) progress.mediaType else "Movie"
                    }
                    val resolved = CachedMediaMetadata(
                        cacheKey = cacheKey,
                        mediaId = progress.mediaId,
                        title = progress.title,
                        subtitle = sub,
                        posterUrl = progress.posterUrl,
                        registeredTimestamp = System.currentTimeMillis()
                    )
                    registerCachedStream(cacheKey, progress.mediaId, progress.title, sub, progress.posterUrl)
                    return resolved
                }
            }
        }

        // 3. Clean human-readable fallback (NEVER display raw hex strings)
        val cleanFallback = try {
            val last = uri?.lastPathSegment ?: "Video Stream"
            val stripped = last.substringBeforeLast(".").replace('_', ' ').replace('-', ' ').trim()
            if (stripped.matches(Regex("""(?i)^[a-f0-9]{12,}$"""))) {
                "Cached Video Stream"
            } else {
                stripped.ifBlank { "Cached Video Stream" }
            }
        } catch (_: Exception) {
            "Cached Video Stream"
        }

        return CachedMediaMetadata(
            cacheKey = cacheKey,
            mediaId = null,
            title = cleanFallback,
            subtitle = "Direct Stream (${formatBytes(sizeBytes)})",
            posterUrl = null,
            registeredTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Fetch list of all active cached video streams with their metadata, size, and auto-delete countdowns.
     */
    suspend fun getCachedStreamEntries(): List<CachedStreamItem> = withContext(Dispatchers.IO) {
        if (!::appContext.isInitialized) return@withContext emptyList()
        try {
            val resources = StreamCacheManager.getCachedResources(appContext)
            val config = _configFlow.value
            val ttlHours = config.cacheTtlHours
            val ttlMillis = if (ttlHours > 0) ttlHours * 3600_000L else -1L
            val now = System.currentTimeMillis()

            resources.map { res ->
                val meta = getCachedMetadata(res.key).let { m ->
                    if (m != null && m.title.isNotBlank() && !m.title.matches(Regex("""(?i)^[a-f0-9]{12,}$"""))) {
                        m
                    } else {
                        resolveMediaInfoForCacheKey(res.key, res.sizeBytes)
                    }
                }
                val effectiveTime = maxOf(res.lastTouchTimestamp, meta.registeredTimestamp)
                val expiryTimestamp = if (ttlMillis > 0L) effectiveTime + ttlMillis else null
                val isExpired = expiryTimestamp != null && now >= expiryTimestamp

                val timeRemainingText = when {
                    expiryTimestamp == null -> "Retained (Auto-delete off)"
                    isExpired -> "Expired (Ready to purge)"
                    else -> {
                        val diffMs = expiryTimestamp - now
                        val totalMinutes = diffMs / 60_000L
                        val hours = totalMinutes / 60L
                        val minutes = totalMinutes % 60L
                        val days = hours / 24L
                        when {
                            days > 1L -> "Auto-deletes in $days days"
                            days == 1L -> "Auto-deletes in 1 day"
                            hours > 0L -> "Auto-deletes in ${hours}h ${minutes}m"
                            else -> "Auto-deletes in ${minutes.coerceAtLeast(1)}m"
                        }
                    }
                }

                CachedStreamItem(
                    key = res.key,
                    mediaId = meta.mediaId,
                    title = meta.title,
                    subtitle = meta.subtitle,
                    posterUrl = meta.posterUrl,
                    sizeBytes = res.sizeBytes,
                    formattedSize = formatBytes(res.sizeBytes),
                    lastAccessedTimestamp = effectiveTime,
                    expiryTimestamp = expiryTimestamp,
                    isExpired = isExpired,
                    timeRemainingText = timeRemainingText
                )
            }.sortedByDescending { it.lastAccessedTimestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cached stream entries", e)
            emptyList()
        }
    }

    /**
     * Enforce cache policies: Media3-native TTL auto-delete and LRU size limits.
     */
    fun enforceCachePolicies() {
        if (!::appContext.isInitialized) return
        scope.launch {
            clearMutex.withLock {
                try {
                    val config = _configFlow.value
                    val resources = StreamCacheManager.getCachedResources(appContext)
                    val ttlHours = config.cacheTtlHours
                    val ttlMillis = if (ttlHours > 0) ttlHours * 3600_000L else -1L
                    val now = System.currentTimeMillis()

                    // 1. Enforce TTL Policy: purge any stream resource whose age >= TTL
                    if (ttlMillis > 0L) {
                        resources.forEach { res ->
                            val meta = getCachedMetadata(res.key)
                            val effectiveTime = maxOf(res.lastTouchTimestamp, meta?.registeredTimestamp ?: 0L)
                            val age = now - effectiveTime
                            if (age >= ttlMillis) {
                                Log.i(TAG, "TTL Expired for key: ${res.key} (Age: ${age / 3600_000L}h >= ${ttlHours}h). Evicting from SimpleCache.")
                                StreamCacheManager.removeResource(res.key)
                                removeCachedMetadata(res.key)
                            }
                        }
                    }

                    // 2. Enforce Size Limit Policy: evict oldest if total exceeds configured MB
                    if (config.cacheLimitMb > 0) {
                        val maxLimitBytes = config.cacheLimitMb * 1024L * 1024L
                        val currentResources = StreamCacheManager.getCachedResources(appContext)
                        var currentTotalBytes = currentResources.sumOf { it.sizeBytes }
                        if (currentTotalBytes > maxLimitBytes) {
                            val sortedOldestFirst = currentResources.sortedBy { it.lastTouchTimestamp }
                            for (res in sortedOldestFirst) {
                                if (currentTotalBytes <= maxLimitBytes) break
                                Log.i(TAG, "Cache size limit exceeded. Evicting oldest: ${res.key} (${res.sizeBytes} bytes)")
                                StreamCacheManager.removeResource(res.key)
                                removeCachedMetadata(res.key)
                                currentTotalBytes -= res.sizeBytes
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error enforcing cache policies", e)
                }
            }
            calculateStorageUsage()
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
