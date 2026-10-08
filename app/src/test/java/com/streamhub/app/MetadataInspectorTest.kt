package com.streamhub.app

import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.components.MetadataIssueType
import com.streamhub.app.ui.components.getMediaItemIssues
import com.streamhub.app.ui.components.isGenreBroken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataInspectorTest {

    @Test
    fun isGenreBroken_detectsEmptyAndGenericGenres() {
        assertTrue(isGenreBroken(emptyList()))
        assertTrue(isGenreBroken(listOf("movie")))
        assertTrue(isGenreBroken(listOf("Movies")))
        assertTrue(isGenreBroken(listOf("series")))
        assertTrue(isGenreBroken(listOf("tv series")))
        assertTrue(isGenreBroken(listOf("anime")))
        assertTrue(isGenreBroken(listOf("Movie", "Series")))

        // Real genres must NOT be marked broken
        assertFalse(isGenreBroken(listOf("Action", "Thriller")))
        assertFalse(isGenreBroken(listOf("Sci-Fi", "Adventure")))
        assertFalse(isGenreBroken(listOf("Action", "Movie"))) // Contains Action, not purely generic
    }

    @Test
    fun getMediaItemIssues_detectsAllMissingSpecs() {
        val brokenSeries = MediaItem(
            id = "test_broken_series",
            title = "Test Broken Series",
            type = "SERIES",
            category = "Series",
            genres = listOf("Series"), // Broken genre
            trailerId = "",            // Missing trailer
            description = "",          // Missing description
            posterUrl = "",            // Missing poster
            bannerUrl = "",            // Missing backdrop
            rating = "",               // Missing rating
            castList = emptyList(),    // Missing cast
            studio = "",               // Missing studio
            producers = "",            // Missing producers
            releaseYear = "",          // Missing release year
            duration = "",             // Missing duration
            totalEpisodes = ""         // Missing episode count
        )

        val seriesIssues = getMediaItemIssues(brokenSeries)

        assertTrue(MetadataIssueType.GENRE in seriesIssues)
        assertTrue(MetadataIssueType.TRAILER in seriesIssues)
        assertTrue(MetadataIssueType.SYNOPSIS in seriesIssues)
        assertTrue(MetadataIssueType.POSTER in seriesIssues)
        assertTrue(MetadataIssueType.BACKDROP in seriesIssues)
        assertTrue(MetadataIssueType.RATING in seriesIssues)
        assertTrue(MetadataIssueType.CAST in seriesIssues)
        assertTrue(MetadataIssueType.STUDIO in seriesIssues)
        assertTrue(MetadataIssueType.MATURITY in seriesIssues)
        assertTrue(MetadataIssueType.YEAR in seriesIssues)
        assertTrue(MetadataIssueType.EPISODES in seriesIssues)
        assertEquals(11, seriesIssues.size)

        val brokenMovie = MediaItem(
            id = "test_broken_movie",
            title = "Test Broken Movie",
            type = "MOVIE",
            category = "Movie",
            genres = listOf("Movie"),
            trailerId = "",
            description = "",
            posterUrl = "",
            bannerUrl = "",
            rating = "",
            castList = emptyList(),
            studio = "",
            releaseYear = "",
            duration = ""
        )
        val movieIssues = getMediaItemIssues(brokenMovie)
        assertTrue(MetadataIssueType.DURATION in movieIssues)
    }

    @Test
    fun getMediaItemIssues_detectsAnimeSpecificProducersAndSource() {
        val animeWithoutSpecs = MediaItem(
            id = "anime_test",
            title = "Demon Slayer",
            type = "SERIES",
            category = "Anime",
            genres = listOf("Action", "Fantasy"),
            trailerId = "abc",
            description = "Tanjirou fights demons.",
            posterUrl = "https://example.com/poster.jpg",
            bannerUrl = "https://example.com/banner.jpg",
            rating = "8.8",
            castList = listOf("Natsuki Hanae"),
            studio = "ufotable",
            producers = "", // Missing producers
            source = "",    // Missing source
            maturityRating = "TV-14",
            releaseYear = "2019",
            duration = "24 min. per ep.",
            totalEpisodes = "26"
        )

        val issues = getMediaItemIssues(animeWithoutSpecs)
        assertTrue(MetadataIssueType.PRODUCERS in issues)
        assertTrue(MetadataIssueType.SOURCE in issues)
    }

    @Test
    fun getMediaItemIssues_returnsEmptyForPristineItem() {
        val pristineItem = MediaItem(
            id = "test_pristine",
            title = "War",
            type = "MOVIE",
            category = "Movie",
            genres = listOf("Action", "Thriller", "Adventure"),
            trailerId = "tQ0mzXRk-oM",
            description = "An Indian soldier is assigned to eliminate his former mentor.",
            posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
            rating = "6.8",
            maturityRating = "PG-13",
            castList = listOf("Hrithik Roshan", "Tiger Shroff", "Vaani Kapoor"),
            studio = "Yash Raj Films",
            producers = "Aditya Chopra",
            releaseYear = "2019",
            aired = "2019-10-02",
            duration = "152m",
            tmdbId = "585268"
        )

        val issues = getMediaItemIssues(pristineItem)
        assertTrue("Pristine item should have zero issues, but got: $issues", issues.isEmpty())
    }

    @Test
    fun getMediaItemIssues_flagsDuplicateBackdropAsPoster() {
        val duplicateBackdropItem = MediaItem(
            id = "test_dup",
            title = "Sample",
            type = "MOVIE",
            genres = listOf("Action"),
            trailerId = "abc",
            description = "Good movie",
            posterUrl = "https://example.com/poster.jpg",
            bannerUrl = "https://example.com/poster.jpg", // Same as poster!
            rating = "8.0",
            maturityRating = "PG-13",
            castList = listOf("Actor 1"),
            studio = "Studio A",
            releaseYear = "2023",
            duration = "120m"
        )

        val issues = getMediaItemIssues(duplicateBackdropItem)
        assertTrue("Duplicate banner should be flagged as BACKDROP issue", MetadataIssueType.BACKDROP in issues)
    }

    @Test
    fun getMediaItemIssues_detectsUnstandardizedSpecs() {
        val itemWithDirtyCodec = MediaItem(
            id = "test_dirty_codec",
            title = "Sample Codec",
            type = "MOVIE",
            mediaInfo = com.streamhub.app.data.models.MediaInfo(
                resolution = "1080",
                videoCodec = "x265 10-bit"
            )
        )
        val issues = getMediaItemIssues(itemWithDirtyCodec)
        assertTrue("Dirty codec should be flagged as UNSTANDARDIZED_SPECS", MetadataIssueType.UNSTANDARDIZED_SPECS in issues)
    }
}

