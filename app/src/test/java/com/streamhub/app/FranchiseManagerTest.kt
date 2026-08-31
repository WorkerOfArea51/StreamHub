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

    @Test
    fun detectSeasonNumber_colonAndHyphenSeparators() {
        assertEquals(1, FranchiseManager.detectSeasonNumber("Squid Game : Season 1"))
        assertEquals(2, FranchiseManager.detectSeasonNumber("Squid Game : Season 2"))
        assertEquals(3, FranchiseManager.detectSeasonNumber("Squid Game - Season 3"))
        assertEquals(1, FranchiseManager.detectSeasonNumber("Squid Game (Season 1)"))
    }

    @Test
    fun getFranchiseId_colonSeparatedSeasons() {
        val s1 = MediaItem(id = "1", title = "Squid Game : Season 1")
        val s2 = MediaItem(id = "2", title = "Squid Game : Season 2")
        assertEquals(FranchiseManager.getFranchiseId(s1), FranchiseManager.getFranchiseId(s2))
    }

    @Test
    fun getFranchiseTag_relativeMultiSeasonProgression() {
        val s1 = MediaItem(id = "aot_1", title = "Attack on Titan", seasonNumber = 1, releaseYear = "2013", relationType = "Prequel", category = "Anime", type = "SERIES")
        val s2 = MediaItem(id = "aot_2", title = "Attack on Titan Season 2", seasonNumber = 2, releaseYear = "2017", relationType = "Sequel", category = "Anime", type = "SERIES")
        val s3 = MediaItem(id = "aot_3", title = "Attack on Titan Season 3", seasonNumber = 3, releaseYear = "2018", relationType = "Sequel", category = "Anime", type = "SERIES")

        // When viewing Season 1:
        assertEquals("CURRENT • TV", FranchiseManager.getFranchiseTag(s1, s1))
        assertEquals("SEQUEL • TV", FranchiseManager.getFranchiseTag(s2, s1))
        assertEquals("SEQUEL • TV", FranchiseManager.getFranchiseTag(s3, s1))

        // When viewing Season 2:
        assertEquals("PREQUEL • TV", FranchiseManager.getFranchiseTag(s1, s2))
        assertEquals("CURRENT • TV", FranchiseManager.getFranchiseTag(s2, s2))
        assertEquals("SEQUEL • TV", FranchiseManager.getFranchiseTag(s3, s2))

        // When viewing Season 3:
        assertEquals("PREQUEL • TV", FranchiseManager.getFranchiseTag(s1, s3))
        assertEquals("PREQUEL • TV", FranchiseManager.getFranchiseTag(s2, s3))
        assertEquals("CURRENT • TV", FranchiseManager.getFranchiseTag(s3, s3))
    }

    @Test
    fun getFranchiseTag_tvSpecialAndOvaCompoundRelations() {
        val s4Part2 = MediaItem(
            id = "aot_s4p2",
            title = "Attack on Titan: Final Season Part 2",
            seasonNumber = 4,
            releaseYear = "2022",
            relationType = "Sequel • TV",
            category = "Anime",
            type = "SERIES"
        )

        val finalChapters = MediaItem(
            id = "aot_final_chapters",
            title = "Attack on Titan: The Final Chapters",
            seasonNumber = 4,
            releaseYear = "2023",
            relationType = "Sequel • TV Special",
            category = "Anime",
            type = "SERIES"
        )

        val ovaNoRegrets = MediaItem(
            id = "aot_no_regrets",
            title = "Attack on Titan: No Regrets",
            seasonNumber = 0,
            releaseYear = "2014",
            relationType = "Side Story • OVA",
            category = "Anime",
            type = "SERIES"
        )

        // When viewing Season 4 Part 2:
        assertEquals("CURRENT • TV", FranchiseManager.getFranchiseTag(s4Part2, s4Part2))
        assertEquals("SEQUEL • TV SPECIAL", FranchiseManager.getFranchiseTag(finalChapters, s4Part2))
        assertEquals("SIDE STORY • OVA", FranchiseManager.getFranchiseTag(ovaNoRegrets, s4Part2))

        // When viewing Final Chapters:
        assertEquals("PREQUEL • TV", FranchiseManager.getFranchiseTag(s4Part2, finalChapters))
        assertEquals("CURRENT • TV SPECIAL", FranchiseManager.getFranchiseTag(finalChapters, finalChapters))
        assertEquals("SIDE STORY • OVA", FranchiseManager.getFranchiseTag(ovaNoRegrets, finalChapters))
    }

    @Test
    fun buildSeasonArcOptions_includesAllFranchiseSeasons() {
        val s1 = MediaItem(id = "aot_1", title = "Attack on Titan", seasonNumber = 1, releaseYear = "2013", relationType = "Prequel", category = "Anime", type = "SERIES")
        val s2 = MediaItem(id = "aot_2", title = "Attack on Titan Season 2", seasonNumber = 2, releaseYear = "2017", relationType = "Sequel • TV", category = "Anime", type = "SERIES")
        val s3 = MediaItem(id = "aot_3", title = "Attack on Titan Season 3", seasonNumber = 3, releaseYear = "2018", relationType = "Sequel • TV", category = "Anime", type = "SERIES")
        val catalog = listOf(s1, s2, s3)

        // When viewing Season 2, all 3 seasons MUST be present in options:
        val s2Options = FranchiseManager.buildSeasonArcOptions(s2, catalog, 2)
        assertEquals(3, s2Options.size)
        assertTrue(s2Options.any { it.id == "aot_1" && !it.isCurrent })
        assertTrue(s2Options.any { it.id == "aot_2" && it.isCurrent })
        assertTrue(s2Options.any { it.id == "aot_3" && !it.isCurrent })
    }
}
