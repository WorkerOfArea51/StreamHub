package com.streamhub.app.data

import android.util.Log
import com.streamhub.app.data.models.Episode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Direct Stream Link Resolver & Smart F2L / Bot Message Batch Importer.
 *
 * Handles:
 * 1. Smart Parsing of Telegram F2L Bot Output blocks (Episode #, Title, File Size, Stream URL, Download URL).
 * 2. JSON Batch payload parsing (from bot REST API `/api/batch/<id>`).
 * 3. Raw URL list sequential indexing (Ep 1..N).
 * 4. Batch sequential URL range generation.
 */
object TelegramLinkResolver {

    private const val TAG = "TelegramLinkResolver"
    private const val MAX_BATCH_SIZE = 500

    /**
     * Generates a list of batch links from start link to end link.
     */
    fun generateBatchTelegramLinks(startUrl: String, endUrl: String): String {
        val regex = Regex("""(https?://.+?/)(\d+)""")
        val startMatch = regex.find(startUrl.trim())
        val endMatch = regex.find(endUrl.trim())

        if (startMatch != null && endMatch != null) {
            val prefix = startMatch.groupValues[1]
            val startId = startMatch.groupValues[2].toLongOrNull() ?: 1L
            val endId = endMatch.groupValues[2].toLongOrNull() ?: startId

            val links = mutableListOf<String>()
            val minId = minOf(startId, endId)
            val maxId = maxOf(startId, endId)

            val rangeSize = maxId - minId + 1
            if (rangeSize > MAX_BATCH_SIZE) {
                return "Error: Batch range too large ($rangeSize links). Maximum is $MAX_BATCH_SIZE."
            }

            for (id in minId..maxId) {
                links.add("$prefix$id")
            }
            return links.joinToString("\n")
        } else if (startUrl.isNotBlank()) {
            return startUrl.trim()
        }
        return ""
    }

    /**
     * Smart Unified Parser for:
     * - Telegram F2L bot formatted message blocks
     * - JSON batch array / payload
     * - Raw URLs (one per line)
     */
    fun parseSmartBotMessageOrLinks(
        rawText: String,
        seasonNumber: Int = 1,
        arcName: String = ""
    ): List<Episode> {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return emptyList()

        // 1. JSON Format Check
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val jsonEpisodes = parseJsonBatch(trimmed, seasonNumber, arcName)
            if (jsonEpisodes.isNotEmpty()) return jsonEpisodes
        }

        // 2. Telegram Bot Formatted Block Message Parser
        // Matches:
        // > 🎬 EP - 01 - Undertaker.mkv (447.4 MB)
        // > 🔗 Stream:
        // https://streamhub69.alwaysdata.net/stream/xyz...
        // > ⬇️ Download:
        // https://streamhub69.alwaysdata.net/dl/xyz...
        val blockRegex = Regex(
            """(?s)(?:>\s*🎬\s*|🎬\s*)?(?:EP|Episode)\s*[-:]?\s*0*(\d+)\s*[-:]?\s*(.+?)(?:\s*\(([\d.]+\s*[KMGT]?B)\))?\s*\n.*?(?:Stream:?|🔗)\s*\n\s*(https?://\S+)(?:.*?(?:Download:?|⬇️|Mirror:?)\s*\n\s*(https?://\S+))?""",
            RegexOption.IGNORE_CASE
        )

        val matches = blockRegex.findAll(trimmed).toList()
        if (matches.isNotEmpty()) {
            return matches.map { match ->
                val epNum = match.groupValues[1].toIntOrNull() ?: 1
                val rawTitle = match.groupValues[2].trim()
                val size = match.groupValues[3].trim()
                val streamUrl = match.groupValues[4].trim()
                val dlUrl = match.groupValues.getOrNull(5)?.trim()?.ifBlank { null } ?: streamUrl

                // Clean title: strip .mkv, .mp4, trailing brackets
                val cleanTitle = rawTitle
                    .replace(Regex("""\.(mkv|mp4|avi|webm)$""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""^\s*-\s*"""), "")
                    .trim()

                val finalTitle = when {
                    cleanTitle.isNotBlank() && arcName.isNotBlank() -> "$arcName - Ep $epNum: $cleanTitle"
                    cleanTitle.isNotBlank() -> "Ep $epNum: $cleanTitle"
                    arcName.isNotBlank() -> "$arcName - Ep $epNum"
                    else -> "Episode $epNum"
                }

                Episode(
                    episodeNumber = epNum,
                    seasonNumber = seasonNumber,
                    arcName = arcName,
                    title = finalTitle,
                    streamUrl = streamUrl,
                    mirrorStreamUrl = dlUrl,
                    fileSize = size,
                    fileName = if (rawTitle.isNotBlank()) rawTitle else "Episode $epNum.mkv",
                    telegramFileId = extractTelegramMessageOrFileId(streamUrl)
                )
            }.sortedBy { it.episodeNumber }
        }

        // 3. Fallback: Parse Line by Line
        return parseAndGroupTelegramLinks(trimmed, seasonNumber, arcName)
    }

    private fun parseJsonBatch(jsonStr: String, seasonNumber: Int, arcName: String): List<Episode> {
        return try {
            val episodes = mutableListOf<Episode>()
            val array = if (jsonStr.startsWith("[")) {
                JSONArray(jsonStr)
            } else {
                val obj = JSONObject(jsonStr)
                obj.optJSONArray("episodes") ?: obj.optJSONArray("data") ?: JSONArray()
            }

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val epNum = item.optInt("episode_num", item.optInt("episodeNumber", i + 1))
                val title = item.optString("title", "Episode $epNum")
                val streamUrl = item.optString("direct_stream_url", item.optString("streamUrl", item.optString("url", "")))
                val dlUrl = item.optString("download_url", streamUrl)
                val size = item.optString("file_size", item.optString("fileSize", ""))
                val fileName = item.optString("file_name", item.optString("fileName", ""))

                if (streamUrl.isNotBlank()) {
                    episodes.add(
                        Episode(
                            episodeNumber = epNum,
                            seasonNumber = seasonNumber,
                            arcName = arcName,
                            title = if (arcName.isNotBlank()) "$arcName - Ep $epNum: $title" else title,
                            streamUrl = streamUrl,
                            mirrorStreamUrl = dlUrl,
                            fileSize = size,
                            fileName = fileName,
                            telegramFileId = extractTelegramMessageOrFileId(streamUrl)
                        )
                    )
                }
            }
            episodes.sortedBy { it.episodeNumber }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON batch", e)
            emptyList()
        }
    }

    /**
     * Parses raw batch stream links / message URLs or direct HTTP links
     * and automatically groups them into structured Episode objects.
     */
    fun parseAndGroupTelegramLinks(
        rawText: String,
        seasonNumber: Int = 1,
        arcName: String = ""
    ): List<Episode> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val episodes = mutableListOf<Episode>()

        lines.forEachIndexed { index, line ->
            if (line.startsWith("http://") || line.startsWith("https://") || line.startsWith("t.me/")) {
                val epNum = extractEpisodeNumber(line) ?: (index + 1)
                val epTitle = if (arcName.isNotBlank()) "$arcName - Ep $epNum" else "Episode $epNum"

                episodes.add(
                    Episode(
                        episodeNumber = epNum,
                        seasonNumber = seasonNumber,
                        arcName = arcName,
                        title = epTitle,
                        streamUrl = line,
                        mirrorStreamUrl = line,
                        telegramFileId = extractTelegramMessageOrFileId(line)
                    )
                )
            }
        }
        return episodes.sortedBy { it.episodeNumber }
    }

    suspend fun resolveAsync(url: String): String {
        return url
    }

    fun resolveSync(url: String): String {
        return url
    }

    fun isTelegramLink(url: String): Boolean {
        return url.contains("://t.me/") || url.startsWith("t.me/")
    }

    suspend fun fetchMetadataForEpisode(episode: Episode): Episode {
        return episode
    }

    private fun extractEpisodeNumber(text: String): Int? {
        val epRegex = Regex("""(?i)(?:ep|episode|s\d+e|\be)\s*[-:]?\s*0*(\d+)""")
        val match = epRegex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    fun extractTelegramMessageOrFileId(url: String): String {
        val msgRegex = Regex("""(?:t\.me/(?:c/\d+|[^/]+)/|watch/|dl/|stream/)([\w-]+)""")
        val match = msgRegex.find(url)
        return match?.groupValues?.get(1) ?: url.hashCode().toString()
    }
}
