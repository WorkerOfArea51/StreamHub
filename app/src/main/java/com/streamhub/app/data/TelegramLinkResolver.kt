package com.streamhub.app.data

import com.streamhub.app.data.models.Episode

object TelegramLinkResolver {

    private const val MAX_BATCH_SIZE = 500

    /**
     * Generates a list of batch Telegram links from start link to end link.
     * E.g. start: "https://t.me/c/2633457020/7159", end: "https://t.me/c/2633457020/7170"
     * Returns 12 links: 7159 through 7170.
     */
    fun generateBatchTelegramLinks(startUrl: String, endUrl: String): String {
        val regex = Regex("""(https?://t\.me/(?:c/\d+|[^/]+)/)(\d+)""")
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
     * Parses raw batch Telegram links / message URLs or stream links
     * and automatically groups them into structured Episode objects.
     */
    fun parseAndGroupTelegramLinks(rawText: String): List<Episode> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val episodes = mutableListOf<Episode>()

        lines.forEachIndexed { index, line ->
            val epNum = extractEpisodeNumber(line) ?: (index + 1)
            val streamUrl = convertToStreamUrl(line)
            val epTitle = "Episode $epNum"

            episodes.add(
                Episode(
                    episodeNumber = epNum,
                    title = epTitle,
                    streamUrl = streamUrl,
                    mirrorStreamUrl = line,
                    telegramFileId = extractTelegramMessageOrFileId(line)
                )
            )
        }
        return episodes.sortedBy { it.episodeNumber }
    }

    private fun extractEpisodeNumber(text: String): Int? {
        val epRegex = Regex("""(?i)(?:ep|episode|s\d+e|e)\s*(\d+)""")
        val match = epRegex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractTelegramMessageOrFileId(url: String): String {
        val msgRegex = Regex("""t\.me/(?:c/\d+|[^/]+)/(\d+)""")
        val match = msgRegex.find(url)
        return match?.groupValues?.get(1) ?: url.hashCode().toString()
    }

    private fun convertToStreamUrl(inputUrl: String): String {
        if (inputUrl.startsWith("http://") || inputUrl.startsWith("https://")) {
            return inputUrl
        }
        return "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
}
