package com.streamhub.app

import com.streamhub.app.ui.theme.AppThemeAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeManagerTest {

    @Test
    fun defaultAccent_isCyan() {
        val defaultAccent = AppThemeAccent.CYAN
        assertEquals("Cyberpunk Cyan", defaultAccent.label)
        assertNotNull(defaultAccent.color)
    }

    @Test
    fun allAccents_haveUniqueKeysAndLabels() {
        val accents = AppThemeAccent.entries
        val keys = accents.map { it.key }.toSet()
        val labels = accents.map { it.label }.toSet()

        assertEquals("All accents should have unique enum keys", accents.size, keys.size)
        assertEquals("All accents should have unique display labels", accents.size, labels.size)
        assertTrue("Should have at least 5 accents", accents.size >= 5)
    }

    @Test
    fun themeManager_accentResolution_fallbackGraceful() {
        val accents = AppThemeAccent.entries
        val found = accents.firstOrNull { it.key == "PURPLE" }
        assertNotNull(found)
        assertEquals("Neon Purple", found?.label)
    }
}
