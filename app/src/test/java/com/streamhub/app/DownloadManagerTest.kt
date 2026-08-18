package com.streamhub.app

import com.streamhub.app.data.DownloadedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadManagerTest {

    @Test
    fun downloadedItem_creation_hasExpectedDefaults() {
        val item = DownloadedItem(
            mediaId = "media_123",
            mediaTitle = "Cyberpunk: Edgerunners",
            posterUrl = "https://image.tmdb.org/poster.jpg",
            episodeIndex = 0,
            episodeTitle = "Episode 1",
            localFilePath = "/data/user/0/com.streamhub.app/files/Movies/StreamHub/ep1.mp4",
            streamUrl = "https://example.com/stream.m3u8"
        )

        assertEquals("media_123", item.mediaId)
        assertEquals("Cyberpunk: Edgerunners", item.mediaTitle)
        assertEquals(0, item.episodeIndex)
        assertEquals(0, item.progressPercent)
        assertFalse(item.isCompleted)
        assertFalse(item.isPaused)
        assertFalse(item.isCanceled)
    }

    @Test
    fun downloadedItem_completion_updatesStatus() {
        val original = DownloadedItem(
            mediaId = "m1",
            mediaTitle = "Demon Slayer",
            posterUrl = "",
            episodeIndex = 0,
            episodeTitle = "Ep 1",
            localFilePath = "/path/to/file.mp4"
        )

        val completed = original.copy(
            progressPercent = 100,
            isCompleted = true,
            fileSizeMb = 350.5
        )

        assertTrue(completed.isCompleted)
        assertEquals(100, completed.progressPercent)
        assertEquals(350.5, completed.fileSizeMb, 0.01)
    }

    @Test
    fun filenameSanitization_removesIllegalFilesystemCharacters() {
        val regex = Regex("[^a-zA-Z0-9]")
        val rawTitle = "Minions & Monsters: The Movie (2026) / 4K [HDR]!"
        val sanitized = rawTitle.replace(regex, "_")

        assertFalse("Should not contain colons", sanitized.contains(":"))
        assertFalse("Should not contain slashes", sanitized.contains("/"))
        assertFalse("Should not contain ampersands", sanitized.contains("&"))
        assertFalse("Should not contain brackets", sanitized.contains("["))
        assertTrue("Should contain alphanumeric underscores", sanitized.contains("Minions___Monsters__The_Movie__2026____4K__HDR__"))
    }
}
