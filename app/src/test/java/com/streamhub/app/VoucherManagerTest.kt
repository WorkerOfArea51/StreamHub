package com.streamhub.app

import com.streamhub.app.data.VoucherManager
import com.streamhub.app.data.models.VipVoucher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoucherManagerTest {

    @Test
    fun generateSecureCode_matchesExpectedFormat() {
        val code = VoucherManager.generateSecureCode()
        // Format: SH-XXXX-YYYY-ZZZZ
        val regex = Regex("""^SH-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$""")
        assertTrue("Generated code $code should match format SH-XXXX-YYYY-ZZZZ", regex.matches(code))
    }

    @Test
    fun generateSecureCode_excludesConfusingCharacters() {
        // Exclude 0, O, 1, I to prevent user typos on mobile keyboards
        for (i in 0 until 100) {
            val code = VoucherManager.generateSecureCode()
            assertFalse("Code should not contain '0': $code", code.contains('0'))
            assertFalse("Code should not contain 'O': $code", code.contains('O'))
            assertFalse("Code should not contain '1': $code", code.contains('1'))
            assertFalse("Code should not contain 'I': $code", code.contains('I'))
        }
    }

    @Test
    fun generateSecureCode_generatesUniqueCodesWithZeroCollisions() {
        val generatedSet = mutableSetOf<String>()
        val totalCodes = 1000
        for (i in 0 until totalCodes) {
            val code = VoucherManager.generateSecureCode()
            assertTrue("Code collision detected: $code", generatedSet.add(code))
        }
        assertEquals(totalCodes, generatedSet.size)
    }

    @Test
    fun vipVoucher_isExpiredLogic_evaluatesCorrectly() {
        val now = System.currentTimeMillis()

        val activeVoucher = VipVoucher(
            code = "SH-TEST-1234-5678",
            status = VipVoucher.STATUS_ACTIVE,
            activatedAt = now,
            expiresAt = now + (30L * 24 * 60 * 60 * 1000L)
        )
        assertFalse(activeVoucher.isExpired)
        assertTrue(activeVoucher.remainingDays >= 29)

        val expiredVoucher = VipVoucher(
            code = "SH-EXPR-1234-5678",
            status = VipVoucher.STATUS_ACTIVE,
            activatedAt = now - (31L * 24 * 60 * 60 * 1000L),
            expiresAt = now - (1L * 24 * 60 * 60 * 1000L)
        )
        assertTrue(expiredVoucher.isExpired)
        assertEquals(0, expiredVoucher.remainingDays)

        val explicitlyExpiredVoucher = VipVoucher(
            code = "SH-MANL-1234-5678",
            status = VipVoucher.STATUS_EXPIRED,
            expiresAt = now + 100000L
        )
        assertTrue(explicitlyExpiredVoucher.isExpired)
    }

    @Test
    fun vipVoucher_remainingDays_coercesAtLeastOneWhenActive() {
        val now = System.currentTimeMillis()
        // 2 hours left on last day
        val nearlyExpiredVoucher = VipVoucher(
            code = "SH-LAST-1234-5678",
            status = VipVoucher.STATUS_ACTIVE,
            expiresAt = now + (2L * 60 * 60 * 1000L)
        )
        assertFalse(nearlyExpiredVoucher.isExpired)
        assertEquals(1, nearlyExpiredVoucher.remainingDays)
    }
}
