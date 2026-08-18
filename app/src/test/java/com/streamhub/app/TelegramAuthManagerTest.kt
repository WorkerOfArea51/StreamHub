package com.streamhub.app

import com.streamhub.app.data.telegram.TelegramUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramAuthManagerTest {

    @Test
    fun telegramUser_displayName_formatsCorrectly() {
        val user1 = TelegramUser(id = 12345L, firstName = "Alice", lastName = "Smith")
        assertEquals("Alice Smith", user1.displayName)

        val user2 = TelegramUser(id = 67890L, firstName = "Bob", lastName = "")
        assertEquals("Bob", user2.displayName)
    }

    @Test
    fun telegramUser_formattedUsername_addsAtPrefix() {
        val user = TelegramUser(id = 12345L, firstName = "Charlie", username = "charlie_admin")
        assertEquals("@charlie_admin", user.formattedUsername)

        val noUsername = TelegramUser(id = 12345L, firstName = "Dave", username = "")
        assertEquals("", noUsername.formattedUsername)
    }

    @Test
    fun ownerUsernameMatching_caseInsensitiveAndCleaned() {
        val configuredOwners = "AdminUser, @super_owner, 987654321"
            .split(",")
            .map { it.trim().lowercase().removePrefix("@") }
            .filter { it.isNotBlank() }

        assertTrue("adminuser in owners", "adminuser" in configuredOwners)
        assertTrue("super_owner in owners", "super_owner" in configuredOwners)
        assertTrue("987654321 in owners", "987654321" in configuredOwners)
        assertFalse("random_user in owners", "random_user" in configuredOwners)
    }
}
