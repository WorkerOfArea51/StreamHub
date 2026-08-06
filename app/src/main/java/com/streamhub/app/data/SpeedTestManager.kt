package com.streamhub.app.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        val qualityRating: String // "4K Ultra HD", "1080p Full HD", "720p HD", "480p SD"
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
 *
 * The test endpoint should be a high-bandwidth CDN URL. Ideally this
 * would be a Telegram CDN streaming endpoint. Currently uses a public
 * image CDN as a reasonable proxy for streaming throughput.
 */
object SpeedTestManager {

    private const val TAG = "SpeedTestManager"

    /**
     * Managed coroutine scope. SupervisorJob ensures resilience.
     * Call [cancelAll] on app termination to clean up.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _testState = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val testState: StateFlow<SpeedTestState> = _testState.asStateFlow()

    /** CDN endpoint for bandwidth testing. Should be a high-bandwidth, low-latency URL. */
    private const val TEST_ENDPOINT = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1200"

    /** Shared HTTP client with appropriate timeouts for speed testing. */
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    fun runSpeedTest() {
        if (_testState.value is SpeedTestState.Testing) return
        _testState.value = SpeedTestState.Testing

        scope.launch {
            try {
                // Phase 1: Measure ping latency (3 HEAD requests)
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
            } catch (e: Exception) {
                Log.e(TAG, "Speed test failed", e)
                _testState.value = SpeedTestState.Error(e.localizedMessage ?: "Connection test failed")
            }
        }
    }

    /**
     * Measure round-trip latency via a HEAD request.
     * Returns the total time in milliseconds including connection + response.
     */
    private fun measurePingMillis(): Long {
        val request = Request.Builder()
            .url(TEST_ENDPOINT)
            .head()
            .build()

        val start = System.currentTimeMillis()
        httpClient.newCall(request).execute().use { response ->
            // Force reading the response to complete the round trip
            response.code
        }
        return System.currentTimeMillis() - start
    }

    /**
     * Measure download throughput by downloading the full test endpoint body.
     * Returns (bytesRead, durationMs).
     */
    private fun measureDownload(): Pair<Int, Long> {
        val request = Request.Builder()
            .url(TEST_ENDPOINT)
            .build()

        var bytesRead = 0
        val startTime = System.currentTimeMillis()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Download test failed: HTTP ${response.code}")
            }
            response.body?.byteStream()?.use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    bytesRead += read
                }
            } ?: throw Exception("Empty response body")
        }

        val durationMs = System.currentTimeMillis() - startTime
        return Pair(bytesRead, durationMs)
    }

    fun resetState() {
        _testState.value = SpeedTestState.Idle
    }

    /**
     * Cancel all in-flight speed tests. Call on app termination.
     */
    fun cancelAll() {
        scope.cancel()
    }
}
