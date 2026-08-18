package com.streamhub.app

import com.streamhub.app.data.CacheConfig
import com.streamhub.app.data.StorageMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun byteToMegaByteConversion_calculation() {
        val bytes = 104857600L // 100 MB
        val mb = bytes / (1024.0 * 1024.0)
        assertEquals(100.0, mb, 0.001)
    }
}
