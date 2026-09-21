package com.streamhub.app

import com.streamhub.app.data.importer.CatalogBundleManager
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.data.models.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogBundleManagerTest {

    private fun createDummyShow(
        id: String,
        title: String,
        category: String,
        episodeCount: Int
    ): MediaItem {
        val episodes = (1..episodeCount).map { i ->
            Episode(
                episodeNumber = i,
                seasonNumber = 1,
                title = "Episode $i",
                streamUrl = "https://worker.serv00.net/stream/file_$id" + "_$i.mkv",
                mirrorStreamUrl = "https://mirror.serv00.net/stream/file_$id" + "_$i.mkv",
                durationMs = 1440000L
            )
        }
        return MediaItem(
            id = id,
            title = title,
            category = category,
            type = if (category == "MOVIE") "MOVIE" else "SERIES",
            description = "A very descriptive synopsis for testing byte estimation and packing logic in StreamHub Smart Bundler.",
            rating = "8.5",
            releaseYear = "2024",
            genres = listOf("Action", "Drama"),
            episodes = episodes,
            mediaInfo = MediaInfo(
                resolution = "1080p FHD",
                videoCodec = "HEVC / x265",
                audioTracks = listOf("Japanese", "English"),
                subtitleTracks = listOf("English")
            )
        )
    }

    @Test
    fun testPackCategorySingleBundle() {
        val shows = (1..10).map { i ->
            createDummyShow("show_$i", "Show Title $i", "ANIME", 12)
        }

        val bundles = CatalogBundleManager.packCategory("Anime", "animes_bundle_part_", shows)

        assertEquals(1, bundles.size)
        val bundle = bundles.first()
        assertEquals("animes_bundle_part_1", bundle.bundleId)
        assertEquals(1, bundle.partIndex)
        assertEquals(1, bundle.totalParts)
        assertEquals(10, bundle.showsCount)
        assertEquals(120, bundle.episodesCount)
        assertTrue("Bundle size must be <= 970 KB", bundle.sizeBytes <= CatalogBundleManager.BUNDLE_MAX_BYTES)
    }

    @Test
    fun testPackEntireCatalogSeparatesCategories() {
        val anime = createDummyShow("anime_1", "Anime One", "ANIME", 24)
        val movie = createDummyShow("movie_1", "Movie One", "MOVIE", 1)
        val series = createDummyShow("series_1", "Series One", "SERIES", 8)

        val catalog = listOf(anime, movie, series)
        val bundles = CatalogBundleManager.packEntireCatalog(catalog)

        assertEquals(3, bundles.size)
        val movieBundle = bundles.find { it.category == "Movies" }
        val seriesBundle = bundles.find { it.category == "Series" }
        val animeBundle = bundles.find { it.category == "Anime" }

        assertNotNull(movieBundle)
        assertNotNull(seriesBundle)
        assertNotNull(animeBundle)

        assertEquals("movies_bundle_part_1", movieBundle?.bundleId)
        assertEquals("series_bundle_part_1", seriesBundle?.bundleId)
        assertEquals("animes_bundle_part_1", animeBundle?.bundleId)
    }

    @Test
    fun testShowsAreNeverTruncatedOrSplitAcrossBundles() {
        // Generate enough shows to force multiple 970 KB bundles
        // Each show with 100 episodes is ~35-40 KB in JSON
        val largeCatalog = (1..60).map { i ->
            createDummyShow("large_show_$i", "Massive Series $i", "ANIME", 100)
        }

        val bundles = CatalogBundleManager.packCategory("Anime", "animes_bundle_part_", largeCatalog)

        assertTrue("Should have multiple parts when catalog is large", bundles.size > 1)

        // Verify each bundle strictly adheres to 970 KB
        bundles.forEach { bundle ->
            assertTrue(
                "Bundle ${bundle.bundleId} exceeded 970 KB: ${bundle.sizeBytes} bytes",
                bundle.sizeBytes <= CatalogBundleManager.BUNDLE_MAX_BYTES
            )
            assertEquals("totalParts should be updated across all parts", bundles.size, bundle.totalParts)
        }

        // Verify all 60 shows and all 6,000 episodes are preserved with zero loss or splitting
        val flattenedShows = bundles.flatMap { it.items }
        assertEquals(60, flattenedShows.size)
        assertEquals(6000, flattenedShows.sumOf { it.episodes.size })

        // Check each show has all 100 episodes intact
        flattenedShows.forEach { show ->
            assertEquals(100, show.episodes.size)
        }
    }
}
