package com.streamhub.app

import com.streamhub.app.data.importer.BulkCatalogImporter
import com.streamhub.app.data.importer.ConflictStrategy
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkCatalogImporterTest {

    @Test
    fun parseInputUrls_splitsAndSanitizesLinesAndCommas() {
        val input = """
            https://example.com/anime1.json, https://example.com/anime2.json
            # Comment line to ignore
            https://example.com/anime3.json
            
            eb76ab230b4d45ef32474cc414585f2471dc849fcf1c669d
        """.trimIndent()

        val urls = BulkCatalogImporter.parseInputUrls(input)
        assertEquals(4, urls.size)
        assertEquals("https://example.com/anime1.json", urls[0])
        assertEquals("https://example.com/anime2.json", urls[1])
        assertEquals("https://example.com/anime3.json", urls[2])
        assertEquals("eb76ab230b4d45ef32474cc414585f2471dc849fcf1c669d", urls[3])
    }

    @Test
    fun applyImport_mergeEpisodes_appendsNewEpisodesWithoutDuplicates() {
        val existingShow = MediaItem(
            id = "solo_leveling",
            title = "Solo Leveling",
            category = "Anime",
            episodes = listOf(
                Episode(episodeNumber = 1, streamUrl = "https://cdn.com/ep1.mp4"),
                Episode(episodeNumber = 2, streamUrl = "https://cdn.com/ep2.mp4")
            )
        )

        val incomingShow = MediaItem(
            id = "solo_leveling",
            title = "Solo Leveling",
            category = "Anime",
            episodes = listOf(
                Episode(episodeNumber = 2, streamUrl = "https://cdn.com/ep2_alt.mp4"), // Duplicate ep 2
                Episode(episodeNumber = 3, streamUrl = "https://cdn.com/ep3.mp4"),
                Episode(episodeNumber = 4, streamUrl = "https://cdn.com/ep4.mp4")
            )
        )

        val (updatedCatalog, summary) = BulkCatalogImporter.applyImport(
            itemsToImport = listOf(incomingShow),
            existingCatalog = listOf(existingShow),
            strategy = ConflictStrategy.MERGE_EPISODES
        )

        assertEquals(1, updatedCatalog.size)
        val mergedShow = updatedCatalog.first()
        assertEquals(4, mergedShow.episodes.size)
        assertEquals(1, mergedShow.episodes[0].episodeNumber)
        assertEquals(2, mergedShow.episodes[1].episodeNumber)
        assertEquals(3, mergedShow.episodes[2].episodeNumber)
        assertEquals(4, mergedShow.episodes[3].episodeNumber)
        assertEquals(2, summary.totalEpisodesAdded) // Only ep 3 and 4 added
        assertEquals(1, summary.mergedCount)
    }

    @Test
    fun applyImport_skipDuplicates_ignoresExistingShows() {
        val existingShow = MediaItem(id = "aot", title = "Attack on Titan", episodes = listOf(Episode(1)))
        val incomingShow = MediaItem(id = "aot", title = "Attack on Titan", episodes = listOf(Episode(1), Episode(2)))

        val (updatedCatalog, summary) = BulkCatalogImporter.applyImport(
            itemsToImport = listOf(incomingShow),
            existingCatalog = listOf(existingShow),
            strategy = ConflictStrategy.SKIP_DUPLICATES
        )

        assertEquals(1, updatedCatalog.size)
        assertEquals(1, updatedCatalog.first().episodes.size)
        assertEquals(1, summary.skippedCount)
        assertEquals(0, summary.importedCount)
    }

    @Test
    fun applyImport_overwriteExisting_replacesShow() {
        val existingShow = MediaItem(id = "jujutsu", title = "Jujutsu Kaisen", rating = "7.0")
        val incomingShow = MediaItem(id = "jujutsu", title = "Jujutsu Kaisen", rating = "9.0", episodes = listOf(Episode(1)))

        val (updatedCatalog, summary) = BulkCatalogImporter.applyImport(
            itemsToImport = listOf(incomingShow),
            existingCatalog = listOf(existingShow),
            strategy = ConflictStrategy.OVERWRITE_EXISTING
        )

        assertEquals(1, updatedCatalog.size)
        assertEquals("9.0", updatedCatalog.first().rating)
        assertEquals(1, summary.importedCount)
    }
}
