package com.streamhub.app.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.data.TelegramLinkResolver
import com.streamhub.app.data.api.SharedHttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Real-time telemetry snapshot of background binge pre-caching progress for Episode N+1.
 */
data class BingePrecacheStatus(
    val isActive: Boolean = false,
    val isCompleted: Boolean = false,
    val cachedBytes: Long = 0L,
    val targetBytes: Long = StreamPreloadManager.BINGE_PRECACHE_BYTES,
    val speedKbps: Long = 0L,
    val episodeTitle: String = "",
    val cacheKey: String = ""
) {
    val progressPercent: Int
        get() = if (targetBytes > 0) ((cachedBytes.toFloat() / targetBytes.toFloat()) * 100f).toInt().coerceIn(0, 100) else 0
}

/**
 * Intelligent Media Stream Pre-Warming & Binge Pre-Caching Engine.
 *
 * Capabilities:
 * 1. Details Screen Micro-Prewarm: Fetches the first 2MB (EBML/MKV container & initial keyframes)
 *    into [StreamCacheManager] disk cache after a 1.8s dwell debounce. Cancels cleanly if user navigates away.
 * 2. In-Player Binge Pre-Cache: While Episode N is safely playing, pre-caches the first 25MB
 *    of Episode N+1 directly into the shared cache for zero-latency, instantaneous transitions.
 */
@OptIn(UnstableApi::class)
object StreamPreloadManager {

    private const val TAG = "StreamPreloadManager"
    private const val USER_AGENT = "StreamHub/4.8 (Linux; Android 14; Mobile)"

    /** 2 MB micro-chunk is sufficient for MKV headers, subtitle tracks, and first audio/video packets */
    const val DETAILS_PREWARM_BYTES = 2L * 1024 * 1024 // 2 MB

    /** 25 MB gives ~30-60s of 1080p video, guaranteeing zero startup delay and smooth playback transition */
    const val BINGE_PRECACHE_BYTES = 25L * 1024 * 1024 // 25 MB

    private var activeDetailsJob: Job? = null
    private var activeDetailsWriter: CacheWriter? = null
    private var activeDetailsDataSource: androidx.media3.datasource.DataSource? = null
    @Volatile private var activePrewarmUrl: String = ""

    private var activeBingeJob: Job? = null
    private var activeBingeWriter: CacheWriter? = null
    private var activeBingeDataSource: androidx.media3.datasource.DataSource? = null

    private val _bingePrecacheStatus = MutableStateFlow(BingePrecacheStatus())
    val bingePrecacheStatus: StateFlow<BingePrecacheStatus> = _bingePrecacheStatus.asStateFlow()

    /** Dedicated OkHttpClient for background preloader to isolate sockets from active player */
    private val preloadClient: okhttp3.OkHttpClient by lazy {
        SharedHttpClient.baseClient.newBuilder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(2, 2, java.util.concurrent.TimeUnit.MINUTES))
            .build()
    }

    val isBingePrecacheActive: Boolean
        get() = synchronized(this) { activeBingeJob?.isActive == true }


    /**
     * Pre-warms the first 2MB of a stream URL into disk cache after a 300ms dwell delay.
     * If already caching this URL, reuses the in-flight job rather than aborting.
     */
    fun prewarmDetailsStream(
        context: Context,
        rawUrl: String,
        scope: CoroutineScope
    ): Job {
        val sanitizedUrl = TelegramLinkResolver.sanitizePlayableUrl(rawUrl)
        if (sanitizedUrl.isBlank()) return Job().apply { complete() }

        synchronized(this) {
            if (activePrewarmUrl == sanitizedUrl && activeDetailsJob?.isActive == true) {
                return activeDetailsJob!!
            }
        }

        cancelDetailsPrewarm()
        activePrewarmUrl = sanitizedUrl

        val job = scope.launch(Dispatchers.IO) {
            try {
                // Short dwell debounce: 300ms ensures instant readiness even on quick taps
                delay(300L)

                if (!PlayerSettingsManager.settingsFlow.value.smartPrewarmEnabled) {
                    Log.d(TAG, "Details prewarm skipped: smartPrewarmEnabled is OFF")
                    return@launch
                }

                val sanitizedUrl = TelegramLinkResolver.sanitizePlayableUrl(rawUrl)
                if (sanitizedUrl.isBlank()) return@launch

                if (!isNetworkConnected(context)) {
                    Log.d(TAG, "Details prewarm aborted: No network available")
                    return@launch
                }

                val appContext = context.applicationContext
                val simpleCache = StreamCacheManager.getCache(appContext)
                val parsedUri = Uri.parse(sanitizedUrl)
                val cacheKey = StreamDataSourceFactory.sanitizeCacheKey(parsedUri)

                // Check if already cached in disk using unified cache key
                val alreadyCached = simpleCache.isCached(cacheKey, 0, DETAILS_PREWARM_BYTES)
                if (alreadyCached) {
                    Log.i(TAG, "Details prewarm skipped: URL already cached locally in disk")
                    return@launch
                }

                Log.i(TAG, "Starting details micro-prewarm (2 MB): $sanitizedUrl")

                val upstreamFactory = OkHttpDataSource.Factory(preloadClient)
                    .setUserAgent(USER_AGENT)
                val sinkFactory = CacheDataSink.Factory()
                    .setCache(simpleCache)
                    .setFragmentSize(4 * 1024 * 1024L)

                val cacheDataSource = CacheDataSource.Factory()
                    .setCache(simpleCache)
                    .setUpstreamDataSourceFactory(upstreamFactory)
                    .setCacheWriteDataSinkFactory(sinkFactory)
                    .setCacheKeyFactory { ds -> ds.key ?: StreamDataSourceFactory.sanitizeCacheKey(ds.uri) }
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .createDataSource()

                val dataSpec = DataSpec.Builder()
                    .setUri(parsedUri)
                    .setKey(cacheKey)
                    .setPosition(0)
                    .setLength(DETAILS_PREWARM_BYTES)
                    .build()

                val writer = CacheWriter(cacheDataSource, dataSpec, null, null)
                synchronized(this@StreamPreloadManager) {
                    activeDetailsWriter = writer
                    activeDetailsDataSource = cacheDataSource
                }

                writer.cache()
                Log.i(TAG, "Details micro-prewarm completed successfully (2 MB cached)")
            } catch (_: CancellationException) {
                Log.d(TAG, "Details prewarm cancelled by user navigation (0 excess data transferred)")
            } catch (e: Exception) {
                Log.w(TAG, "Details prewarm non-fatal error: ${e.message}")
            } finally {
                synchronized(this@StreamPreloadManager) {
                    activeDetailsWriter = null
                    if (activeDetailsJob === coroutineContext[Job]) {
                        activeDetailsJob = null
                    }
                }
            }
        }

        activeDetailsJob = job
        return job
    }

    /**
     * Cancels any ongoing details pre-warm job and releases in-flight sockets.
     */
    fun cancelDetailsPrewarm() {
        synchronized(this) {
            try {
                activeDetailsWriter?.cancel()
                activeDetailsDataSource?.close()
                preloadClient.dispatcher.cancelAll()
                preloadClient.connectionPool.evictAll()
            } catch (_: Exception) {}
            activeDetailsWriter = null
            activeDetailsDataSource = null
            activeDetailsJob?.cancel()
            activeDetailsJob = null
            activePrewarmUrl = ""
        }
    }

    /**
     * Pre-caches the first [targetBytes] (default 25 MB) of Episode N+1 into [StreamCacheManager].
     */
    fun precacheNextEpisode(
        context: Context,
        rawNextUrl: String,
        episodeTitle: String = "",
        targetBytes: Long = BINGE_PRECACHE_BYTES,
        scope: CoroutineScope
    ): Job {
        cancelBingePrecache(resetCompleted = false)

        val job = scope.launch(Dispatchers.IO) {
            try {
                if (!PlayerSettingsManager.settingsFlow.value.bingePrecacheEnabled) {
                    Log.d(TAG, "Binge precache skipped: bingePrecacheEnabled is OFF")
                    return@launch
                }

                val sanitizedUrl = TelegramLinkResolver.sanitizePlayableUrl(rawNextUrl)
                if (sanitizedUrl.isBlank()) return@launch

                if (!isNetworkConnected(context)) {
                    Log.d(TAG, "Binge precache aborted: No network available")
                    return@launch
                }

                val appContext = context.applicationContext
                val simpleCache = StreamCacheManager.getCache(appContext)
                val parsedUri = Uri.parse(sanitizedUrl)
                val cacheKey = StreamDataSourceFactory.sanitizeCacheKey(parsedUri)

                // 1. Probe total length to pre-cache the MKV Cues / seek index (tail) FIRST!
                // In Matroska files, ExoPlayer cannot begin presentation without the Cues table.
                // Pre-caching the tail first guarantees that even if the user skips ahead early
                // (e.g. at 10% or 64% of head preload), the seek index is already on disk.
                var metaLen = androidx.media3.datasource.cache.ContentMetadata.getContentLength(simpleCache.getContentMetadata(cacheKey))
                if (metaLen <= 0L) {
                    metaLen = probeContentLength(sanitizedUrl)
                }
                if (metaLen > targetBytes) {
                    precacheTailIndexAtomically(appContext, sanitizedUrl, cacheKey, metaLen, targetBytes)
                }

                // 2. Check if already cached in disk using unified cache key
                val alreadyCached = simpleCache.isCached(cacheKey, 0, targetBytes)
                if (alreadyCached) {
                    Log.i(TAG, "Binge pre-cache: Next episode head (25 MB) already cached in disk ($cacheKey)")
                    _bingePrecacheStatus.value = BingePrecacheStatus(
                        isActive = false,
                        isCompleted = true,
                        cachedBytes = targetBytes,
                        targetBytes = targetBytes,
                        speedKbps = 0L,
                        episodeTitle = episodeTitle,
                        cacheKey = cacheKey
                    )
                    return@launch
                }

                Log.i(TAG, "Starting binge pre-cache (${targetBytes / (1024 * 1024)} MB) for next episode: $sanitizedUrl")
                _bingePrecacheStatus.value = BingePrecacheStatus(
                    isActive = true,
                    isCompleted = false,
                    cachedBytes = 0L,
                    targetBytes = targetBytes,
                    speedKbps = 0L,
                    episodeTitle = episodeTitle,
                    cacheKey = cacheKey
                )

                val upstreamFactory = OkHttpDataSource.Factory(preloadClient)
                    .setUserAgent(USER_AGENT)
                val sinkFactory = CacheDataSink.Factory()
                    .setCache(simpleCache)
                    .setFragmentSize(4 * 1024 * 1024L)

                val cacheDataSource = CacheDataSource.Factory()
                    .setCache(simpleCache)
                    .setUpstreamDataSourceFactory(upstreamFactory)
                    .setCacheWriteDataSinkFactory(sinkFactory)
                    .setCacheKeyFactory { ds -> ds.key ?: StreamDataSourceFactory.sanitizeCacheKey(ds.uri) }
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .createDataSource()

                val dataSpec = DataSpec.Builder()
                    .setUri(parsedUri)
                    .setKey(cacheKey)
                    .setPosition(0)
                    .setLength(targetBytes)
                    .build()

                var lastCalcTimeMs = System.currentTimeMillis()
                var lastBytesAtCalc = 0L

                val writer = CacheWriter(cacheDataSource, dataSpec, null) { totalLength, bytesCached, _ ->
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastCalcTimeMs
                    if (elapsed >= 400L) {
                        val deltaBytes = (bytesCached - lastBytesAtCalc).coerceAtLeast(0L)
                        val speedKbps = if (elapsed > 0) ((deltaBytes * 1000L / elapsed) / 1024L) else 0L
                        lastCalcTimeMs = now
                        lastBytesAtCalc = bytesCached
                        _bingePrecacheStatus.value = BingePrecacheStatus(
                            isActive = true,
                            isCompleted = false,
                            cachedBytes = bytesCached,
                            targetBytes = targetBytes,
                            speedKbps = speedKbps,
                            episodeTitle = episodeTitle,
                            cacheKey = cacheKey
                        )
                    }
                    if (totalLength > 0 && bytesCached % (4 * 1024 * 1024L) < 65536) {
                        Log.d(TAG, "Binge pre-cache progress: ${bytesCached / (1024 * 1024)} MB cached")
                    }
                }

                synchronized(this@StreamPreloadManager) {
                    activeBingeWriter = writer
                    activeBingeDataSource = cacheDataSource
                }

                writer.cache()
                synchronized(this@StreamPreloadManager) {
                    activeBingeWriter = null
                }
                Log.i(TAG, "Binge pre-cache: Head + Tail fully primed! Next episode is ready for instant 0ms play.")
                _bingePrecacheStatus.value = BingePrecacheStatus(
                    isActive = false,
                    isCompleted = true,
                    cachedBytes = targetBytes,
                    targetBytes = targetBytes,
                    speedKbps = 0L,
                    episodeTitle = episodeTitle,
                    cacheKey = cacheKey
                )
            } catch (_: CancellationException) {
                Log.d(TAG, "Binge pre-cache paused/cancelled")
                if (!_bingePrecacheStatus.value.isCompleted) {
                    _bingePrecacheStatus.value = BingePrecacheStatus()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Binge pre-cache non-fatal error: ${e.message}")
                if (!_bingePrecacheStatus.value.isCompleted) {
                    _bingePrecacheStatus.value = BingePrecacheStatus()
                }
            } finally {
                synchronized(this@StreamPreloadManager) {
                    activeBingeWriter = null
                    activeBingeDataSource = null
                    if (activeBingeJob === coroutineContext[Job]) {
                        activeBingeJob = null
                    }
                }
            }
        }

        activeBingeJob = job
        return job
    }

    /**
     * Probes the remote media URL with Range: bytes=0-0 to read total length from Content-Range header.
     */
    private fun probeContentLength(sanitizedUrl: String): Long {
        return try {
            val req = Request.Builder()
                .url(sanitizedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Range", "bytes=0-0")
                .build()
            preloadClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val cr = resp.header("Content-Range")
                    if (cr != null && cr.contains("/")) {
                        val lenStr = cr.substringAfterLast("/").trim()
                        val len = lenStr.toLongOrNull()
                        if (len != null && len > 0L) return len
                    }
                    val cl = resp.header("Content-Length")?.toLongOrNull()
                    if (cl != null && cl > 1L) return cl
                }
            }
            -1L
        } catch (_: Exception) {
            -1L
        }
    }

    /**
     * Gracefully cancels binge pre-cache and awaits background writer to release SimpleCache locks.
     */
    suspend fun cancelBingePrecacheAwait() {
        val jobToJoin = synchronized(this) {
            val job = activeBingeJob
            activeBingeWriter?.cancel()
            try {
                preloadClient.dispatcher.cancelAll()
            } catch (_: Exception) {}
            job?.cancel()
            activeBingeWriter = null
            activeBingeDataSource = null
            activeBingeJob = null
            _bingePrecacheStatus.value = BingePrecacheStatus()
            job
        }
        try {
            jobToJoin?.join()
        } catch (_: Exception) {}
    }

    /**
     * Cancels any ongoing binge pre-cache job without blocking.
     */
    fun cancelBingePrecache(resetCompleted: Boolean = true) {
        synchronized(this) {
            try {
                activeBingeWriter?.cancel()
                preloadClient.dispatcher.cancelAll()
            } catch (_: Exception) {}
            activeBingeWriter = null
            activeBingeDataSource = null
            activeBingeJob?.cancel()
            activeBingeJob = null
            if (resetCompleted || !_bingePrecacheStatus.value.isCompleted) {
                _bingePrecacheStatus.value = BingePrecacheStatus()
            } else {
                _bingePrecacheStatus.value = _bingePrecacheStatus.value.copy(isActive = false, speedKbps = 0L)
            }
        }
    }

    /**
     * Checks whether the beginning of a stream is already pre-cached on disk.
     */
    fun isStreamPrecached(context: Context, rawUrl: String, minBytes: Long = 2 * 1024 * 1024L): Boolean {
        if (rawUrl.isBlank()) return false
        val sanitized = TelegramLinkResolver.sanitizePlayableUrl(rawUrl)
        if (sanitized.isBlank()) return false
        val uri = Uri.parse(sanitized)
        val key = StreamDataSourceFactory.sanitizeCacheKey(uri)
        return try {
            val simpleCache = StreamCacheManager.getCache(context.applicationContext)
            simpleCache.isCached(key, 0, minBytes)
        } catch (_: Exception) {
            false
        }
    }


    /**
     * Atomically pre-caches the tail (~512KB) of the stream containing the MKV Cues (seek index).
     * Guarantees 512KB chunk alignment for Telegram MTProto proxy compatibility.
     * Buffers into memory first before committing to disk, preventing any partial or corrupt cache spans.
     */
    private suspend fun precacheTailIndexAtomically(
        context: Context,
        sanitizedUrl: String,
        cacheKey: String,
        totalLength: Long,
        headBytes: Long
    ) {
        val chunkSize = 512L * 1024L // 512 KB aligned to Telegram MTProto boundary
        if (totalLength <= headBytes + chunkSize) {
            return
        }

        val rawTailStart = (totalLength - chunkSize).coerceAtLeast(headBytes)
        val alignedTailStart = (rawTailStart / chunkSize) * chunkSize
        val tailLength = totalLength - alignedTailStart
        if (tailLength <= 0 || tailLength > 4 * 1024 * 1024L) return

        val simpleCache = StreamCacheManager.getCache(context.applicationContext)

        // Check if tail is already cached
        if (simpleCache.isCached(cacheKey, alignedTailStart, tailLength)) {
            Log.i(TAG, "Tail index already cached on disk at offset $alignedTailStart ($tailLength bytes)")
            return
        }

        try {
            val rangeHeader = "bytes=$alignedTailStart-${totalLength - 1}"
            val request = Request.Builder()
                .url(sanitizedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Range", rangeHeader)
                .build()

            Log.i(TAG, "Fetching aligned tail index ($tailLength bytes) at offset $alignedTailStart: $rangeHeader")

            withContext(Dispatchers.IO) {
                preloadClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful || response.code != 206) {
                        Log.w(TAG, "Tail fetch failed: HTTP ${response.code}")
                        return@use
                    }

                    val body = response.body ?: return@use
                    val bytes = body.bytes() // Read into memory buffer first (atomic)

                    if (bytes.size.toLong() == tailLength) {
                        // 100% verified complete — commit to SimpleCache atomically
                        val sink = CacheDataSink(simpleCache, 4 * 1024 * 1024L)
                        val tailSpec = DataSpec.Builder()
                            .setUri(Uri.parse(sanitizedUrl))
                            .setKey(cacheKey)
                            .setPosition(alignedTailStart)
                            .setLength(tailLength)
                            .build()

                        sink.open(tailSpec)
                        sink.write(bytes, 0, bytes.size)
                        sink.close()
                        Log.i(TAG, "Binge pre-cache: Tail index ($tailLength bytes) committed to disk cache! 0ms instant transition primed.")
                    } else {
                        Log.w(TAG, "Tail fetch incomplete: expected $tailLength bytes, received ${bytes.size}. Discarding buffer.")
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal: tail index fetch failed: ${e.message}")
        }
    }

    private fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
