package com.streamhub.app

import com.streamhub.app.data.CacheConfig
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.data.StorageMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageCacheManagerTest {

    @Test
    fun storageCacheManager_formatBytes_formatsCorrectly() {
        // Direct call on StorageCacheManager.formatBytes
        assertEquals("0 B", StorageCacheManager.formatBytes(0L))
        assertEquals("0 B", StorageCacheManager.formatBytes(-50L))
        assertEquals("500.00 B", StorageCacheManager.formatBytes(500L))
        assertEquals("1.00 KB", StorageCacheManager.formatBytes(1024L))
        assertEquals("1.50 MB", StorageCacheManager.formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.00 GB", StorageCacheManager.formatBytes(2L * 1024 * 1024 * 1024))
        assertEquals("1.00 TB", StorageCacheManager.formatBytes(1024L * 1024 * 1024 * 1024))
    }

    @Test
    fun storageMetrics_defaults_areZero() {
        val metrics = StorageMetrics()
        assertEquals(0L, metrics.videoCacheBytes)
        assertEquals(0L, metrics.imageCacheBytes)
        assertEquals(0L, metrics.downloadsBytes)
        assertEquals(0L, metrics.totalAppBytes)
        assertFalse(metrics.isCalculating)
    }

    @Test
    fun cacheConfig_defaults_areStandard() {
        val config = CacheConfig()
        assertEquals(-1, config.cacheLimitMb)
        assertEquals(-1, config.cacheTtlHours)
        assertTrue(config.keepWatchedForInstantResume)
    }
}
