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

    @Test
    fun parseRawDump_f2lBatchJsonObject_extractsDurationsAndExactTitles() {
        val f2lJson = """
            {"batch_id":"9af0276c674e15892cfdb7627086d8e5","category":"anime","channel_id":-1002633457020,"episodes":[{"code":"0d07b93b37770e5c2f3ea796cb43268dba85886553895acc","download_url":"https://streamhub69.alwaysdata.net/dl/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc","duration":1421.0,"duration_formatted":"23:41","episode_num":1,"file_name":"EP - 01 - Undertaker.mkv","file_size":469136808,"mime_type":"video/x-matroska","size_formatted":"447.4 MB","stream_url":"https://streamhub69.alwaysdata.net/stream/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc"},{"code":"1737eabf2d800430739c2d171cbeee18fe47f19fdc149412","download_url":"https://streamhub69.alwaysdata.net/dl/1737eabf2d800430739c2d171cbeee18fe47f19fdc149412","duration":1429.0,"duration_formatted":"23:49","episode_num":2,"file_name":"EP - 02 - Spearhead.mkv","file_size":493015093,"mime_type":"video/x-matroska","size_formatted":"470.18 MB","stream_url":"https://streamhub69.alwaysdata.net/stream/1737eabf2d800430739c2d171cbeee18fe47f19fdc149412"}],"status":"success","title":"86 Eighty-Six","total_episodes":2}
        """.trimIndent()

        val parsed = BatchEpisodeParser.parseRawDump(f2lJson, defaultSeason = 1)
        assertEquals(2, parsed.size)
        assertEquals(1, parsed[0].episodeNumber)
        assertEquals("EP - 01 - Undertaker", parsed[0].title)
        assertEquals(1421000L, parsed[0].durationMs)
        assertEquals("447.4 MB", parsed[0].fileSize)
        assertEquals("https://streamhub69.alwaysdata.net/dl/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc", parsed[0].streamUrl)

        assertEquals(2, parsed[1].episodeNumber)
        assertEquals("EP - 02 - Spearhead", parsed[1].title)
        assertEquals(1429000L, parsed[1].durationMs)
        assertEquals("470.18 MB", parsed[1].fileSize)
        assertEquals("https://streamhub69.alwaysdata.net/dl/1737eabf2d800430739c2d171cbeee18fe47f19fdc149412", parsed[1].streamUrl)
    }

    @Test
    fun parseRawDump_longEpisodeDuration_formatsWithHours() {
        val squidGameDump = """
            {"batch_id":"squid_game","category":"series","episodes":[{"episode_num":1,"title":"Red Light, Green Light","duration_ms":3644000,"direct_stream_url":"https://streamhub69.alwaysdata.net/dl/123"}]}
        """.trimIndent()

        val parsed = BatchEpisodeParser.parseRawDump(squidGameDump)
        assertEquals(1, parsed.size)
        assertEquals(3644000L, parsed[0].durationMs)

        val jsonStr = BatchEpisodeParser.toJsonString(parsed)
        assertTrue(jsonStr.contains("\"duration_formatted\": \"1:00:44\""))
    }
}
