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

                val directPlayUrl = streamUrl.ifBlank { dlUrl }
                val fallbackMirrorUrl = dlUrl.ifBlank { streamUrl }

                Episode(
                    episodeNumber = epNum,
                    seasonNumber = seasonNumber,
                    arcName = arcName,
                    title = finalTitle,
                    streamUrl = sanitizePlayableUrl(directPlayUrl),
                    mirrorStreamUrl = sanitizePlayableUrl(fallbackMirrorUrl),
                    fileSize = size,
                    fileName = if (rawTitle.isNotBlank()) rawTitle else "Episode $epNum.mkv",
                    telegramFileId = extractTelegramMessageOrFileId(directPlayUrl)
                )
            }.sortedBy { it.episodeNumber }
        }

        // 3. Fallback: Parse Line by Line
        return parseAndGroupTelegramLinks(trimmed, seasonNumber, arcName)
    }

    /**
     * Parses JSON batch array or object from F2L / Stream bot REST API.
     */
    private fun parseJsonBatch(
        jsonString: String,
        seasonNumber: Int,
        arcName: String
    ): List<Episode> {
        return try {
            val episodes = mutableListOf<Episode>()
            val jsonElement = com.google.gson.JsonParser.parseString(jsonString)
            val jsonArray = when {
                jsonElement.isJsonArray -> jsonElement.asJsonArray
                jsonElement.isJsonObject -> {
                    val obj = jsonElement.asJsonObject
                    obj.getAsJsonArray("episodes") ?: obj.getAsJsonArray("files") ?: obj.getAsJsonArray("data") ?: com.google.gson.JsonArray()
                }
                else -> com.google.gson.JsonArray()
            }

            for (i in 0 until jsonArray.size()) {
                val item = jsonArray.get(i).asJsonObject
                val epNum = item.get("episode_num")?.asInt
                    ?: item.get("episode")?.asInt
                    ?: (i + 1)
                val fileName = item.get("file_name")?.asString
                    ?: item.get("name")?.asString
                    ?: item.get("fileName")?.asString
                    ?: ""
                val streamUrl = item.get("direct_stream_url")?.asString
                    ?: item.get("stream_url")?.asString
                    ?: item.get("streamUrl")?.asString
                    ?: item.get("url")?.asString
                    ?: ""
                val dlUrl = item.get("download_url")?.asString
                    ?: item.get("downloadUrl")?.asString
                    ?: streamUrl
                val size = item.get("size_formatted")?.asString
                    ?: item.get("file_size_formatted")?.asString
                    ?: item.get("file_size")?.asString
                    ?: item.get("fileSize")?.asString
                    ?: ""
                val rawTitle = item.get("title")?.asString
                    ?: fileName.replace(Regex("""\.(mkv|mp4|avi|webm)$""", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("""(?i)^(?:>\s*🎬\s*|🎬\s*)?(?:EP|Episode)\s*[-:]?\s*0*\d+\s*[-:]?\s*"""), "")
                        .trim()
                val code = item.get("code")?.asString ?: item.get("id")?.asString ?: ""

                val primaryPlayUrl = sanitizePlayableUrl(streamUrl.ifBlank { dlUrl })
                val mirrorUrl = sanitizePlayableUrl(dlUrl.ifBlank { streamUrl })

                if (primaryPlayUrl.isNotBlank()) {
                    val finalTitle = when {
                        rawTitle.isNotBlank() && arcName.isNotBlank() -> "$arcName - Ep $epNum: $rawTitle"
                        rawTitle.isNotBlank() -> "Ep $epNum: $rawTitle"
                        arcName.isNotBlank() -> "$arcName - Ep $epNum"
                        else -> "Episode $epNum"
                    }
                    episodes.add(
                        Episode(
                            episodeNumber = epNum,
                            seasonNumber = seasonNumber,
                            arcName = arcName,
                            title = finalTitle,
                            streamUrl = primaryPlayUrl,
                            mirrorStreamUrl = mirrorUrl,
                            fileSize = size,
                            fileName = fileName,
                            telegramFileId = code.ifBlank { extractTelegramMessageOrFileId(primaryPlayUrl) }
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
                // Ignore batch API endpoints from being parsed as raw video stream files
                if (line.contains("/api/batch", ignoreCase = true) || line.contains("/batch/", ignoreCase = true)) {
                    return@forEachIndexed
                }
                val epNum = extractEpisodeNumber(line) ?: (index + 1)
                val epTitle = if (arcName.isNotBlank()) "$arcName - Ep $epNum" else "Episode $epNum"
                val sanitizedUrl = sanitizePlayableUrl(line)

                episodes.add(
                    Episode(
                        episodeNumber = epNum,
                        seasonNumber = seasonNumber,
                        arcName = arcName,
                        title = epTitle,
                        streamUrl = sanitizedUrl,
                        mirrorStreamUrl = sanitizedUrl,
                        telegramFileId = extractTelegramMessageOrFileId(sanitizedUrl)
                    )
                )
            }
        }
        return episodes.sortedBy { it.episodeNumber }
    }

    fun sanitizePlayableUrl(url: String): String {
        return url.trim()
    }

    suspend fun resolveAsync(url: String): String {
        return sanitizePlayableUrl(url)
    }

    fun resolveSync(url: String): String {
        return sanitizePlayableUrl(url)
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
