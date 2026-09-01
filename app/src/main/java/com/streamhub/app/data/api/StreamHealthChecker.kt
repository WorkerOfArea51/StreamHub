package com.streamhub.app.data.api

import android.util.Log
import com.streamhub.app.data.models.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class EpisodeHealthResult(
    val episodeKey: String,
    val episodeNumber: Int,
    val arcName: String,
    val streamUrl: String,
    val isAlive: Boolean,
    val httpCode: Int,
    val latencyMs: Long,
    val contentType: String? = null,
    val errorMessage: String? = null
)

data class StreamHealthReport(
    val isChecking: Boolean = false,
    val totalCount: Int = 0,
    val checkedCount: Int = 0,
    val aliveCount: Int = 0,
    val deadCount: Int = 0,
    val results: Map<String, EpisodeHealthResult> = emptyMap()
)

object StreamHealthChecker {
    private const val TAG = "StreamHealthChecker"

    private val probeClient: OkHttpClient by lazy {
        SharedHttpClient.baseClient.newBuilder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(false)
            .build()
    }

    suspend fun probeUrl(url: String): EpisodeHealthResult {
        if (url.isBlank()) {
            return EpisodeHealthResult(
                episodeKey = "",
                episodeNumber = 0,
                arcName = "",
                streamUrl = "",
                isAlive = false,
                httpCode = 0,
                latencyMs = 0,
                errorMessage = "Stream URL is empty"
            )
        }

        val startTime = System.currentTimeMillis()
        val request = Request.Builder()
            .url(url.trim())
            .header("Range", "bytes=0-1024")
            .header("User-Agent", "StreamHub-HealthChecker/4.8")
            .build()

        return try {
            probeClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val code = response.code
                val contentType = response.header("Content-Type")
                val isSuccess = code in 200..399

                EpisodeHealthResult(
                    episodeKey = url,
                    episodeNumber = 0,
                    arcName = "",
                    streamUrl = url,
                    isAlive = isSuccess,
                    httpCode = code,
                    latencyMs = latency,
                    contentType = contentType,
                    errorMessage = if (!isSuccess) "HTTP $code ${response.message}" else null
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val msg = when (e) {
                is java.net.SocketTimeoutException -> "Connection Timed Out"
                is java.net.UnknownHostException -> "Server Not Found"
                else -> e.message ?: "Network Error"
            }
            EpisodeHealthResult(
                episodeKey = url,
                episodeNumber = 0,
                arcName = "",
                streamUrl = url,
                isAlive = false,
                httpCode = 0,
                latencyMs = latency,
                errorMessage = msg
            )
        }
    }

    fun checkEpisodesHealth(episodes: List<Episode>): Flow<StreamHealthReport> = flow {
        if (episodes.isEmpty()) {
            emit(StreamHealthReport(isChecking = false, totalCount = 0))
            return@flow
        }

        val total = episodes.size
        var checked = 0
        var alive = 0
        var dead = 0
        val resultMap = mutableMapOf<String, EpisodeHealthResult>()

        emit(
            StreamHealthReport(
                isChecking = true,
                totalCount = total,
                checkedCount = 0,
                aliveCount = 0,
                deadCount = 0,
                results = emptyMap()
            )
        )

        val semaphore = Semaphore(8)

        coroutineScope {
            val tasks = episodes.mapIndexed { idx, ep ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val key = "${ep.seasonNumber}_${ep.arcName}_${ep.episodeNumber}"
                        val singleResult = probeUrl(ep.streamUrl)
                        val epResult = singleResult.copy(
                            episodeKey = key,
                            episodeNumber = ep.episodeNumber,
                            arcName = ep.arcName
                        )
                        synchronized(resultMap) {
                            resultMap[key] = epResult
                            checked++
                            if (epResult.isAlive) alive++ else dead++
                        }
                        val snapshot = synchronized(resultMap) {
                            StreamHealthReport(
                                isChecking = checked < total,
                                totalCount = total,
                                checkedCount = checked,
                                aliveCount = alive,
                                deadCount = dead,
                                results = resultMap.toMap()
                            )
                        }
                        snapshot
                    }
                }
            }

            for (task in tasks) {
                val progress = task.await()
                emit(progress)
            }
        }

        emit(
            StreamHealthReport(
                isChecking = false,
                totalCount = total,
                checkedCount = checked,
                aliveCount = alive,
                deadCount = dead,
                results = resultMap.toMap()
            )
        )
    }.flowOn(Dispatchers.IO)
}
