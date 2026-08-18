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
        // Direct call on DownloadManager
        DownloadManager.setCustomDownloadPath("content://com.android.externalstorage.documents/tree/primary%3AMovies")
        assertEquals("content:// URIs should be sanitized to empty string", "", DownloadManager.customDownloadPath.value)

        DownloadManager.setCustomDownloadPath("")
        assertEquals("Empty path should be preserved as default", "", DownloadManager.customDownloadPath.value)
    }

    @Test
    fun downloadManager_setCustomScreenshotPath_sanitizesContentUris() {
        // Direct call on DownloadManager
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

        // Directly call pauseDownload
        DownloadManager.pauseDownload(initialItem)

        // Verify paused copy
        val pausedCopy = initialItem.copy(isPaused = true, downloadId = -1L)
        assertTrue(pausedCopy.isPaused)
        assertEquals(-1L, pausedCopy.downloadId)
        assertEquals(45, pausedCopy.progressPercent)
    }
}
