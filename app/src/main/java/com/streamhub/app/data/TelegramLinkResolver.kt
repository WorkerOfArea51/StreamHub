package com.streamhub.app.data

import android.util.Log
import com.streamhub.app.data.models.Episode

/**
 * Direct Stream Link Resolver & Batch URL Generator.
 *
 * Handles:
 * 1. Batch sequential URL generation (e.g. episode ranges 1..24).
 * 2. Parsing raw text lines into structured [Episode] objects.
 * 3. Direct HTTP / File-to-Link URL passthrough for high-performance streaming.
 */
object TelegramLinkResolver {

    private const val TAG = "TelegramLinkResolver"
    private const val MAX_BATCH_SIZE = 500

    /**
     * Generates a list of batch links from start link to end link.
     * E.g. start: "https://your-domain.alwaysdata.net/watch/7159", end: "https://your-domain.alwaysdata.net/watch/7170"
     * Returns 12 sequential links: 7159 through 7170.
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
        return episodes.sortedBy { it.episodeNumber }
    }

    suspend fun fetchMetadataForEpisode(episode: Episode): Episode {
        return episode
    }

    /**
     * Resolves a stream URL to a playable HTTP URL or local file path.
     */
    suspend fun resolveAsync(url: String): String {
        return url
    }

    fun resolveSync(url: String): String {
        return url
    }

    fun isTelegramLink(url: String): Boolean {
        return url.contains("://t.me/") || url.startsWith("t.me/")
    }

    private fun extractEpisodeNumber(text: String): Int? {
        val epRegex = Regex("""(?i)(?:ep|episode|s\d+e|\be)\s*(\d+)""")
        val match = epRegex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    fun extractTelegramMessageOrFileId(url: String): String {
        val msgRegex = Regex("""(?:t\.me/(?:c/\d+|[^/]+)/|watch/|dl/)(\d+)""")
        val match = msgRegex.find(url)
        return match?.groupValues?.get(1) ?: url.hashCode().toString()
    }
}
