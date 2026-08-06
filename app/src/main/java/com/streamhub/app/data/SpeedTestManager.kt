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

    private const val TEST_ENDPOINT = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1200"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    fun runSpeedTest() {
        if (_testState.value is SpeedTestState.Testing) return
        _testState.value = SpeedTestState.Testing

        testJob = scope.launch {
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
     * FIX #9: Explicit cancelTest method.
     */
    fun cancelTest() {
        testJob?.cancel()
        testJob = null
        _testState.value = SpeedTestState.Idle
    }

    private fun measurePingMillis(): Long {
        val request = Request.Builder()
            .url(TEST_ENDPOINT)
            .head()
            .build()

        val start = System.currentTimeMillis()
        httpClient.newCall(request).execute().use { response ->
            response.code
        }
        return System.currentTimeMillis() - start
    }

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
        cancelTest()
    }

    fun cancelAll() {
        cancelTest()
        scope.cancel()
    }
}
