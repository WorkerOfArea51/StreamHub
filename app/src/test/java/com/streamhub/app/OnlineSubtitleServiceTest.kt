package com.streamhub.app

import com.streamhub.app.data.api.OnlineSubtitleService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineSubtitleServiceTest {

    @Test
    fun testSanitizeTitle() {
        val raw1 = "Solo.Leveling.S01E03.1080p.CR.WEB-DL.mkv"
        val clean1 = OnlineSubtitleService.sanitizeTitle(raw1)
        assertEquals("Solo Leveling", clean1)

        val raw2 = "[Judas] Jujutsu Kaisen - 01 [1080p][HEVC].mp4"
        val clean2 = OnlineSubtitleService.sanitizeTitle(raw2)
        assertEquals("Jujutsu Kaisen", clean2)

        val raw3 = "Dune.Part.Two.2024.2160p.UHD.HDR.x265"
        val clean3 = OnlineSubtitleService.sanitizeTitle(raw3)
        assertTrue(clean3.contains("Dune Part Two"))
    }

    @Test
    fun testLanguageDisplayName() {
        assertEquals("English 🇬🇧", OnlineSubtitleService.getLanguageDisplayName("eng"))
        assertEquals("Spanish 🇪🇸", OnlineSubtitleService.getLanguageDisplayName("spa"))
        assertEquals("Arabic 🇸🇦", OnlineSubtitleService.getLanguageDisplayName("ara"))
        assertEquals("Bengali 🇧🇩", OnlineSubtitleService.getLanguageDisplayName("ben"))
        assertEquals("Japanese 🇯🇵", OnlineSubtitleService.getLanguageDisplayName("jpn"))
        assertEquals("Hindi 🇮🇳", OnlineSubtitleService.getLanguageDisplayName("hin"))
    }

    @Test
    fun testSupportedLanguagesContainsCommonCodes() {
        val codes = OnlineSubtitleService.supportedLanguages.map { it.code }
        assertTrue(codes.contains("all"))
        assertTrue(codes.contains("eng"))
        assertTrue(codes.contains("spa"))
        assertTrue(codes.contains("ara"))
        assertTrue(codes.contains("hin"))
        assertTrue(codes.contains("ben"))
    }
}
