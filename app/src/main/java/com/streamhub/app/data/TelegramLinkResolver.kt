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
            """(?s)(?:>\s*🎬\s*|🎬\s*)?((?:EP|Episode)\s*[-:]?\s*0*(\d+)\s*[-:]?\s*(.+?))(?:\s*\(([\d.]+\s*[KMGT]?B)\))?\s*\n.*?(?:Stream:?|🔗)\s*\n\s*(https?://\S+)(?:.*?(?:Download:?|⬇️|Mirror:?)\s*\n\s*(https?://\S+))?""",
            RegexOption.IGNORE_CASE
        )

        val matches = blockRegex.findAll(trimmed).toList()
        if (matches.isNotEmpty()) {
            return matches.map { match ->
                val fullRawTitle = match.groupValues[1].trim()
                val epNum = match.groupValues[2].toIntOrNull() ?: 1
                val size = match.groupValues[4].trim()
                val streamUrl = match.groupValues[5].trim()
                val dlUrl = match.groupValues.getOrNull(6)?.trim()?.ifBlank { null } ?: streamUrl

                // Clean title: strip .mkv, bot icons, underscores
                val cleanTitle = cleanEpisodeTitle(fullRawTitle, epNum)

                val finalTitle = when {
                    cleanTitle.isNotBlank() -> cleanTitle
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
                    fileName = if (fullRawTitle.isNotBlank()) "$fullRawTitle.mkv" else "Episode $epNum.mkv",
                    telegramFileId = extractTelegramMessageOrFileId(directPlayUrl)
                )
            }.sortedBy { it.episodeNumber }
        }

        // 3. Fallback: Parse Line by Line
        return parseAndGroupTelegramLinks(trimmed, seasonNumber, arcName)
    }

    /**
     * Extracts decimal or explicit episode numbers from filename/title (e.g. "11.5" from "EP - 11.5 - ...", "12" from "EP - 12 - ...")
     */
    fun extractEpisodeDisplayLabel(rawText: String, fallbackNumber: Int): String {
        // 1. Check for decimal episodes like 11.5, 0.5
        val decimalMatch = Regex("""(?i)\b(?:EP|Episode|E)?\s*[-_.:]?\s*(\d+\.\d+)\b""").find(rawText)
        if (decimalMatch != null) {
            return decimalMatch.groupValues[1]
        }

        // 2. Check for explicit episode number in filename (e.g. "EP - 12 - ", "EP 12 - ", "Episode 12 - ")
        val explicitEpMatch = Regex("""(?i)\b(?:EP|Episode|E)\s*[-_.:]?\s*0*(\d+)\b""").find(rawText)
        if (explicitEpMatch != null) {
            val num = explicitEpMatch.groupValues[1].toIntOrNull()
            if (num != null && num > 0) return num.toString()
        }

        return if (fallbackNumber > 0) fallbackNumber.toString() else "1"
    }

    /**
     * Cleans raw episode title / filename by stripping file extensions, bot icons,
     * while preserving the exact title and casing given by the creator.
     */
    fun cleanEpisodeTitle(rawText: String, episodeNumber: Int = 1): String {
        var title = rawText.trim()
        if (title.isBlank()) return ""

        // 1. Strip file extension (.mkv, .mp4, etc.)
        title = title.replace(Regex("""\.(?i)(mkv|mp4|avi|webm|ts|flv|mov|m4v|3gp|wmv|m2ts|vob)$"""), "")

        // 2. Strip leading bot markdown icons (e.g. "> 🎬 " or "🎬 ")
        title = title.replace(Regex("""^(?:>\s*🎬\s*|🎬\s*)"""), "")

        // 3. If the string contains underscores instead of spaces, convert underscores to spaces
        if (title.contains("_") && !title.contains(" ")) {
            title = title.replace("_", " ")
        }

        // 4. Clean leading and trailing punctuation & excess whitespace
        title = title.replace(Regex("""\s+"""), " ").trim()

        return if (title.isBlank() || title.equals("mkv", ignoreCase = true) || title.equals("mp4", ignoreCase = true)) {
            ""
        } else {
            title
        }
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
                val fileName = item.get("file_name")?.asString
                    ?: item.get("name")?.asString
                    ?: item.get("fileName")?.asString
                    ?: ""
                val rawTitleInput = item.get("title")?.asString ?: fileName
                val explicitNum = extractEpisodeNumber(rawTitleInput) ?: extractEpisodeNumber(fileName)
                val epNum = explicitNum
                    ?: item.get("episode_num")?.asInt
                    ?: item.get("episode")?.asInt
                    ?: (i + 1)
                val streamUrl = item.get("direct_stream_url")?.asString
                    ?: item.get("stream_link")?.asString
                    ?: item.get("stream_url")?.asString
                    ?: item.get("streamUrl")?.asString
                    ?: item.get("dl_link")?.asString
                    ?: item.get("url")?.asString
                    ?: ""
                val dlUrl = item.get("download_url")?.asString
                    ?: item.get("dl_link")?.asString
                    ?: item.get("downloadUrl")?.asString
                    ?: streamUrl
                val size = item.get("size_formatted")?.asString
                    ?: item.get("file_size_formatted")?.asString
                    ?: item.get("file_size")?.let { elem ->
                        if (elem.isJsonPrimitive && elem.asJsonPrimitive.isNumber) {
                            formatBytesToReadable(elem.asLong)
                        } else {
                            val str = elem.asString
                            val numeric = str.toLongOrNull()
                            if (numeric != null && numeric > 10000) formatBytesToReadable(numeric) else str
                        }
                    }
                    ?: item.get("fileSize")?.asString
                    ?: ""
                val cleanedTitle = cleanEpisodeTitle(rawTitleInput, epNum)
                val code = item.get("code")?.asString ?: item.get("id")?.asString ?: ""

                val durationMs = when {
                    item.has("duration_ms") -> item.get("duration_ms").asLong
                    item.has("duration_sec") -> (item.get("duration_sec").asDouble.toLong()) * 1000L
                    item.has("duration") && item.get("duration").isJsonPrimitive -> {
                        val prim = item.get("duration").asJsonPrimitive
                        val d = if (prim.isNumber) prim.asDouble.toLong() else prim.asString.toDoubleOrNull()?.toLong() ?: 0L
                        if (d > 10000) d else d * 1000L
                    }
                    item.has("duration_formatted") -> {
                        val str = item.get("duration_formatted").asString
                        val parts = str.split(":").mapNotNull { it.toLongOrNull() }
                        when (parts.size) {
                            2 -> (parts[0] * 60 + parts[1]) * 1000L
                            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
                            else -> 0L
                        }
                    }
                    else -> 0L
                }

                val primaryPlayUrl = sanitizePlayableUrl(streamUrl.ifBlank { dlUrl })
                val mirrorUrl = sanitizePlayableUrl(dlUrl.ifBlank { streamUrl })

                if (primaryPlayUrl.isNotBlank()) {
                    val finalTitle = when {
                        cleanedTitle.isNotBlank() -> cleanedTitle
                        else -> "Episode $epNum"
                    }
                    val itemArcName = item.get("arc_name")?.asString?.takeIf { it.isNotBlank() } ?: arcName
                    val itemSeasonNum = item.get("season_num")?.asInt ?: item.get("seasonNumber")?.asInt ?: seasonNumber

                    episodes.add(
                        Episode(
                            episodeNumber = epNum,
                            seasonNumber = itemSeasonNum,
                            arcName = itemArcName,
                            title = finalTitle,
                            streamUrl = primaryPlayUrl,
                            mirrorStreamUrl = mirrorUrl,
                            fileSize = size,
                            fileName = fileName.ifBlank { "Episode $epNum.mkv" },
                            durationMs = durationMs,
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
                val epTitle = "Episode $epNum"
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

    /**
     * Sanitizes a URL for playback.
     *
     * Ensures any web-player landing page route (`/stream/`) on the F2L bot backend
     * is resolved to the direct binary media stream route (`/dl/`), which serves
     * HTTP 206 Partial Content (Accept-Ranges: bytes, video/x-matroska/mp4).
     * Also performs cosmetic cleanup (trim + strip invisible/zero-width characters).
     */
    fun sanitizePlayableUrl(url: String): String {
        val cleaned = url.trim().replace(Regex("""[\s\u200B-\u200D\uFEFF]"""), "")
        if (cleaned.contains("alwaysdata.net/stream/", ignoreCase = true)) {
            return cleaned.replace(Regex("""(?i)alwaysdata\.net/stream/"""), "alwaysdata.net/dl/")
        }
        return cleaned
    }

    /**
     * Returns the download-route twin (`/dl/`) of an F2L link — used by the
     * DOWNLOAD pipeline, which wants the attachment endpoint.
     *
     * Scoped strictly to the F2L backend host so unrelated direct links
     * (other hosts, YouTube-resolved URLs, local files) are never rewritten.
     */
    fun toDownloadUrl(url: String): String {
        val trimmed = url.trim()
        if (!trimmed.contains("alwaysdata.net", ignoreCase = true)) return trimmed
        return if (trimmed.contains("/stream/", ignoreCase = true)) {
            trimmed.replace(Regex("""(?i)/stream/"""), "/dl/")
        } else {
            trimmed
        }
    }

    /**
     * Derives a safe playable mirror URL.
     *
     * Ensures /stream/ web landing pages are converted to /dl/ direct media stream.
     * Never converts /dl/ to /stream/ since /stream/ is an HTML web-player page.
     */
    fun deriveMirrorUrl(url: String): String {
        val trimmed = url.trim()
        if (!trimmed.contains("alwaysdata.net", ignoreCase = true)) return ""
        return if (trimmed.contains("/stream/", ignoreCase = true)) {
            trimmed.replace(Regex("""(?i)/stream/"""), "/dl/")
        } else {
            ""
        }
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
        if (Regex("""(?i)\b(?:EP|Episode|E)?\s*[-_.:]?\s*\d+\.\d+\b""").containsMatchIn(text)) {
            return null
        }
        val epRegex = Regex("""(?i)(?:ep|episode|s\d+e)\s*[-:]?\s*0*(\d+)\b""")
        val match = epRegex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    fun extractTelegramMessageOrFileId(url: String): String {
        val msgRegex = Regex("""(?:t\.me/(?:c/\d+|[^/]+)/|watch/|dl/|stream/)([\w-]+)""")
        val match = msgRegex.find(url)
        return match?.groupValues?.get(1) ?: url.hashCode().toString()
    }

    fun formatBytesToReadable(bytes: Long): String {
        if (bytes <= 0) return ""
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1000.0) {
            String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            String.format(java.util.Locale.US, "%.2f MB", mb)
        }
    }
}
