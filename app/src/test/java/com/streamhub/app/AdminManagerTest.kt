package com.streamhub.app

import com.streamhub.app.data.AdminManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminManagerTest {

    @Test
    fun adminPasswordVerification_validPasswords_returnTrue() {
        assertTrue(AdminManager.verifyPassword("StreamHubAdmin2026"))
        assertTrue(AdminManager.verifyPassword("7860"))
        assertTrue(AdminManager.verifyPassword("admin"))
        assertTrue(AdminManager.verifyPassword("  7860  "))
    }

    @Test
    fun adminPasswordVerification_invalidPasswords_returnFalse() {
        assertFalse(AdminManager.verifyPassword("wrongpass"))
        assertFalse(AdminManager.verifyPassword("12345"))
        assertFalse(AdminManager.verifyPassword(""))
    }
}
