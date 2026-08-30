package com.streamhub.app

import com.streamhub.app.data.parser.BatchEpisodeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchEpisodeParserTest {

    @Test
    fun parseRawDump_extractsSequentialEpisodesFromLinks() {
        val rawDump = """
            https://example.com/anime/jujutsu_kaisen_ep01_1080p.mp4
            https://example.com/anime/jujutsu_kaisen_ep02_1080p.mp4
            https://example.com/anime/jujutsu_kaisen_ep03_1080p.mp4
        """.trimIndent()

        val parsed = BatchEpisodeParser.parseRawDump(rawDump, defaultSeason = 2, defaultArc = "Shibuya")
        assertEquals(3, parsed.size)
        assertEquals(1, parsed[0].episodeNumber)
        assertEquals(2, parsed[1].episodeNumber)
        assertEquals(3, parsed[2].episodeNumber)
        assertEquals(2, parsed[0].seasonNumber)
        assertEquals("Shibuya", parsed[0].arcName)
        assertEquals("https://example.com/anime/jujutsu_kaisen_ep01_1080p.mp4", parsed[0].streamUrl)
    }

    @Test
    fun generateSequentialBatch_replacesPlaceholdersAccurately() {
        val template = "https://cdn.anime.io/stream/naruto_ep_{0n}.mp4"
        val generated = BatchEpisodeParser.generateSequentialBatch(
            templateUrl = template,
            startEp = 1,
            endEp = 12,
            seasonNum = 1,
            arcName = "Prologue",
            titleTemplate = "Episode {n}"
        )

        assertEquals(12, generated.size)
        assertEquals(1, generated[0].episodeNumber)
        assertEquals("https://cdn.anime.io/stream/naruto_ep_01.mp4", generated[0].streamUrl)
        assertEquals("Episode 1", generated[0].title)
        assertEquals("https://cdn.anime.io/stream/naruto_ep_12.mp4", generated[11].streamUrl)
        assertEquals("Episode 12", generated[11].title)
    }

    @Test
    fun validateEpisodes_detectsDuplicatesAndEmptyUrls() {
        val dump = """
            Ep 1: https://example.com/1.mp4
            Ep 1: https://example.com/1_dup.mp4
            Ep 2:
        """.trimIndent()

        val parsed = BatchEpisodeParser.parseRawDump(dump)
        val validation = BatchEpisodeParser.validateEpisodes(parsed)

        assertFalse(validation.isValid)
        assertTrue(validation.duplicateEpisodeNumbers.contains(1))
    }

    @Test
    fun jsonRoundTrip_preservesAllFields() {
        val original = BatchEpisodeParser.generateSequentialBatch(
            templateUrl = "https://cdn.io/ep_{n}.mp4",
            startEp = 1,
            endEp = 5,
            seasonNum = 1,
            arcName = "Canon"
        )
        val jsonStr = BatchEpisodeParser.toJsonString(original)
        val restored = BatchEpisodeParser.parseJsonArray(jsonStr)

        assertEquals(5, restored.size)
        assertEquals(1, restored[0].episodeNumber)
        assertEquals("Canon", restored[0].arcName)
        assertEquals("https://cdn.io/ep_1.mp4", restored[0].streamUrl)
    }
}
