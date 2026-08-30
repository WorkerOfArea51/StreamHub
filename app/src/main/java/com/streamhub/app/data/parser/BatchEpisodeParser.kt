package com.streamhub.app.data.parser

import com.streamhub.app.data.models.Episode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class EpisodeValidationResult(
    val isValid: Boolean,
    val totalCount: Int,
    val duplicateEpisodeNumbers: List<Int>,
    val emptyUrlEpisodeNumbers: List<Int>,
    val warningMessages: List<String>
)

/**
 * Intelligent Batch Episode Formatter & Parser for Creator Studio.
 *
 * Capabilities:
 * 1. Multi-format text/Telegram link parsing (URL extraction, episode regex, resolution detection).
 * 2. Sequential pattern generation with placeholder support ({n}, {ep}, {0n}).
 * 3. JSON array / F2L message parser.
 * 4. Validation and duplicate detection.
 */
object BatchEpisodeParser {

    private val URL_REGEX = Regex("https?://[^\\s\"'<>]+", RegexOption.IGNORE_CASE)
    private val EPISODE_NUM_PATTERNS = listOf(
        Regex("(?i)(?:episode|ep|e)[.\\s_-]*(\\d{1,4})"),
        Regex("\\[(\\d{1,4})\\]"),
        Regex("[-_\\s](\\d{1,4})[-_\\s]"),
        Regex("[-_\\s](\\d{1,4})\\.(?:mkv|mp4|webm|avi)")
    )
    private val RESOLUTION_REGEX = Regex("(?i)(4k|2160p|1080p|720p|480p|360p|fhd|hd)")

    /**
     * Parses arbitrary raw text (Telegram posts, plain link lists, markdown, etc.)
     */
    fun parseRawDump(
        rawText: String,
        defaultSeason: Int = 1,
        defaultArc: String = ""
    ): List<Episode> {
        if (rawText.isBlank()) return emptyList()

        // 1. Check if rawText is valid JSON array or object
        val trimmed = rawText.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val fromJson = parseJsonArray(trimmed, defaultSeason, defaultArc)
            if (fromJson.isNotEmpty()) return fromJson
        }

        // 2. Line-by-line or Token-based extraction
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val episodes = mutableListOf<Episode>()
        var autoIndex = 1

        for (line in lines) {
            val urls = URL_REGEX.findAll(line).map { it.value }.toList()
            if (urls.isEmpty()) {
                // Check if the line is an F2L token or raw path
                if (line.startsWith("/") || line.contains(".")) {
                    val epNum = extractEpisodeNumber(line) ?: autoIndex++
                    val epTitle = extractCleanTitle(line, epNum)
                    episodes.add(
                        Episode(
                            episodeNumber = epNum,
                            seasonNumber = defaultSeason,
                            arcName = defaultArc,
                            title = epTitle,
                            streamUrl = line,
                            fileName = File(line).name
                        )
                    )
                }
                continue
            }

            val streamUrl = urls.firstOrNull() ?: ""
            val mirrorUrl = if (urls.size > 1) urls[1] else ""

            // Extract episode number from the text line or URL
            val epNum = extractEpisodeNumber(line) ?: extractEpisodeNumber(streamUrl) ?: autoIndex++
            val resolution = RESOLUTION_REGEX.find(line)?.value?.uppercase() ?: RESOLUTION_REGEX.find(streamUrl)?.value?.uppercase() ?: ""
            val epTitle = extractCleanTitle(line, epNum, resolution)
            val fileName = extractFileName(line, streamUrl)

            episodes.add(
                Episode(
                    episodeNumber = epNum,
                    seasonNumber = defaultSeason,
                    arcName = defaultArc,
                    title = epTitle,
                    streamUrl = streamUrl,
                    mirrorStreamUrl = mirrorUrl,
                    fileName = fileName
                )
            )
        }

        // Return sorted by episode number
        return episodes.sortedBy { it.episodeNumber }
    }

    /**
     * Generates a sequential batch of episodes given a template URL or base pattern.
     * Supports placeholders:
     *   {n}   -> 1, 2, 3...
     *   {0n}  -> 01, 02, 03...
     *   {ep}  -> 1, 2, 3...
     */
    fun generateSequentialBatch(
        templateUrl: String,
        startEp: Int,
        endEp: Int,
        seasonNum: Int = 1,
        arcName: String = "",
        titleTemplate: String = "Episode {n}"
    ): List<Episode> {
        val start = startEp.coerceAtLeast(1)
        val end = endEp.coerceAtLeast(start)
        val episodes = mutableListOf<Episode>()

        for (ep in start..end) {
            val formattedEpNum2Digits = String.format("%02d", ep)
            val formattedEpNum3Digits = String.format("%03d", ep)

            val resolvedUrl = if (templateUrl.contains("{0n}") || templateUrl.contains("{00n}") || templateUrl.contains("{n}") || templateUrl.contains("{ep}")) {
                templateUrl
                    .replace("{00n}", formattedEpNum3Digits)
                    .replace("{0n}", formattedEpNum2Digits)
                    .replace("{n}", ep.toString())
                    .replace("{ep}", ep.toString())
            } else if (templateUrl.isNotBlank()) {
                val lastNumRegex = Regex("(\\d+)(?=[^\\d]*$)")
                if (lastNumRegex.containsMatchIn(templateUrl)) {
                    templateUrl.replace(lastNumRegex, ep.toString())
                } else {
                    templateUrl
                }
            } else {
                ""
            }

            val resolvedTitle = titleTemplate
                .replace("{0n}", formattedEpNum2Digits)
                .replace("{n}", ep.toString())
                .replace("{ep}", ep.toString())

            episodes.add(
                Episode(
                    episodeNumber = ep,
                    seasonNumber = seasonNum,
                    arcName = arcName,
                    title = resolvedTitle,
                    streamUrl = resolvedUrl,
                    fileName = if (resolvedUrl.isNotBlank()) File(resolvedUrl).name else "Episode_$ep.mp4"
                )
            )
        }

        return episodes
    }

    fun parseJsonArray(jsonString: String, defaultSeason: Int = 1, defaultArc: String = ""): List<Episode> {
        try {
            val array = JSONArray(jsonString)
            val result = mutableListOf<Episode>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val epNum = obj.optInt("episode_num", obj.optInt("episodeNumber", i + 1))
                val title = obj.optString("title", obj.optString("episode_title", "Episode $epNum"))
                val streamUrl = obj.optString("direct_stream_url", obj.optString("streamUrl", obj.optString("url", "")))
                val mirrorUrl = obj.optString("download_url", obj.optString("mirrorStreamUrl", ""))
                val fileName = obj.optString("file_name", obj.optString("fileName", ""))
                val fileSize = obj.optString("file_size", obj.optString("fileSize", ""))
                val arc = obj.optString("arc_name", defaultArc)
                val season = obj.optInt("season_num", defaultSeason)

                result.add(
                    Episode(
                        episodeNumber = epNum,
                        seasonNumber = season,
                        arcName = arc,
                        title = title,
                        streamUrl = streamUrl,
                        mirrorStreamUrl = mirrorUrl,
                        fileName = fileName,
                        fileSize = fileSize
                    )
                )
            }
            if (result.isNotEmpty()) return result.sortedBy { it.episodeNumber }
        } catch (_: Throwable) {
            // Fallback for JVM unit tests or unmocked org.json
        }

        // Pure Kotlin regex-based JSON objects parser fallback
        return parseJsonObjectsFallback(jsonString, defaultSeason, defaultArc)
    }

    fun toJsonString(episodes: List<Episode>): String {
        return buildString {
            append("[\n")
            episodes.forEachIndexed { index, ep ->
                append("  {\n")
                append("    \"episode_num\": ${ep.episodeNumber},\n")
                append("    \"season_num\": ${ep.seasonNumber},\n")
                if (ep.arcName.isNotBlank()) append("    \"arc_name\": \"${escapeJson(ep.arcName)}\",\n")
                append("    \"title\": \"${escapeJson(ep.title)}\",\n")
                append("    \"file_name\": \"${escapeJson(ep.fileName)}\",\n")
                append("    \"file_size\": \"${escapeJson(ep.fileSize)}\",\n")
                append("    \"direct_stream_url\": \"${escapeJson(ep.streamUrl)}\",\n")
                append("    \"download_url\": \"${escapeJson(ep.mirrorStreamUrl)}\"\n")
                append("  }")
                if (index < episodes.size - 1) append(",")
                append("\n")
            }
            append("]")
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun parseJsonObjectsFallback(json: String, defaultSeason: Int, defaultArc: String): List<Episode> {
        val objPattern = Regex("\\{([^}]+)\\}")
        val episodes = mutableListOf<Episode>()

        for (match in objPattern.findAll(json)) {
            val content = match.groupValues[1]
            val epNum = Regex("\"episode_num\"\\s*:\\s*(\\d+)").find(content)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("\"episodeNumber\"\\s*:\\s*(\\d+)").find(content)?.groupValues?.get(1)?.toIntOrNull()
                ?: (episodes.size + 1)
            val season = Regex("\"season_num\"\\s*:\\s*(\\d+)").find(content)?.groupValues?.get(1)?.toIntOrNull() ?: defaultSeason
            val title = Regex("\"title\"\\s*:\\s*\"([^\"]*)\"").find(content)?.groupValues?.get(1) ?: "Episode $epNum"
            val streamUrl = Regex("\"direct_stream_url\"\\s*:\\s*\"([^\"]*)\"").find(content)?.groupValues?.get(1)
                ?: Regex("\"streamUrl\"\\s*:\\s*\"([^\"]*)\"").find(content)?.groupValues?.get(1)
                ?: ""
            val downloadUrl = Regex("\"download_url\"\\s*:\\s*\"([^\"]*)\"").find(content)?.groupValues?.get(1)
                ?: Regex("\"mirrorStreamUrl\"\\s*:\\s*\"([^\"]*)\"").find(content)?.groupValues?.get(1)
                ?: ""
            val fileName = Regex("\"file_name\"\\s*:\\s*\"([^\"]*)\"").find(content)?.groupValues?.get(1) ?: ""
            val fileSize = Regex("\"file_size\"\\s*:\\s*\"([^\"]*)\"").find(content)?.groupValues?.get(1) ?: ""
            val arc = Regex("\"arc_name\"\\s*:\\s*\"([^\"]*)\"").find(content)?.groupValues?.get(1) ?: defaultArc

            episodes.add(
                Episode(
                    episodeNumber = epNum,
                    seasonNumber = season,
                    arcName = arc,
                    title = title,
                    streamUrl = streamUrl,
                    mirrorStreamUrl = downloadUrl,
                    fileName = fileName,
                    fileSize = fileSize
                )
            )
        }

        return episodes.sortedBy { it.episodeNumber }
    }

    fun validateEpisodes(episodes: List<Episode>): EpisodeValidationResult {
        if (episodes.isEmpty()) {
            return EpisodeValidationResult(
                isValid = false,
                totalCount = 0,
                duplicateEpisodeNumbers = emptyList(),
                emptyUrlEpisodeNumbers = emptyList(),
                warningMessages = listOf("No episodes found.")
            )
        }

        val duplicates = episodes.groupBy { it.episodeNumber }
            .filter { it.value.size > 1 }
            .keys.toList()

        val emptyUrls = episodes.filter { it.streamUrl.isBlank() && it.mirrorStreamUrl.isBlank() }
            .map { it.episodeNumber }

        val warnings = mutableListOf<String>()
        if (duplicates.isNotEmpty()) {
            warnings.add("Duplicate episode numbers detected: ${duplicates.joinToString(", ")}")
        }
        if (emptyUrls.isNotEmpty()) {
            warnings.add("Episodes without stream links: ${emptyUrls.joinToString(", ")}")
        }

        return EpisodeValidationResult(
            isValid = duplicates.isEmpty() && emptyUrls.isEmpty(),
            totalCount = episodes.size,
            duplicateEpisodeNumbers = duplicates,
            emptyUrlEpisodeNumbers = emptyUrls,
            warningMessages = warnings
        )
    }

    private fun extractEpisodeNumber(text: String): Int? {
        for (regex in EPISODE_NUM_PATTERNS) {
            val match = regex.find(text)
            if (match != null) {
                val numStr = match.groupValues[1]
                val num = numStr.toIntOrNull()
                if (num != null && num in 1..9999) return num
            }
        }
        return null
    }

    private fun extractCleanTitle(line: String, epNum: Int, resolution: String = ""): String {
        val withoutUrls = line.replace(URL_REGEX, "").trim()
        val cleaned = withoutUrls
            .replace(Regex("(?i)^(?:episode|ep|e)[.\\s_-]*\\d+[:\\s-]*"), "")
            .replace(Regex("(?i)\\.(?:mkv|mp4|webm|avi)$"), "")
            .replace(Regex("[_\\[\\]()]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return if (cleaned.isNotBlank() && cleaned.length > 2 && !cleaned.equals("Episode $epNum", ignoreCase = true)) {
            "Episode $epNum - $cleaned"
        } else if (resolution.isNotBlank()) {
            "Episode $epNum ($resolution)"
        } else {
            "Episode $epNum"
        }
    }

    private fun extractFileName(line: String, url: String): String {
        val candidate = if (url.isNotBlank()) {
            val clean = url.substringBefore("?").substringBefore("#")
            clean.substringAfterLast("/")
        } else {
            ""
        }
        if (candidate.isNotBlank() && candidate.contains(".")) return candidate
        val cleanLine = line.replace(URL_REGEX, "").trim()
        return if (cleanLine.isNotBlank()) cleanLine else candidate
    }
}
