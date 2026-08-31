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
        assertEquals("EP - 01 - Undertaker", episodes[0].title)
        assertEquals("447.4 MB", episodes[0].fileSize)
        assertEquals("https://streamhub69.alwaysdata.net/dl/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc", episodes[0].streamUrl)
        assertEquals("https://streamhub69.alwaysdata.net/dl/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc", episodes[0].mirrorStreamUrl)

        // Ep 2
        assertEquals(2, episodes[1].episodeNumber)
        assertEquals("EP - 02 - Spearhead", episodes[1].title)
        assertEquals("470.18 MB", episodes[1].fileSize)

        // Ep 3
        assertEquals(3, episodes[2].episodeNumber)
        assertEquals("EP - 03 - I Don't Want to Die", episodes[2].title)
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
        assertEquals("https://streamhub69.alwaysdata.net/dl/eb76ab1", episodes[0].mirrorStreamUrl)
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

    @Test
    fun getFranchiseTag_johnWickFranchise_computesDynamicPrequelAndSequelCorrectly() {
        val jw1 = com.streamhub.app.data.models.MediaItem(id = "jw1", title = "John Wick", releaseYear = "2014", category = "MOVIE")
        val jw2 = com.streamhub.app.data.models.MediaItem(id = "jw2", title = "John Wick: Chapter 2", releaseYear = "2017", category = "MOVIE")
        val jw3 = com.streamhub.app.data.models.MediaItem(id = "jw3", title = "John Wick: Chapter 3 - Parabellum", releaseYear = "2019", category = "MOVIE")
        val jw4 = com.streamhub.app.data.models.MediaItem(id = "jw4", title = "John Wick: Chapter 4", releaseYear = "2023", category = "MOVIE")

        // When viewing John Wick 1 (2014)
        assertEquals("CURRENT • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw1, jw1))
        assertEquals("SEQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw2, jw1))
        assertEquals("SEQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw3, jw1))
        assertEquals("SEQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw4, jw1))

        // When viewing John Wick: Chapter 2 (2017)
        assertEquals("PREQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw1, jw2))
        assertEquals("CURRENT • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw2, jw2))
        assertEquals("SEQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw3, jw2))
        assertEquals("SEQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw4, jw2))

        // When viewing John Wick: Chapter 3 (2019)
        assertEquals("PREQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw1, jw3))
        assertEquals("PREQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw2, jw3))
        assertEquals("CURRENT • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw3, jw3))
        assertEquals("SEQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw4, jw3))

        // When viewing John Wick: Chapter 4 (2023)
        assertEquals("PREQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw1, jw4))
        assertEquals("PREQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw2, jw4))
        assertEquals("PREQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw3, jw4))
        assertEquals("CURRENT • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jw4, jw4))
    }

    @Test
    fun getFranchiseTag_explicitPrequelMovie_ordersBeforeMainSeasons() {
        val jjk0 = com.streamhub.app.data.models.MediaItem(id = "jjk0", title = "Jujutsu Kaisen 0", releaseYear = "2021", category = "MOVIE", relationType = "Prequel")
        val jjkS1 = com.streamhub.app.data.models.MediaItem(id = "jjkS1", title = "Jujutsu Kaisen Season 1", releaseYear = "2020", category = "Anime", seasonNumber = 1)
        val jjkS2 = com.streamhub.app.data.models.MediaItem(id = "jjkS2", title = "Jujutsu Kaisen Season 2", releaseYear = "2023", category = "Anime", seasonNumber = 2)

        // When viewing Jujutsu Kaisen Season 1
        assertEquals("PREQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jjk0, jjkS1))
        assertEquals("CURRENT • TV", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jjkS1, jjkS1))
        assertEquals("SEQUEL • TV", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jjkS2, jjkS1))

        // When viewing Jujutsu Kaisen 0 (The Prequel Movie)
        assertEquals("CURRENT • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jjk0, jjk0))
        assertEquals("SEQUEL • TV", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jjkS1, jjk0))
        assertEquals("SEQUEL • TV", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jjkS2, jjk0))

        // When viewing Jujutsu Kaisen Season 2
        assertEquals("PREQUEL • MOVIE", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jjk0, jjkS2))
        assertEquals("PREQUEL • TV", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jjkS1, jjkS2))
        assertEquals("CURRENT • TV", com.streamhub.app.data.FranchiseManager.getFranchiseTag(jjkS2, jjkS2))
    }

    @Test
    fun cleanEpisodeTitle_variousDirtyFormats_cleansToCleanReadableTitle() {
        // Underscored filename with EP_03_ prefix
        assertEquals(
            "EP 03 The Circumstances of the Classic Lit Club's Scion",
            TelegramLinkResolver.cleanEpisodeTitle("EP_03_The_Circumstances_of_the_Classic_Lit_Club's_Scion.mkv", 3)
        )

        // Standard hyphenated format
        assertEquals(
            "EP - 01 - The Return of the Time-Honored Classic Lit Club",
            TelegramLinkResolver.cleanEpisodeTitle("EP - 01 - The Return of the Time-Honored Classic Lit Club.mkv", 1)
        )
    }

    @Test
    fun parseSmartBotMessage_underscoredJsonPayload_parsesCleanTitlesAndDuration() {
        val jsonPayload = """
            [
                {
                    "episode_num": 3,
                    "file_name": "EP_03_The_Circumstances_of_the_Classic_Lit_Club's_Scion.mkv",
                    "file_size": "324.98 MB",
                    "direct_stream_url": "https://streamhub69.alwaysdata.net/dl/ep3",
                    "duration_sec": 1500
                }
            ]
        """.trimIndent()

        val episodes = TelegramLinkResolver.parseSmartBotMessageOrLinks(jsonPayload, seasonNumber = 1)
        assertEquals(1, episodes.size)
        assertEquals(3, episodes[0].episodeNumber)
        assertEquals("EP 03 The Circumstances of the Classic Lit Club's Scion", episodes[0].title)
        assertEquals(1500000L, episodes[0].durationMs)
    }

    @Test
    fun parseSmartBotMessage_decimalSpecialEpisode_parsesEp11_5Correctly() {
        val jsonPayload = """
            [
                {
                    "episode_num": 12,
                    "file_name": "EP - 11.5 - What Should Be Had (Special).mkv",
                    "file_size": 344888728,
                    "dl_link": "https://streamhub69.alwaysdata.net/dl/ep11_5",
                    "stream_link": "https://streamhub69.alwaysdata.net/stream/ep11_5"
                }
            ]
        """.trimIndent()

        val episodes = TelegramLinkResolver.parseSmartBotMessageOrLinks(jsonPayload, seasonNumber = 1)
        assertEquals(1, episodes.size)
        assertEquals(12, episodes[0].episodeNumber)
        assertEquals("EP - 11.5 - What Should Be Had (Special)", episodes[0].title)
        assertEquals("328.91 MB", episodes[0].fileSize)
    }

    @Test
    fun parseSmartBotMessage_episodesAfterSpecial_matchesFilenameEpisodeNumbers() {
        val jsonPayload = """
            [
                {
                    "episode_num": 12,
                    "file_name": "EP - 11.5 - What Should Be Had (Special).mkv",
                    "file_size": 344888728,
                    "dl_link": "https://streamhub69.alwaysdata.net/dl/ep11_5"
                },
                {
                    "episode_num": 13,
                    "file_name": "EP - 12 - Those Things Piled Up Endlessly.mkv",
                    "file_size": 444188728,
                    "dl_link": "https://streamhub69.alwaysdata.net/dl/ep12"
                },
                {
                    "episode_num": 14,
                    "file_name": "EP - 13 - A Corpse by Evening.mkv",
                    "file_size": 406188728,
                    "dl_link": "https://streamhub69.alwaysdata.net/dl/ep13"
                }
            ]
        """.trimIndent()

        val episodes = TelegramLinkResolver.parseSmartBotMessageOrLinks(jsonPayload, seasonNumber = 1)
        assertEquals(3, episodes.size)
        assertEquals("EP - 11.5 - What Should Be Had (Special)", episodes[0].title)
        assertEquals("EP - 12 - Those Things Piled Up Endlessly", episodes[1].title)
        assertEquals("EP - 13 - A Corpse by Evening", episodes[2].title)
    }

    @Test
    fun parseSmartBotMessage_floatDurationAndFormatted_parsesDurationCorrectly() {
        val jsonPayload = """
            {
              "status": "success",
              "batch_id": "9af0276c674e15892cfdb7627086d8e5",
              "episodes": [
                {
                  "code": "0d07b93b37770e5c2f3ea796cb43268dba85886553895acc",
                  "download_url": "https://streamhub69.alwaysdata.net/dl/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc",
                  "duration": 1421.0,
                  "duration_formatted": "23:41",
                  "episode_num": 1,
                  "file_name": "EP - 01 - Undertaker.mkv",
                  "file_size": 469136808,
                  "size_formatted": "447.4 MB"
                }
              ]
            }
        """.trimIndent()

        val episodes = TelegramLinkResolver.parseSmartBotMessageOrLinks(jsonPayload, seasonNumber = 1)
        assertEquals(1, episodes.size)
        assertEquals("EP - 01 - Undertaker", episodes[0].title)
        assertEquals(1421000L, episodes[0].durationMs)
        assertEquals("447.4 MB", episodes[0].fileSize)
    }

    @Test
    fun parseSmartBotMessage_explicitEpisodeNumberFromTitle_overridesBatchIndex() {
        val jsonPayload = """
            {
              "status": "success",
              "episodes": [
                {
                  "episode_num": 1,
                  "file_name": "EP - 14 - Dungeon.mkv",
                  "title": "EP - 14 - Dungeon",
                  "download_url": "https://streamhub69.alwaysdata.net/dl/xyz"
                },
                {
                  "episode_num": 2,
                  "file_name": "EP - 15 - The Diamond Mage.mkv",
                  "title": "EP - 15 - The Diamond Mage",
                  "download_url": "https://streamhub69.alwaysdata.net/dl/abc"
                }
              ]
            }
        """.trimIndent()

        val episodes = TelegramLinkResolver.parseSmartBotMessageOrLinks(jsonPayload, seasonNumber = 1)
        assertEquals(2, episodes.size)
        assertEquals(14, episodes[0].episodeNumber)
        assertEquals("EP - 14 - Dungeon", episodes[0].title)
        assertEquals(15, episodes[1].episodeNumber)
        assertEquals("EP - 15 - The Diamond Mage", episodes[1].title)
    }

    @Test
    fun parseSmartBotMessage_preservesJsonArcNameOverFallback() {
        val jsonPayload = """
            [
                {
                    "episode_num": 1,
                    "arc_name": "Magic Knights Entrance Arc",
                    "title": "EP - 01 - Asta and Yuno",
                    "stream_link": "https://streamhub69.alwaysdata.net/stream/ep1"
                },
                {
                    "episode_num": 14,
                    "arc_name": "Dungeon Exploration Arc",
                    "title": "EP - 14 - Dungeon",
                    "stream_link": "https://streamhub69.alwaysdata.net/stream/ep14"
                }
            ]
        """.trimIndent()

        val episodes = TelegramLinkResolver.parseSmartBotMessageOrLinks(jsonPayload, seasonNumber = 1, arcName = "Fallback Arc")
        assertEquals(2, episodes.size)
        assertEquals("Magic Knights Entrance Arc", episodes[0].arcName)
        assertEquals("Dungeon Exploration Arc", episodes[1].arcName)
    }
}
