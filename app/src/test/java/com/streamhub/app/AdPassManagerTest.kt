package com.streamhub.app

import com.streamhub.app.data.ads.AdPassManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPassManagerTest {

    @Test
    fun testPassDuration_isTwelveHours() {
        assertEquals(12 * 60 * 60 * 1000L, AdPassManager.PASS_DURATION_MS)
    }

    @Test
    fun testFormatRemainingTime_hoursAndMinutes() {
        val threeHoursTwentyMins = (3 * 3600 + 20 * 60) * 1000L
        val formatted = AdPassManager.formatRemainingTime(threeHoursTwentyMins)
        assertEquals("3h 20m", formatted)
    }

    @Test
    fun testFormatRemainingTime_lessThanHour() {
        val fortyFiveMins = 45 * 60 * 1000L
        val formatted = AdPassManager.formatRemainingTime(fortyFiveMins)
        assertEquals("45m 0s", formatted)
    }

    @Test
    fun testFormatRemainingTime_expiredOrZero() {
        assertEquals("Expired", AdPassManager.formatRemainingTime(0L))
        assertEquals("Expired", AdPassManager.formatRemainingTime(-1000L))
    }

    @Test
    fun testHasActivePass_whenNoExpirySet() {
        // Without shared prefs initialized, default pass state is false
        assertFalse(AdPassManager.hasActivePass())
        assertEquals(0L, AdPassManager.getRemainingTimeMillis())
    }
}
