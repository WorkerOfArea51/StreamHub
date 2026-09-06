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
        val brokenItem = MediaItem(
            id = "test_broken",
            title = "Test Broken Title",
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

        val issues = getMediaItemIssues(brokenItem)

        assertTrue(MetadataIssueType.GENRE in issues)
        assertTrue(MetadataIssueType.TRAILER in issues)
        assertTrue(MetadataIssueType.SYNOPSIS in issues)
        assertTrue(MetadataIssueType.POSTER in issues)
        assertTrue(MetadataIssueType.BACKDROP in issues)
        assertTrue(MetadataIssueType.RATING in issues)
        assertTrue(MetadataIssueType.CAST in issues)
        assertTrue(MetadataIssueType.STUDIO in issues)
        assertTrue(MetadataIssueType.YEAR in issues)
        assertTrue(MetadataIssueType.DURATION in issues)
        assertTrue(MetadataIssueType.EPISODES in issues)
        assertEquals(11, issues.size)
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
            castList = listOf("Actor 1"),
            studio = "Studio A",
            releaseYear = "2023",
            duration = "120m"
        )

        val issues = getMediaItemIssues(duplicateBackdropItem)
        assertTrue("Duplicate banner should be flagged as BACKDROP issue", MetadataIssueType.BACKDROP in issues)
    }
}
