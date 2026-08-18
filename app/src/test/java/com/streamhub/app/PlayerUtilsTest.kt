package com.streamhub.app

import com.streamhub.app.ui.screens.player.formatTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerUtilsTest {

    @Test
    fun formatTime_underOneHour_mm_ss() {
        assertEquals("00:00", formatTime(0L))
        assertEquals("00:45", formatTime(45_000L))
        assertEquals("05:30", formatTime(330_000L))
        assertEquals("59:59", formatTime(3599_000L))
    }

    @Test
    fun formatTime_overOneHour_h_mm_ss() {
        assertEquals("1:00:00", formatTime(3600_000L))
        assertEquals("2:15:30", formatTime(8130_000L))
        assertEquals("10:00:05", formatTime(36005_000L))
    }

    @Test
    fun formatTime_negativeMilliseconds_coercedToZero() {
        assertEquals("00:00", formatTime(-5000L))
    }
}
