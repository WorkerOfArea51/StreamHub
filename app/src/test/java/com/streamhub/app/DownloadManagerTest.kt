package com.streamhub.app

import com.streamhub.app.data.DownloadedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadManagerTest {

    @Test
    fun downloadList_stateTransitions_pauseAndResume() {
        val initialItem = DownloadedItem(
            mediaId = "media_42",
            mediaTitle = "Attack on Titan",
            posterUrl = "https://example.com/poster.jpg",
            episodeIndex = 3,
            episodeTitle = "Episode 4",
            localFilePath = "/data/user/0/com.streamhub.app/files/Movies/StreamHub/aot_ep4.mp4",
            fileSizeMb = 0.0,
            downloadId = 1001L,
            progressPercent = 45,
            isCompleted = false,
            streamUrl = "https://cdn.example.com/aot_ep4.mp4"
        )

        // Simulate pause state mutation (honest pause)
        val pausedItem = initialItem.copy(
            isPaused = true,
            downloadId = -1L
        )

        assertTrue("Item should be marked paused", pausedItem.isPaused)
        assertEquals("Download ID should be reset upon pause", -1L, pausedItem.downloadId)
        assertEquals("Progress percent should be preserved during pause", 45, pausedItem.progressPercent)
        assertEquals("Stream URL should remain intact for resume", "https://cdn.example.com/aot_ep4.mp4", pausedItem.streamUrl)

        // Simulate resume state mutation
        val resumedItem = pausedItem.copy(
            isPaused = false,
            downloadId = 1002L
        )

        assertFalse("Item should no longer be paused", resumedItem.isPaused)
        assertEquals("Item should receive new download ID", 1002L, resumedItem.downloadId)
    }

    @Test
    fun pathValidation_detectsSuspiciousSystemPaths() {
        val suspiciousPaths = listOf("/data/system/secret", "/system/bin/sh", "/proc/cpuinfo", "/dev/null")
        val safePaths = listOf("/storage/emulated/0/Download", "/storage/emulated/0/Movies/Custom")

        val isSuspicious: (String) -> Boolean = { path ->
            val prefixes = listOf("/data/", "/system/", "/proc/", "/dev/")
            prefixes.any { path.startsWith(it) }
        }

        for (suspicious in suspiciousPaths) {
            assertTrue("Should detect suspicious path: $suspicious", isSuspicious(suspicious))
        }

        for (safe in safePaths) {
            assertFalse("Should allow safe external path: $safe", isSuspicious(safe))
        }
    }

    @Test
    fun pathSanitization_stripsContentUrisToEmpty() {
        val sanitizePath: (String) -> String = { path ->
            if (path.startsWith("content://")) "" else path
        }

        assertEquals("", sanitizePath("content://com.android.externalstorage.documents/tree/primary%3AMovies"))
        assertEquals("/storage/emulated/0/Movies", sanitizePath("/storage/emulated/0/Movies"))
        assertEquals("", sanitizePath(""))
    }

    @Test
    fun filenameSanitization_handlesComplexPunctuation() {
        val regex = Regex("[^a-zA-Z0-9]")
        val dirtyTitle = "Frieren: Beyond Journey's End (Season 2) [1080p] #Ep1!"
        val sanitized = dirtyTitle.replace(regex, "_")

        assertFalse(sanitized.contains(":"))
        assertFalse(sanitized.contains("'"))
        assertFalse(sanitized.contains("["))
        assertFalse(sanitized.contains("]"))
        assertFalse(sanitized.contains("#"))
        assertFalse(sanitized.contains("!"))
        assertTrue(sanitized.startsWith("Frieren__Beyond_Journey_s_End__Season_2___1080p___Ep1_"))
    }
}
