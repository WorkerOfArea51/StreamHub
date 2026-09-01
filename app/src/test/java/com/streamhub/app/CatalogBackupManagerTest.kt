package com.streamhub.app

import com.streamhub.app.data.importer.CatalogBackupManager
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.data.models.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogBackupManagerTest {

    @Test
    fun testGenerateAndParseBackupJson() {
        val sampleCatalog = listOf(
            MediaItem(
                id = "solo_leveling_2024",
                title = "Solo Leveling",
                category = "ANIME",
                type = "ANIME",
                rating = "8.6",
                releaseYear = "2024",
                genres = listOf("Action", "Fantasy"),
                episodes = listOf(
                    Episode(
                        episodeNumber = 1,
                        seasonNumber = 1,
                        arcName = "D-Rank Dungeon",
                        title = "I'm Used to It",
                        streamUrl = "https://stream.example.com/ep1.mp4"
                    ),
                    Episode(
                        episodeNumber = 2,
                        seasonNumber = 1,
                        arcName = "D-Rank Dungeon",
                        title = "If I Had One More Chance",
                        streamUrl = "https://stream.example.com/ep2.mp4"
                    )
                ),
                mediaInfo = MediaInfo(resolution = "1080p FHD", videoCodec = "HEVC")
            ),
            MediaItem(
                id = "oppenheimer_2023",
                title = "Oppenheimer",
                category = "MOVIE",
                type = "MOVIE",
                rating = "8.9",
                releaseYear = "2023",
                genres = listOf("Biography", "Drama", "History"),
                episodes = listOf(
                    Episode(
                        episodeNumber = 1,
                        title = "Full Movie",
                        streamUrl = "https://stream.example.com/oppenheimer.mp4"
                    )
                )
            )
        )

        // 1. Export JSON
        val exportedJson = CatalogBackupManager.generateBackupJson(sampleCatalog)
        assertTrue(exportedJson.isNotBlank())
        assertTrue(exportedJson.contains("Solo Leveling"))
        assertTrue(exportedJson.contains("Oppenheimer"))
        assertTrue(exportedJson.contains("mediaCatalog"))
        assertTrue(exportedJson.contains("totalMediaCount\": 2"))
        assertTrue(exportedJson.contains("totalEpisodeCount\": 3"))

        // 2. Parse JSON back
        val parseResult = CatalogBackupManager.parseBackupJson(exportedJson)
        assertTrue(parseResult.isSuccess)

        val payload = parseResult.getOrNull()
        assertNotNull(payload)
        assertEquals(2, payload!!.mediaCatalog.size)
        assertEquals(3, payload.header.totalEpisodeCount)

        val anime = payload.mediaCatalog.first { it.id == "solo_leveling_2024" }
        assertEquals("Solo Leveling", anime.title)
        assertEquals(2, anime.episodes.size)
        assertEquals("D-Rank Dungeon", anime.episodes[0].arcName)
        assertEquals("1080p FHD", anime.mediaInfo.resolution)
    }

    @Test
    fun testParseDirectArrayFormat() {
        val jsonArray = """
            [
              {
                "id": "naruto_2002",
                "title": "Naruto",
                "category": "ANIME",
                "episodes": [
                  {
                    "episodeNumber": 1,
                    "title": "Enter: Naruto Uzumaki!",
                    "streamUrl": "https://stream.example.com/naruto_1.mp4"
                  }
                ]
              }
            ]
        """.trimIndent()

        val parseResult = CatalogBackupManager.parseBackupJson(jsonArray)
        assertTrue(parseResult.isSuccess)
        val payload = parseResult.getOrNull()
        assertNotNull(payload)
        assertEquals(1, payload!!.mediaCatalog.size)
        assertEquals("Naruto", payload.mediaCatalog[0].title)
        assertEquals(1, payload.header.totalEpisodeCount)
    }
}
