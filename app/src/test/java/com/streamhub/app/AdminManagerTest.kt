package com.streamhub.app

import com.streamhub.app.data.AdminManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminManagerTest {

    @Test
    fun adminPasswordVerification_validOwnerPassword_returnsTrue() {
        assertTrue(AdminManager.verifyPassword("StreamHub#Admin9872!"))
        assertTrue(AdminManager.verifyPassword("  StreamHub#Admin9872!  ")) // leading/trailing spaces trimmed
    }

    @Test
    fun adminPasswordVerification_invalidPasswords_returnFalse() {
        assertFalse(AdminManager.verifyPassword("wrongpass"))
        assertFalse(AdminManager.verifyPassword("12345"))
        assertFalse(AdminManager.verifyPassword(""))
        assertFalse(AdminManager.verifyPassword("admin"))
        assertFalse(AdminManager.verifyPassword("StreamHub#Admin9872")) // missing !
        assertFalse(AdminManager.verifyPassword("streamhub#admin9872!")) // case mismatch
    }

    @Test
    fun sha256_computesCorrectly() {
        val hash = AdminManager.sha256("StreamHub#Admin9872!")
        org.junit.Assert.assertEquals(AdminManager.MASTER_PASSWORD_SHA256, hash)
    }
}
