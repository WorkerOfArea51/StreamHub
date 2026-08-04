package com.streamhub.app.data

import com.streamhub.app.data.models.Episode

object TelegramLinkResolver {

    /**
     * Parses raw batch Telegram links / message URLs or stream links
     * and automatically groups them into structured Episode objects.
     *
     * Supports formats:
     * - https://t.me/channel_name/101
     * - https://t.me/c/123456789/101
     * - https://api.telegram.org/file/bot<TOKEN>/<FILE_PATH>
     * - http://your-stream-proxy.com/stream?id=101
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
