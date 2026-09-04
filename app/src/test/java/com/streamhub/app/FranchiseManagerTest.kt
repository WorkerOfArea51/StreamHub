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
        val s2Options = FranchiseManager.buildSeasonOptions(s2, catalog)
        assertEquals(3, s2Options.size)
        assertTrue(s2Options.any { it.id == "aot_1" && !it.isCurrent })
        assertTrue(s2Options.any { it.id == "aot_2" && it.isCurrent })
        assertTrue(s2Options.any { it.id == "aot_3" && !it.isCurrent })
    }

    @Test
    fun buildArcOptions_extractsInternalArcsCleanly() {
        val blackCloverS1 = MediaItem(
            id = "bc_1",
            title = "Black Clover",
            seasonNumber = 1,
            episodes = listOf(
                com.streamhub.app.data.models.Episode(episodeNumber = 1, arcName = "Magic Knights Entrance Arc", title = "EP - 01 - Asta and Yuno"),
                com.streamhub.app.data.models.Episode(episodeNumber = 2, arcName = "Magic Knights Entrance Arc", title = "EP - 02 - A Boy's Vow"),
                com.streamhub.app.data.models.Episode(episodeNumber = 14, arcName = "Dungeon Exploration Arc", title = "EP - 14 - Dungeon"),
            )
        )

        val arcOptions = FranchiseManager.buildArcOptions(blackCloverS1)
        assertEquals(2, arcOptions.size)
        assertEquals("Arc 1: Magic Knights Entrance Arc", arcOptions[0].title)
        assertEquals(2, arcOptions[0].episodeCount)
        assertEquals("Arc 2: Dungeon Exploration Arc", arcOptions[1].title)
        assertEquals(1, arcOptions[1].episodeCount)
    }

    @Test
    fun testFranchise_seasonsAndSequelMovieChronology() {
        val s1 = MediaItem(
            id = "code_geass_s1",
            title = "Code Geass: Lelouch of the Rebellion",
            category = "Anime",
            type = "SERIES",
            releaseYear = "2006",
            seasonNumber = 1,
            franchiseId = "code-geass"
        )
        val s2 = MediaItem(
            id = "code_geass_s2",
            title = "Code Geass: Lelouch of the Rebellion R2",
            category = "Anime",
            type = "SERIES",
            releaseYear = "2008",
            seasonNumber = 2,
            franchiseId = "code-geass"
        )
        val movie = MediaItem(
            id = "code_geass_movie",
            title = "Code Geass: Lelouch of the Re;surrection",
            category = "Movie",
            type = "MOVIE",
            relationType = "Movie",
            releaseYear = "2019",
            seasonNumber = 0,
            franchiseId = "code-geass"
        )

        val catalog = listOf(movie, s2, s1)
        val sortedFranchise = FranchiseManager.getFranchiseItems(s1, catalog)

        assertEquals(3, sortedFranchise.size)
        assertEquals("s1 should be first (2006)", "code_geass_s1", sortedFranchise[0].id)
        assertEquals("s2 should be second (2008)", "code_geass_s2", sortedFranchise[1].id)
        assertEquals("movie should be third (2019)", "code_geass_movie", sortedFranchise[2].id)

        // Tags when viewing the movie:
        assertEquals("PREQUEL • TV", FranchiseManager.getFranchiseTag(s1, movie))
        assertEquals("PREQUEL • TV", FranchiseManager.getFranchiseTag(s2, movie))
        assertEquals("CURRENT • MOVIE", FranchiseManager.getFranchiseTag(movie, movie))

        // Tags when viewing S2:
        assertEquals("PREQUEL • TV", FranchiseManager.getFranchiseTag(s1, s2))
        assertEquals("CURRENT • TV", FranchiseManager.getFranchiseTag(s2, s2))
        assertEquals("SEQUEL • MOVIE", FranchiseManager.getFranchiseTag(movie, s2))
    }

    @Test
    fun testFranchise_adminExplicitSeasonOrderOverridesReleaseYear() {
        // Scenario: Movie released in 2008, but in 2019 an anime prequel was released.
        // Admin assigns Season 1 to the 2019 anime and Season 2 to the 2008 movie.
        val animePrequel2019 = MediaItem(
            id = "anime_2019",
            title = "Origins Story",
            releaseYear = "2019",
            seasonNumber = 1,
            category = "Anime",
            type = "SERIES",
            franchiseId = "origin-universe"
        )
        val movie2008 = MediaItem(
            id = "movie_2008",
            title = "The Main Movie",
            releaseYear = "2008",
            seasonNumber = 2,
            category = "Movie",
            type = "MOVIE",
            franchiseId = "origin-universe"
        )

        val catalog = listOf(movie2008, animePrequel2019)
        val sorted = FranchiseManager.getFranchiseItems(animePrequel2019, catalog)

        // The 2019 anime (Season 1) MUST come before the 2008 movie (Season 2)
        assertEquals("anime_2019", sorted[0].id)
        assertEquals("movie_2008", sorted[1].id)
    }

    @Test
    fun testFranchise_prequelTagOverridesReleaseYear() {
        // Scenario: Standalone Movie in 2021 tagged as "Prequel" relative to 2018 TV Series
        val mainSeries2018 = MediaItem(
            id = "main_2018",
            title = "Main Anime",
            releaseYear = "2018",
            seasonNumber = 1,
            category = "Anime",
            type = "SERIES",
            franchiseId = "main-universe"
        )
        val prequelMovie2021 = MediaItem(
            id = "prequel_2021",
            title = "Prequel Zero Movie",
            releaseYear = "2021",
            seasonNumber = 0,
            relationType = "Prequel",
            category = "Movie",
            type = "MOVIE",
            franchiseId = "main-universe"
        )

        val catalog = listOf(mainSeries2018, prequelMovie2021)
        val sorted = FranchiseManager.getFranchiseItems(mainSeries2018, catalog)

        // Prequel movie MUST come before the main series
        assertEquals("prequel_2021", sorted[0].id)
        assertEquals("main_2018", sorted[1].id)
    }

    @Test
    fun testFranchise_movieWithCustomSeasonNumberMaintainsMovieFormatAndSubtitle() {
        // Scenario: Dragon Ball Super (TV) + Dragon Ball Super: Broly (Movie assigned Season # 4)
        val dbsTv = MediaItem(
            id = "dbs_tv",
            title = "Dragon Ball Super",
            category = "Anime",
            type = "SERIES",
            releaseYear = "2015",
            seasonNumber = 4,
            totalEpisodes = "131 Eps",
            franchiseId = "dragon-ball"
        )
        val brolyMovie = MediaItem(
            id = "dbs_broly",
            title = "Dragon Ball Super: Broly",
            category = "Anime",
            type = "MOVIE",
            relationType = "Sequel • Movie",
            releaseYear = "2018",
            duration = "100 min",
            seasonNumber = 4,
            franchiseId = "dragon-ball"
        )

        // 1. Format label must remain MOVIE
        assertEquals("MOVIE", FranchiseManager.getMediaFormatLabel(brolyMovie))
        assertEquals("TV", FranchiseManager.getMediaFormatLabel(dbsTv))

        // 2. Tag must be SEQUEL • MOVIE
        assertEquals("SEQUEL • MOVIE", FranchiseManager.getFranchiseTag(brolyMovie, dbsTv))

        // 3. Subtitle must show release year and duration, NOT "Season 4"
        val subtitle = FranchiseManager.getSeasonCardSubtitle(brolyMovie)
        assertEquals("2018 • 100 min", subtitle)
    }

    @Test
    fun testFranchise_explicitFranchiseOrderOverridesReleaseYearAndFormats() {
        // Scenario: Mushoku Tensei franchise timeline ordering
        // S1 Pt 1 released in 2021
        // S1 Pt 2 released in 2021
        // Eris OVA released in 2022, but creator explicitly wants it ordered 2nd (between S1 and S1 Pt 2)
        val s1Pt1 = MediaItem(
            id = "mt_s1_pt1",
            title = "Mushoku Tensei: Jobless Reincarnation",
            category = "Anime",
            type = "SERIES",
            releaseYear = "2021",
            seasonNumber = 1,
            franchiseOrder = 1.0,
            franchiseId = "mushoku-tensei"
        )
        val erisOva = MediaItem(
            id = "mt_eris_ova",
            title = "Mushoku Tensei: Jobless Reincarnation - Eris the Goblin Slayer",
            category = "Anime",
            type = "MOVIE",
            relationType = "Side Story • Special",
            releaseYear = "2022",
            seasonNumber = 1,
            franchiseOrder = 2.0,
            franchiseId = "mushoku-tensei"
        )
        val s1Pt2 = MediaItem(
            id = "mt_s1_pt2",
            title = "Mushoku Tensei: Jobless Reincarnation Season 1 Pt 2",
            category = "Anime",
            type = "SERIES",
            releaseYear = "2021",
            seasonNumber = 1,
            partNumber = 2,
            franchiseOrder = 3.0,
            franchiseId = "mushoku-tensei"
        )
        val s2Pt1 = MediaItem(
            id = "mt_s2_pt1",
            title = "Mushoku Tensei: Jobless Reincarnation Season 2",
            category = "Anime",
            type = "SERIES",
            releaseYear = "2023",
            seasonNumber = 2,
            franchiseOrder = 4.0,
            franchiseId = "mushoku-tensei"
        )

        val catalog = listOf(s2Pt1, s1Pt2, erisOva, s1Pt1)
        val sorted = FranchiseManager.getFranchiseItems(erisOva, catalog)

        assertEquals(4, sorted.size)
        assertEquals("1st should be S1 Pt 1", "mt_s1_pt1", sorted[0].id)
        assertEquals("2nd should be Eris OVA", "mt_eris_ova", sorted[1].id)
        assertEquals("3rd should be S1 Pt 2", "mt_s1_pt2", sorted[2].id)
        assertEquals("4th should be S2 Pt 1", "mt_s2_pt1", sorted[3].id)

        // Verify badges when viewing Eris OVA:
        assertEquals("S1 Pt 1 should be PREQUEL relative to Eris", "PREQUEL • TV", FranchiseManager.getFranchiseTag(s1Pt1, erisOva))
        assertEquals("Eris should be CURRENT relative to itself", "CURRENT • SPECIAL", FranchiseManager.getFranchiseTag(erisOva, erisOva))
        assertEquals("S1 Pt 2 should be SEQUEL relative to Eris", "SEQUEL • TV", FranchiseManager.getFranchiseTag(s1Pt2, erisOva))
        assertEquals("S2 Pt 1 should be SEQUEL relative to Eris", "SEQUEL • TV", FranchiseManager.getFranchiseTag(s2Pt1, erisOva))
    }
}
