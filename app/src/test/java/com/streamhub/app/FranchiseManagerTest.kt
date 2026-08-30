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

    @Test
    fun getFranchiseItems_groupsAndSortsChronologically() {
        val s1 = MediaItem(id = "s1", title = "My Hero Academia Season 1", releaseYear = "2016", seasonNumber = 1)
        val s3 = MediaItem(id = "s3", title = "My Hero Academia Season 3", releaseYear = "2018", seasonNumber = 3)
        val s2 = MediaItem(id = "s2", title = "My Hero Academia Season 2", releaseYear = "2017", seasonNumber = 2)
        val unrelated = MediaItem(id = "other", title = "One Piece", releaseYear = "1999", seasonNumber = 1)

        val catalog = listOf(s3, unrelated, s1, s2)
        val franchiseGroup = FranchiseManager.getFranchiseItems(s1, catalog)

        assertEquals("Should only contain 3 franchise items", 3, franchiseGroup.size)
        assertEquals("s1 should be first", "s1", franchiseGroup[0].id)
        assertEquals("s2 should be second", "s2", franchiseGroup[1].id)
        assertEquals("s3 should be third", "s3", franchiseGroup[2].id)
    }

    @Test
    fun getFranchiseTag_compoundMultiRelationLabels() {
        val tvSeries = MediaItem(
            id = "anohana_tv",
            title = "Anohana: The Flower We Saw That Day",
            type = "SERIES",
            category = "Anime",
            releaseYear = "2011",
            seasonNumber = 1
        )

        val sequelMovie = MediaItem(
            id = "anohana_movie",
            title = "Anohana: The Flower We Saw That Day The Movie",
            type = "MOVIE",
            category = "Movie",
            relationType = "Sequel",
            releaseYear = "2013",
            seasonNumber = 0
        )

        // When viewing TV Series:
        assertEquals("CURRENT • TV", FranchiseManager.getFranchiseTag(tvSeries, tvSeries))
        assertEquals("SEQUEL • MOVIE", FranchiseManager.getFranchiseTag(sequelMovie, tvSeries))

        // When viewing Sequel Movie:
        assertEquals("CURRENT • MOVIE", FranchiseManager.getFranchiseTag(sequelMovie, sequelMovie))
        assertEquals("PREQUEL • TV", FranchiseManager.getFranchiseTag(tvSeries, sequelMovie))
    }
}
