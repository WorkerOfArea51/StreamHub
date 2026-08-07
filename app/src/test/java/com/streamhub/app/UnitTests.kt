package com.streamhub.app

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitTests {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun telegramLinkRegex_parsesPrivateChannelLink() {
        val regex = Regex("""https?://t\.me/c/(\d+)/(\d+)""")
        val match = regex.find("https://t.me/c/2633457020/7159")
        assert(match != null)
        assertEquals("2633457020", match!!.groupValues[1])
        assertEquals("7159", match.groupValues[2])
    }

    @Test
    fun telegramLinkRegex_parsesPublicChannelLink() {
        val regex = Regex("""https?://t\.me/([a-zA-Z][\w]{4,31})/(\d+)""")
        val match = regex.find("https://t.me/AnimeChannel/42")
        assert(match != null)
        assertEquals("AnimeChannel", match!!.groupValues[1])
        assertEquals("42", match.groupValues[2])
    }

    @Test
    fun chatIdParsing_bareChannelId_toSupergroupChatId() {
        val bareId = 2633457020L
        val chatId = -1_000_000_000_000L - bareId
        assertEquals(-1002633457020L, chatId)
    }
}
