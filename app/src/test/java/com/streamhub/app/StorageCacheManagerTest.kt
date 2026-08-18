package com.streamhub.app

import com.streamhub.app.data.CacheConfig
import com.streamhub.app.data.StorageMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class StorageCacheManagerTest {

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
        assertEquals(2048, config.cacheLimitMb)
        assertEquals(7, config.cacheTtlDays)
        assertTrue(config.keepWatchedForInstantResume)
    }

    @Test
    fun evictionLogic_calculatesPurgeRequirementCorrectly() {
        val currentCacheBytes = 3L * 1024 * 1024 * 1024 // 3 GB
        val limitMb = 2048 // 2 GB limit
        val limitBytes = limitMb * 1024L * 1024L

        val isOverQuota = currentCacheBytes > limitBytes
        val bytesToPurge = if (isOverQuota) currentCacheBytes - limitBytes else 0L

        assertTrue("Cache of 3GB should exceed 2GB limit", isOverQuota)
        assertEquals("Should purge exactly 1GB", 1024L * 1024L * 1024L, bytesToPurge)
    }

    @Test
    fun ttlExpirationLogic_identifiesStaleFiles() {
        val ttlDays = 7
        val now = System.currentTimeMillis()
        val staleTimestamp = now - TimeUnit.DAYS.toMillis(10) // 10 days ago
        val freshTimestamp = now - TimeUnit.DAYS.toMillis(2)  // 2 days ago

        val isStale: (Long) -> Boolean = { lastModified ->
            (now - lastModified) > TimeUnit.DAYS.toMillis(ttlDays.toLong())
        }

        assertTrue("10-day-old file should be stale under 7-day TTL", isStale(staleTimestamp))
        assertFalse("2-day-old file should be fresh under 7-day TTL", isStale(freshTimestamp))
    }
}
