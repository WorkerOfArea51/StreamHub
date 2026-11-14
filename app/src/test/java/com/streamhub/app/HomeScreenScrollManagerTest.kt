package com.streamhub.app

import com.streamhub.app.data.HomeScreenScrollManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HomeScreenScrollManagerTest {

    @Before
    fun setUp() {
        HomeScreenScrollManager.resetAll()
    }

    @Test
    fun verticalScroll_retainsPositionAcrossInvocations() {
        assertEquals(0, HomeScreenScrollManager.verticalScrollIndex)
        assertEquals(0, HomeScreenScrollManager.verticalScrollOffset)

        HomeScreenScrollManager.verticalScrollIndex = 14
        HomeScreenScrollManager.verticalScrollOffset = 120

        assertEquals(14, HomeScreenScrollManager.verticalScrollIndex)
        assertEquals(120, HomeScreenScrollManager.verticalScrollOffset)

        HomeScreenScrollManager.resetVertical()
        assertEquals(0, HomeScreenScrollManager.verticalScrollIndex)
        assertEquals(0, HomeScreenScrollManager.verticalScrollOffset)
    }

    @Test
    fun horizontalScroll_retainsEachShelfIndependently() {
        // Shelf 1: Trending
        HomeScreenScrollManager.setHorizontalScroll("section_trending", 5, 45)

        // Shelf 2: Anime
        HomeScreenScrollManager.setHorizontalScroll("section_shelf_ANIME", 8, 90)

        // Verify independent retrieval
        val (trendIdx, trendOffset) = HomeScreenScrollManager.getHorizontalScroll("section_trending")
        assertEquals(5, trendIdx)
        assertEquals(45, trendOffset)

        val (animeIdx, animeOffset) = HomeScreenScrollManager.getHorizontalScroll("section_shelf_ANIME")
        assertEquals(8, animeIdx)
        assertEquals(90, animeOffset)

        // Unset shelf defaults to (0, 0)
        val (defaultIdx, defaultOffset) = HomeScreenScrollManager.getHorizontalScroll("section_unknown")
        assertEquals(0, defaultIdx)
        assertEquals(0, defaultOffset)
    }
}
