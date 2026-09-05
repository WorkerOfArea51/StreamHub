package com.streamhub.app.data.api

import android.util.Log
import com.streamhub.app.data.StreamBackendConfig
import com.streamhub.app.data.models.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * High-Speed OmniArchiver-F2L REST API Client.
 *
 * Fetches batch episode manifests directly from the F2L Bot backend on Serv00 VPS:
 * GET https://midnighthawk.serv00.net/api/batch/{batch_id}
 */
object F2lApiClient {

    private const val TAG = "F2lApiClient"
    private const val DEFAULT_BASE_URL = StreamBackendConfig.DEFAULT_BATCH_API_BASE_URL

    /**
     * Fetches batch episode manifest from F2L API.
     *
     * @param batchInput Either a full URL (e.g. "https://midnighthawk.serv00.net/api/batch/xyz")
     *                   or a raw batch ID (e.g. "eb76ab230b4d45ef32474cc414585f2471dc849fcf1c669d").
     * @param seasonNumber Season number to assign to generated episodes.
     * @param arcName Optional saga/arc name to prefix episode titles with.
     */
    suspend fun fetchBatch(
        batchInput: String,
        seasonNumber: Int = 1,
        arcName: String = ""
    ): Result<List<Episode>> = withContext(Dispatchers.IO) {
        val trimmed = batchInput.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Batch ID or URL cannot be empty"))
        }

        val requestUrl = StreamBackendConfig.resolveBatchApiUrl(trimmed)

        runCatching {
            val request = Request.Builder()
                .url(requestUrl)
                .get()
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("F2L API returned HTTP ${response.code}: ${response.message}")
            }

            val bodyString = response.body?.string()
                ?: throw IllegalStateException("Empty response body from F2L API")

            val episodeList = com.streamhub.app.data.TelegramLinkResolver.parseSmartBotMessageOrLinks(
                rawText = bodyString,
                seasonNumber = seasonNumber,
                arcName = arcName
            )

            Log.d(TAG, "Successfully parsed ${episodeList.size} episodes from F2L API with durations & exact titles")
            episodeList
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}
