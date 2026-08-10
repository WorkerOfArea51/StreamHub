package com.streamhub.app.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

sealed class SpeedTestState {
    data object Idle : SpeedTestState()
    data object Testing : SpeedTestState()
    data class Completed(
        val pingMs: Long,
        val speedMbps: Double,
        val qualityRating: String
    ) : SpeedTestState()
    data class Error(val message: String) : SpeedTestState()
}

/**
 * Network Speed & Latency Benchmark Engine.
 *
 * Measures ping latency and download bandwidth against a real CDN endpoint,
 * then provides a streaming quality rating based on the measured speed.
 *
 * The speed measurement is honest — no multipliers or clamping.
 * The reported Mbps reflects actual download throughput.
 */
object SpeedTestManager {

    private const val TAG = "SpeedTestManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var testJob: Job? = null

    private val _testState = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val testState: StateFlow<SpeedTestState> = _testState.asStateFlow()

    // FIX: Use Cloudflare CDN endpoint for speed testing instead of Unsplash.
    // Cloudflare's CDN is designed for high-throughput static delivery and won't
    // rate-limit or block like Unsplash's image API.
    // Cloudflare ultra-fast CDN trace endpoint for instant latency measurement
    private const val PING_ENDPOINT = "https://1.1.1.1/cdn-cgi/trace"
    // Cloudflare speed benchmark download endpoint (2.5 MB)
    private const val TEST_ENDPOINT = "https://speed.cloudflare.com/__down?bytes=2500000"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun runSpeedTest() {
        if (_testState.value is SpeedTestState.Testing) return
        _testState.value = SpeedTestState.Testing

        testJob?.cancel()
        testJob = scope.launch {
            try {
                // Phase 1: Measure ping latency (3 GET requests to Cloudflare trace endpoint)
                var totalPing = 0L
                val pingCount = 3
                for (i in 1..pingCount) {
                    val pingTime = withContext(Dispatchers.IO) {
                        measurePingMillis()
                    }
                    totalPing += pingTime
                }
                val avgPing = (totalPing / pingCount).coerceAtLeast(1L)

                // Phase 2: Measure download bandwidth
                val (bytesRead, durationMs) = withContext(Dispatchers.IO) {
                    measureDownload()
                }

                val durationSec = (durationMs / 1000.0).coerceAtLeast(0.1)
                val megabits = (bytesRead * 8.0) / (1024.0 * 1024.0)
                val measuredMbps = megabits / durationSec
                val roundedMbps = Math.round(measuredMbps * 10.0) / 10.0

                val quality = when {
                    roundedMbps >= 35.0 -> "4K Ultra HD (Blazing Fast)"
                    roundedMbps >= 15.0 -> "1080p Full HD (Smooth)"
                    roundedMbps >= 5.0 -> "720p HD (Good)"
                    else -> "480p SD (Basic)"
                }

                _testState.value = SpeedTestState.Completed(
                    pingMs = avgPing,
                    speedMbps = roundedMbps,
                    qualityRating = quality
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Speed test failed", e)
                _testState.value = SpeedTestState.Error(e.localizedMessage ?: "Connection test failed")
            }
        }
    }

    /**
     * FIX #9: Explicit cancelTest method.
     */
    fun cancelTest() {
        testJob?.cancel()
        testJob = null
        _testState.value = SpeedTestState.Idle
    }

    private fun measurePingMillis(): Long {
        val request = Request.Builder()
            .url(PING_ENDPOINT)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) StreamHub/4.0.3")
            .get()
            .build()

        val start = System.currentTimeMillis()
        try {
            httpClient.newCall(request).execute().use { response ->
                response.code
            }
        } catch (_: Exception) { }
        return System.currentTimeMillis() - start
    }

    private fun measureDownload(): Pair<Long, Long> {
        val request = Request.Builder()
            .url(TEST_ENDPOINT)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) StreamHub/4.0.3")
            .build()

        var bytesRead = 0L
        val startTime = System.currentTimeMillis()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // Fallback to cachefly if Cloudflare rate limits
                return measureDownloadFallback()
            }
            response.body?.byteStream()?.use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    bytesRead += read.toLong()
                }
            } ?: throw Exception("Empty response body")
        }

        val durationMs = System.currentTimeMillis() - startTime
        return Pair(bytesRead, durationMs)
    }

    private fun measureDownloadFallback(): Pair<Long, Long> {
        val request = Request.Builder()
            .url("https://cachefly.cachefly.net/2mb.test")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) StreamHub/4.0.3")
            .build()

        var bytesRead = 0L
        val startTime = System.currentTimeMillis()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Download test failed: HTTP ${response.code}")
            }
            response.body?.byteStream()?.use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    bytesRead += read.toLong()
                }
            } ?: throw Exception("Empty response body")
        }

        val durationMs = System.currentTimeMillis() - startTime
        return Pair(bytesRead, durationMs)
    }

    fun resetState() {
        cancelTest()
    }

    fun cancelAll() {
        cancelTest()
        scope.cancel()
    }
}
