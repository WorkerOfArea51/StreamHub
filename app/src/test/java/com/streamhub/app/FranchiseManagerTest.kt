package com.streamhub.app

import com.streamhub.app.data.FranchiseManager
import com.streamhub.app.data.models.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FranchiseManagerTest {

    @Test
    fun detectSeasonNumber_standardSeasonFormats() {
        assertEquals(2, FranchiseManager.detectSeasonNumber("Solo Leveling Season 2"))
        assertEquals(3, FranchiseManager.detectSeasonNumber("Demon Slayer S3: Swordsmith Village"))
        assertEquals(4, FranchiseManager.detectSeasonNumber("Attack on Titan Season 4 Part 2"))
        assertEquals(1, FranchiseManager.detectSeasonNumber("Jujutsu Kaisen"))
    }

    @Test
    fun getFranchiseId_normalizesTitles() {
        val item1 = MediaItem(id = "1", title = "Solo Leveling Season 2: Arise from the Shadow")
        val item2 = MediaItem(id = "2", title = "Solo Leveling")

        val id1 = FranchiseManager.getFranchiseId(item1)
        val id2 = FranchiseManager.getFranchiseId(item2)

        assertEquals("solo-leveling", id1)
        assertEquals("solo-leveling", id2)
    }

    @Test
    fun getFranchiseTitle_cleansSubtitles() {
        val item = MediaItem(id = "1", title = "Demon Slayer: Kimetsu no Yaiba Season 2")
        val title = FranchiseManager.getFranchiseTitle(item)
        assertTrue("Title should contain Demon Slayer", title.contains("Demon Slayer", ignoreCase = true))
    }
}
