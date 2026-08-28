package com.streamhub.app.data.api

import android.util.Log
import com.streamhub.app.data.models.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * High-Speed OmniArchiver-F2L REST API Client.
 *
 * Fetches batch episode manifests directly from the F2L Bot backend:
 * GET https://streamhub69.alwaysdata.net/api/batch/{batch_id}
 */
object F2lApiClient {

    private const val TAG = "F2lApiClient"
    private const val DEFAULT_BASE_URL = "https://streamhub69.alwaysdata.net/api/batch/"

    /**
     * Fetches batch episode manifest from F2L API.
     *
     * @param batchInput Either a full URL (e.g. "https://streamhub69.alwaysdata.net/api/batch/xyz")
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

        val requestUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "$DEFAULT_BASE_URL$trimmed"
        }

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

            val json = JSONObject(bodyString)
            val episodesArray = json.optJSONArray("episodes")
                ?: json.optJSONArray("data")
                ?: throw IllegalStateException("Invalid JSON response: missing 'episodes' array")

            val episodeList = mutableListOf<Episode>()
            for (i in 0 until episodesArray.length()) {
                val item = episodesArray.getJSONObject(i)
                val epNum = item.optInt("episode_num", item.optInt("episodeNumber", i + 1))
                val fileName = item.optString("file_name", item.optString("fileName", "Episode $epNum.mkv"))
                val sizeFormatted = item.optString("size_formatted", item.optString("file_size_formatted", ""))
                val fileSizeLong = item.optLong("file_size", 0L)
                val streamUrl = item.optString("stream_url", item.optString("streamUrl", ""))
                val downloadUrl = item.optString("download_url", item.optString("downloadUrl", streamUrl))
                val code = item.optString("code", item.optString("id", ""))

                // Clean title: remove .mkv, .mp4, and leading "EP - 01 - "
                val cleanTitle = fileName
                    .replace(Regex("""\.(mkv|mp4|avi|webm)$""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""(?i)^(?:>\s*🎬\s*|🎬\s*)?(?:EP|Episode)\s*[-:]?\s*0*\d+\s*[-:]?\s*"""), "")
                    .trim()

                val finalTitle = when {
                    cleanTitle.isNotBlank() && arcName.isNotBlank() -> "$arcName - Ep $epNum: $cleanTitle"
                    cleanTitle.isNotBlank() -> "Ep $epNum: $cleanTitle"
                    arcName.isNotBlank() -> "$arcName - Ep $epNum"
                    else -> "Episode $epNum"
                }

                // In-App Player uses direct download_url for robust byte-range 206 progressive playback
                val primaryPlayUrl = downloadUrl.ifBlank { streamUrl }
                val fallbackMirrorUrl = streamUrl.ifBlank { downloadUrl }
                val resolvedSize = if (sizeFormatted.isNotBlank()) sizeFormatted else formatBytes(fileSizeLong)

                if (primaryPlayUrl.isNotBlank()) {
                    episodeList.add(
                        Episode(
                            episodeNumber = epNum,
                            seasonNumber = seasonNumber,
                            arcName = arcName,
                            title = finalTitle,
                            streamUrl = primaryPlayUrl,
                            mirrorStreamUrl = fallbackMirrorUrl,
                            fileSize = resolvedSize,
                            fileName = fileName,
                            telegramFileId = code.ifBlank { epNum.toString() }
                        )
                    )
                }
            }

            episodeList.sortBy { it.episodeNumber }
            Log.d(TAG, "Successfully parsed ${episodeList.size} episodes from F2L API")
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
