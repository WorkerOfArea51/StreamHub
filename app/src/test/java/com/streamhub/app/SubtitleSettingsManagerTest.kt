package com.streamhub.app

import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import com.streamhub.app.data.SubtitleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class SubtitleSettingsManagerTest {

    @Test
    fun defaultSubtitleConfig_hasStandardValues() {
        val config = SubtitleConfig()
        assertEquals(18f, config.fontSizeSp, 0.01f)
        assertEquals(0xFFFFE066L, config.textColorArgb)
        assertEquals(0xAA000000L, config.backgroundColorArgb)
        assertEquals(CaptionStyleCompat.EDGE_TYPE_OUTLINE, config.edgeType)
    }

    @Test
    fun subtitleConfig_copyMaintainsImmutability() {
        val original = SubtitleConfig()
        val custom = original.copy(fontSizeSp = 24f, textColorArgb = 0xFFFFFFFFL)

        assertEquals(18f, original.fontSizeSp, 0.01f)
        assertEquals(24f, custom.fontSizeSp, 0.01f)
        assertEquals(0xFFFFFFFFL, custom.textColorArgb)
    }

    @Test
    fun subtitleColorHexValues_evaluateAsUnsignedLongs() {
        val yellow = 0xFFFFE066L
        val white = 0xFFFFFFFFL
        val cyan = 0xFF22D3EEL
        val green = 0xFF4ADE80L

        assertTrue("White should evaluate to positive Long", white > 0L)
        assertTrue("Yellow should evaluate to positive Long", yellow > 0L)
        assertTrue("Cyan should evaluate to positive Long", cyan > 0L)
        assertTrue("Green should evaluate to positive Long", green > 0L)
    }
}
