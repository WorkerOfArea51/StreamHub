package com.streamhub.app

import com.streamhub.app.data.TelegramLinkResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramLinkResolverTest {

    @Test
    fun parseSmartBotMessage_exactBotFormat_returnsSortedEpisodesWithTitlesAndSizes() {
        val botMessage = """
            ✅ Batch Indexing Completed!

            📁 Category: ANIME
            📦 Total Episodes: 3

            > 🎬 EP - 01 - Undertaker.mkv (447.4 MB)
            > 🔗 Stream:
            https://streamhub69.alwaysdata.net/stream/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc
            > ⬇️ Download:
            https://streamhub69.alwaysdata.net/dl/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc

            > 🎬 EP - 02 - Spearhead.mkv (470.18 MB)
            > 🔗 Stream:
            https://streamhub69.alwaysdata.net/stream/1737eabf2d800438739c2d171cbeee18fe47f19fdc149412
            > ⬇️ Download:
            https://streamhub69.alwaysdata.net/dl/1737eabf2d800438739c2d171cbeee18fe47f19fdc149412

            > 🎬 EP - 03 - I Don't Want to Die.mkv (412.35 MB)
            > 🔗 Stream:
            https://streamhub69.alwaysdata.net/stream/cb72cb8cc3817357150c2519ddc68b59094c2474bc95ca42
            > ⬇️ Download:
            https://streamhub69.alwaysdata.net/dl/cb72cb8cc3817357150c2519ddc68b59094c2474bc95ca42
        """.trimIndent()

        val episodes = TelegramLinkResolver.parseSmartBotMessageOrLinks(botMessage, seasonNumber = 1)

        assertEquals(3, episodes.size)

        // Ep 1
        assertEquals(1, episodes[0].episodeNumber)
        assertEquals("Ep 1: Undertaker", episodes[0].title)
        assertEquals("447.4 MB", episodes[0].fileSize)
        assertEquals("https://streamhub69.alwaysdata.net/stream/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc", episodes[0].streamUrl)
        assertEquals("https://streamhub69.alwaysdata.net/dl/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc", episodes[0].mirrorStreamUrl)

        // Ep 2
        assertEquals(2, episodes[1].episodeNumber)
        assertEquals("Ep 2: Spearhead", episodes[1].title)
        assertEquals("470.18 MB", episodes[1].fileSize)

        // Ep 3
        assertEquals(3, episodes[2].episodeNumber)
        assertEquals("Ep 3: I Don't Want to Die", episodes[2].title)
        assertEquals("412.35 MB", episodes[2].fileSize)
    }

    @Test
    fun parseSmartBotMessage_f2lApiJson_returnsParsedEpisodes() {
        val jsonPayload = """
            {
              "status": "success",
              "batch_id": "eb76ab230b4d45ef32474cc414585f2471dc849fcf1c669d",
              "title": "ANIME Batch (2 episodes)",
              "category": "anime",
              "total_episodes": 2,
              "episodes": [
                {
                  "episode_num": 1,
                  "file_name": "EP - 01 - Undertaker.mkv",
                  "file_size": 469136888,
                  "size_formatted": "447.4 MB",
                  "mime_type": "video/x-matroska",
                  "stream_url": "https://streamhub69.alwaysdata.net/stream/eb76ab1",
                  "download_url": "https://streamhub69.alwaysdata.net/dl/eb76ab1",
                  "code": "eb76ab1"
                },
                {
                  "episode_num": 2,
                  "file_name": "EP - 02 - Spearhead.mkv",
                  "file_size": 493033553,
                  "size_formatted": "470.18 MB",
                  "mime_type": "video/x-matroska",
                  "stream_url": "https://streamhub69.alwaysdata.net/stream/eb76ab2",
                  "download_url": "https://streamhub69.alwaysdata.net/dl/eb76ab2",
                  "code": "eb76ab2"
                }
              ]
            }
        """.trimIndent()

        val episodes = TelegramLinkResolver.parseSmartBotMessageOrLinks(jsonPayload, seasonNumber = 1)

        assertEquals(2, episodes.size)
        assertEquals(1, episodes[0].episodeNumber)
        assertEquals("https://streamhub69.alwaysdata.net/dl/eb76ab1", episodes[0].streamUrl)
        assertEquals("https://streamhub69.alwaysdata.net/stream/eb76ab1", episodes[0].mirrorStreamUrl)
        assertEquals(2, episodes[1].episodeNumber)
    }

    @Test
    fun parseSmartBotMessage_rawLinks_returnsSequentialEpisodes() {
        val rawLinks = """
            https://streamhub69.alwaysdata.net/stream/link1
            https://streamhub69.alwaysdata.net/stream/link2
        """.trimIndent()

        val episodes = TelegramLinkResolver.parseSmartBotMessageOrLinks(rawLinks, seasonNumber = 1)

        assertEquals(2, episodes.size)
        assertEquals(1, episodes[0].episodeNumber)
        assertEquals(2, episodes[1].episodeNumber)
    }
}
