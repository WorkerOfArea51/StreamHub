package com.streamhub.app

import com.streamhub.app.data.StreamBackendConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamBackendConfigTest {

    @Test
    fun migrateUrl_legacyAlwaysdataStream_migratesToServ00Dl() {
        val legacyStream = "https://streamhub69.alwaysdata.net/stream/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc"
        val expected = "https://midnighthawk.serv00.net/dl/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc"
        assertEquals(expected, StreamBackendConfig.migrateUrl(legacyStream))
    }

    @Test
    fun migrateUrl_legacyAlwaysdataDl_migratesToServ00Dl() {
        val legacyDl = "https://streamhub69.alwaysdata.net/dl/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc"
        val expected = "https://midnighthawk.serv00.net/dl/0d07b93b37770e5c2f3ea796cb43268dba85886553895acc"
        assertEquals(expected, StreamBackendConfig.migrateUrl(legacyDl))
    }

    @Test
    fun migrateUrl_serv00Stream_rewritesToServ00Dl() {
        val serv00Stream = "https://midnighthawk.serv00.net/stream/test_hash_123"
        val expected = "https://midnighthawk.serv00.net/dl/test_hash_123"
        assertEquals(expected, StreamBackendConfig.migrateUrl(serv00Stream))
    }

    @Test
    fun migrateUrl_serv00Dl_retainsServ00Dl() {
        val serv00Dl = "https://midnighthawk.serv00.net/dl/test_hash_123"
        assertEquals(serv00Dl, StreamBackendConfig.migrateUrl(serv00Dl))
    }

    @Test
    fun migrateUrl_httpLegacy_upgradesToHttpsServ00Dl() {
        val httpLegacy = "http://streamhub69.alwaysdata.net/stream/xyz"
        val expected = "https://midnighthawk.serv00.net/dl/xyz"
        assertEquals(expected, StreamBackendConfig.migrateUrl(httpLegacy))
    }

    @Test
    fun migrateUrl_externalCdn_remainsUntouched() {
        val external = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        assertEquals(external, StreamBackendConfig.migrateUrl(external))
    }

    @Test
    fun migrateUrl_blankOrEmpty_returnsOriginal() {
        assertEquals("", StreamBackendConfig.migrateUrl(""))
        assertEquals("   ", StreamBackendConfig.migrateUrl("   "))
    }

    @Test
    fun isBackendHost_matchesBothServ00AndLegacyAlwaysdata() {
        assertTrue(StreamBackendConfig.isBackendHost("https://midnighthawk.serv00.net/dl/123"))
        assertTrue(StreamBackendConfig.isBackendHost("https://streamhub69.alwaysdata.net/stream/123"))
        assertFalse(StreamBackendConfig.isBackendHost("https://youtube.com/watch?v=123"))
        assertFalse(StreamBackendConfig.isBackendHost("https://example.com/video.mp4"))
    }

    @Test
    fun isLegacyBackendHost_identifiesOnlyLegacyAlwaysdata() {
        assertTrue(StreamBackendConfig.isLegacyBackendHost("https://streamhub69.alwaysdata.net/dl/123"))
        assertTrue(StreamBackendConfig.isLegacyBackendHost("streamhub69.alwaysdata.net"))
        assertFalse(StreamBackendConfig.isLegacyBackendHost("https://midnighthawk.serv00.net/dl/123"))
        assertFalse(StreamBackendConfig.isLegacyBackendHost("midnighthawk.serv00.net"))
    }
}
