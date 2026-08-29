package com.streamhub.app

import com.streamhub.app.data.AdminManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminManagerTest {

    @Test
    fun adminPasswordVerification_invalidPasswords_returnFalse() {
        assertFalse(AdminManager.verifyPassword("wrongpass"))
        assertFalse(AdminManager.verifyPassword("12345"))
        assertFalse(AdminManager.verifyPassword(""))
        assertFalse(AdminManager.verifyPassword("admin"))
        assertFalse(AdminManager.verifyPassword("7860"))
    }
}
