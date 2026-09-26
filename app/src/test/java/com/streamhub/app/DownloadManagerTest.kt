package com.streamhub.app

import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.DownloadedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadManagerTest {

    @Test
    fun downloadManager_setCustomDownloadPath_sanitizesContentUris() {
        DownloadManager.setCustomDownloadPath("content://com.android.externalstorage.documents/tree/primary%3AMovies")
        assertEquals("content:// URIs should be sanitized to empty string", "", DownloadManager.customDownloadPath.value)

        DownloadManager.setCustomDownloadPath("")
        assertEquals("Empty path should be preserved as default", "", DownloadManager.customDownloadPath.value)
    }

    @Test
    fun downloadManager_setCustomScreenshotPath_sanitizesContentUris() {
        DownloadManager.setCustomScreenshotPath("content://com.android.externalstorage.documents/tree/primary%3APictures")
        assertEquals("content:// URIs should be sanitized to empty string", "", DownloadManager.customScreenshotPath.value)

        DownloadManager.setCustomScreenshotPath("")
        assertEquals("Empty path should be preserved as default", "", DownloadManager.customScreenshotPath.value)
    }

    @Test
    fun downloadManager_pauseDownload_mutatesState() {
        val initialItem = DownloadedItem(
            mediaId = "media_42",
            mediaTitle = "Attack on Titan",
            posterUrl = "https://example.com/poster.jpg",
            episodeIndex = 3,
            episodeTitle = "Episode 4",
            localFilePath = "/path/to/aot_ep4.mp4",
            downloadId = 1001L,
            progressPercent = 45,
            isCompleted = false,
            streamUrl = "https://cdn.example.com/aot_ep4.mp4"
        )

        // Seed DownloadManager._downloads via the public API
        DownloadManager.addOrUpdateDownload(initialItem)

        // Pre-condition: verify the seed took effect
        val beforePause = DownloadManager.downloads.value
        assertEquals(1, beforePause.size)
        assertEquals(1001L, beforePause[0].downloadId)
        assertFalse(beforePause[0].isPaused)

        // Call the real method under test
        DownloadManager.pauseDownload(initialItem)

        // Assert on the REAL state change in DownloadManager.downloads
        val afterPause = DownloadManager.downloads.value
        assertEquals(1, afterPause.size)
        assertTrue("Item should be marked paused", afterPause[0].isPaused)
        assertEquals("Download ID should be reset to -1L", -1L, afterPause[0].downloadId)
        assertEquals("Progress should be preserved", 45, afterPause[0].progressPercent)
    }

    @Test
    fun downloadManager_hasActiveDownload_evaluatesCorrectly() {
        // Clear downloads list
        DownloadManager.deleteDownloadsForMedia("media_test")

        val queuedItem = DownloadedItem(
            mediaId = "media_test",
            mediaTitle = "One Piece",
            posterUrl = "https://example.com/op.jpg",
            episodeIndex = 1,
            episodeTitle = "Episode 2",
            localFilePath = "/path/to/op_ep2.mp4",
            isCompleted = false,
            isPaused = false,
            isQueued = true,
            streamUrl = "https://cdn.example.com/op_ep2.mp4"
        )
        DownloadManager.addOrUpdateDownload(queuedItem)

        // Queued item should NOT count as actively running
        assertFalse("Queued item should not count as active download", DownloadManager.hasActiveDownload())

        val activeItem = DownloadedItem(
            mediaId = "media_test",
            mediaTitle = "One Piece",
            posterUrl = "https://example.com/op.jpg",
            episodeIndex = 0,
            episodeTitle = "Episode 1",
            localFilePath = "/path/to/op_ep1.mp4",
            isCompleted = false,
            isPaused = false,
            isQueued = false,
            streamUrl = "https://cdn.example.com/op_ep1.mp4"
        )
        DownloadManager.addOrUpdateDownload(activeItem)

        // Active item present -> hasActiveDownload should be true
        assertTrue("Active item should count as active download", DownloadManager.hasActiveDownload())

        // Pause the active item -> should be false again
        DownloadManager.pauseDownload(activeItem)
        assertFalse("Paused item should not count as active download", DownloadManager.hasActiveDownload())

        // Cleanup
        DownloadManager.deleteDownloadsForMedia("media_test")
    }
}
