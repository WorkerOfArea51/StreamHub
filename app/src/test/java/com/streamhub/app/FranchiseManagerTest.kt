package com.streamhub.app

import com.streamhub.app.data.FranchiseManager
import com.streamhub.app.data.models.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        // 3. Subtitle must show Movie number and duration, NOT "Season 4"
        val subtitle = FranchiseManager.getSeasonCardSubtitle(brolyMovie)
        assertEquals("Movie 4 • 2018 • 100 min", subtitle)
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

    @Test
    fun testGenerateReadableMediaId_doesNotTruncateUniqueSubtitles() {
        val fitzId = com.streamhub.app.ui.components.generateReadableMediaId(
            title = "Mushoku Tensei: Jobless Reincarnation Season 2 - Guardian Fitz",
            releaseYear = "2023"
        )
        val s2Id = com.streamhub.app.ui.components.generateReadableMediaId(
            title = "Mushoku Tensei: Jobless Reincarnation Season 2",
            releaseYear = "2023"
        )

        // Must NOT collide!
        org.junit.Assert.assertNotEquals("IDs must be distinct to prevent Firestore overwrite", fitzId, s2Id)
        org.junit.Assert.assertTrue("fitzId should contain guardian_fitz", fitzId.contains("guardian_fitz"))
    }

    @Test
    fun testFranchiseTags_multiRelationPreservesSequelAndSideStoryAndFormat() {
        // Scenario reported by user: Naruto: Shippuden (Current) vs The Last: Naruto the Movie (Sequel • Side Story • Movie)
        val shippuden = MediaItem(
            id = "naruto_shippuden",
            title = "Naruto: Shippuden",
            category = "Anime",
            type = "SERIES",
            releaseYear = "2007",
            seasonNumber = 2,
            franchiseId = "naruto"
        )
        val theLastMovie = MediaItem(
            id = "naruto_the_last",
            title = "The Last: Naruto the Movie",
            category = "Anime",
            type = "MOVIE",
            relationType = "Sequel • Side Story • Movie",
            releaseYear = "2014",
            seasonNumber = 0,
            franchiseId = "naruto"
        )

        val tags = FranchiseManager.getFranchiseTags(theLastMovie, shippuden)
        val tagLabels = tags.map { it.label }

        assertEquals("Must contain 3 tags: SEQUEL, SIDE STORY, MOVIE", listOf("SEQUEL", "SIDE STORY", "MOVIE"), tagLabels)
        assertEquals("SEQUEL • SIDE STORY • MOVIE", FranchiseManager.getFranchiseTag(theLastMovie, shippuden))

        // When viewing The Last: Naruto the Movie, Shippuden must be tagged PREQUEL • TV
        val shippudenTags = FranchiseManager.getFranchiseTags(shippuden, theLastMovie)
        assertEquals(listOf("PREQUEL", "TV"), shippudenTags.map { it.label })
        assertEquals("PREQUEL • TV", FranchiseManager.getFranchiseTag(shippuden, theLastMovie))
    }

    @Test
    fun testFranchiseTags_multiRelationPreservesPrequelAndSideStoryAndOva() {
        val currentShow = MediaItem(
            id = "main_show",
            title = "Main Series Season 2",
            category = "Anime",
            type = "SERIES",
            releaseYear = "2020",
            seasonNumber = 2,
            franchiseId = "universe"
        )
        val prequelOva = MediaItem(
            id = "prequel_side_ova",
            title = "Origins Story",
            category = "Anime",
            type = "SERIES",
            relationType = "Prequel • Side Story • OVA",
            releaseYear = "2018",
            seasonNumber = 0,
            franchiseId = "universe"
        )

        val tags = FranchiseManager.getFranchiseTags(prequelOva, currentShow)
        val tagLabels = tags.map { it.label }

        assertEquals(listOf("PREQUEL", "SIDE STORY", "OVA"), tagLabels)
        assertEquals("PREQUEL • SIDE STORY • OVA", FranchiseManager.getFranchiseTag(prequelOva, currentShow))
    }

    @Test
    fun testUnovaTitleDoesNotMatchOva() {
        val unova = MediaItem(
            id = "pokemon_unova",
            title = "Pokémon BW: Adventures in Unova",
            category = "Anime",
            type = "SERIES",
            relationType = "Sequel, TV",
            releaseYear = "2013",
            seasonNumber = 6,
            totalEpisodes = "14",
            franchiseId = "pokemon",
            episodes = (1..14).map { 
                com.streamhub.app.data.models.Episode(episodeNumber = it, seasonNumber = 6, title = "Episode $it")
            }
        )

        // 1. Format label must be "TV", NOT "OVA"
        val format = FranchiseManager.getMediaFormatLabel(unova)
        assertEquals("TV", format)

        // 2. Tags for current must be CURRENT, TV
        val tags = FranchiseManager.getFranchiseTags(unova, unova)
        assertEquals(listOf("CURRENT", "TV"), tags.map { it.label })

        // 3. Subtitle must contain Season 6, 2013, 14 Episodes
        val subtitle = FranchiseManager.getSeasonCardSubtitle(unova, includeDuration = false)
        assertEquals("Season 6 • 2013 • 14 Episodes", subtitle)
    }

    @Test
    fun testAvengersEndgameVsEncoreEdition() {
        val endgame = MediaItem(
            id = "avengers_endgame",
            title = "Avengers: Endgame",
            category = "Movies",
            type = "MOVIE",
            releaseYear = "2019",
            seasonNumber = 0,
            franchiseOrder = 4.0,
            relationType = "Main Story • Movie",
            franchiseId = "avengers"
        )
        val encore = MediaItem(
            id = "avengers_encore",
            title = "Avengers Endgame: Encore",
            category = "Movies",
            type = "MOVIE",
            releaseYear = "2026",
            seasonNumber = 0,
            franchiseOrder = 4.1,
            relationType = "Encore • Movie",
            franchiseId = "avengers"
        )

        val catalog = listOf(encore, endgame)
        val sorted = FranchiseManager.getFranchiseItems(endgame, catalog)

        assertEquals("Endgame must come first", "avengers_endgame", sorted[0].id)
        assertEquals("Encore must come second", "avengers_encore", sorted[1].id)

        // When viewing Endgame, Encore must be tagged ENCORE • MOVIE (NOT SEQUEL!)
        val encoreTags = FranchiseManager.getFranchiseTags(encore, endgame)
        val labels = encoreTags.map { it.label }
        assertEquals(listOf("ENCORE", "MOVIE"), labels)
        assertTrue("Encore must never be auto-tagged as SEQUEL", labels.none { it == "SEQUEL" })

        // Options dropdown
        val options = FranchiseManager.buildSeasonOptions(endgame, catalog)
        val encoreOption = options.first { it.id == "avengers_encore" }
        assertEquals("ENCORE", encoreOption.badge)
        assertEquals("Encore Cut", encoreOption.shortLabel)
    }

    @Test
    fun testScissorSevenVsExtrasZeroSeason() {
        val extras = MediaItem(
            id = "scissor_extras",
            title = "Scissor Seven Extras",
            category = "ANIME",
            type = "SERIES",
            releaseYear = "2018",
            seasonNumber = 0,
            relationType = "Prequel • Extra • ONA",
            franchiseId = "scissor-seven"
        )
        val s1 = MediaItem(
            id = "scissor_s1",
            title = "Scissor Seven",
            category = "ANIME",
            type = "SERIES",
            releaseYear = "2018",
            seasonNumber = 1,
            relationType = "Main Story • ONA",
            franchiseId = "scissor-seven"
        )
        val s2 = MediaItem(
            id = "scissor_s2",
            title = "Scissor Seven Season 2",
            category = "ANIME",
            type = "SERIES",
            releaseYear = "2019",
            seasonNumber = 2,
            relationType = "Sequel • ONA",
            franchiseId = "scissor-seven"
        )

        val catalog = listOf(s2, extras, s1)
        val sorted = FranchiseManager.getFranchiseItems(s1, catalog)

        // Verify zero-season behavior:
        assertEquals("Extras effective season must be 0", 0, FranchiseManager.getEffectiveSeasonNumber(extras))
        assertEquals("S1 effective season must be 1", 1, FranchiseManager.getEffectiveSeasonNumber(s1))
        assertEquals("S2 effective season must be 2", 2, FranchiseManager.getEffectiveSeasonNumber(s2))

        // Options dropdown badges:
        val options = FranchiseManager.buildSeasonOptions(s1, catalog)
        val extrasOpt = options.first { it.id == "scissor_extras" }
        val s1Opt = options.first { it.id == "scissor_s1" }
        val s2Opt = options.first { it.id == "scissor_s2" }

        assertEquals("Extras badge must be EXTRA, not S1", "EXTRA", extrasOpt.badge)
        assertEquals("S1 badge must be S1", "S1", s1Opt.badge)
        assertEquals("S2 badge must be S2", "S2", s2Opt.badge)

        // Subtitles:
        val extrasSubtitle = FranchiseManager.getSeasonCardSubtitle(extras, includeDuration = false)
        assertTrue("Extras subtitle should not say Season 0", !extrasSubtitle.contains("Season 0"))
    }

    @Test
    fun testAttackOnTitanFinalSeasonTheFinalChaptersSeriesFormatMasterAuthority() {
        // Attack on Titan: The Final Chapters is added as FORMAT: SERIES with Season 4, Part 3, and relation "TV Special"
        val aotS4P2 = MediaItem(
            id = "aot_s4_p2",
            title = "Attack on Titan Final Season Part 2",
            type = "SERIES",
            category = "ANIME",
            seasonNumber = 4,
            partNumber = 2,
            releaseYear = "2022",
            totalEpisodes = "12 Episodes",
            relationType = "Prequel, TV",
            franchiseId = "attack-on-titan",
            franchiseTitle = "Attack on Titan"
        )
        val aotFinalChapters = MediaItem(
            id = "aot_s4_p3",
            title = "Attack on Titan Final Season THE FINAL CHAPTERS",
            type = "SERIES",
            category = "ANIME",
            seasonNumber = 4,
            partNumber = 3,
            releaseYear = "2023",
            totalEpisodes = "2 Episodes",
            relationType = "TV Special",
            franchiseId = "attack-on-titan",
            franchiseTitle = "Attack on Titan"
        )

        val catalog = listOf(aotS4P2, aotFinalChapters)

        // 1. Overview Tab Master Authority: isMovie must be FALSE because type == SERIES
        assertFalse("Series format on Overview tab must prevent isMovie", FranchiseManager.isMovie(aotFinalChapters))

        // 2. Season and Part numbers must be preserved
        assertEquals("Effective season must be 4", 4, FranchiseManager.getEffectiveSeasonNumber(aotFinalChapters))
        assertEquals("Effective part must be 3", 3, FranchiseManager.getEffectivePartNumber(aotFinalChapters))

        // 3. Card subtitle MUST display Season 4 Pt 3, NOT Movie 3!
        val subtitle = FranchiseManager.getSeasonCardSubtitle(aotFinalChapters, includeDuration = false)
        assertEquals("Season 4 Pt 3 • 2023 • 2 Episodes", subtitle)

        // 4. Dropdown options must show Season 4 Pt 3 and S4 P3 badge
        val options = FranchiseManager.buildSeasonOptions(aotFinalChapters, catalog)
        val p3Opt = options.first { it.id == "aot_s4_p3" }
        assertEquals("Short label must be Season 4 Pt 3", "Season 4 Pt 3", p3Opt.shortLabel)
        assertEquals("Badge must be S4 P3", "S4 P3", p3Opt.badge)
    }

    @Test
    fun testDemonSlayerInfinityCastleMovie1FranchiseOrder6NeverDisplaysM6() {
        val s1 = MediaItem(id = "ds_s1", title = "Demon Slayer: Kimetsu no Yaiba", type = "SERIES", seasonNumber = 1, franchiseId = "demon-slayer")
        val s2 = MediaItem(id = "ds_s2", title = "Demon Slayer Mugen Train Arc", type = "SERIES", seasonNumber = 2, franchiseId = "demon-slayer")
        val s3 = MediaItem(id = "ds_s3", title = "Demon Slayer Entertainment District Arc", type = "SERIES", seasonNumber = 3, franchiseId = "demon-slayer")
        val s4 = MediaItem(id = "ds_s4", title = "Demon Slayer Swordsmith Village Arc", type = "SERIES", seasonNumber = 4, franchiseId = "demon-slayer")
        val s5 = MediaItem(id = "ds_s5", title = "Demon Slayer Hashira Training Arc", type = "SERIES", seasonNumber = 5, franchiseId = "demon-slayer")
        val movie1 = MediaItem(
            id = "ds_infinity_castle_1",
            title = "Demon Slayer: Kimetsu no Yaiba Infinity Castle",
            type = "MOVIE",
            seasonNumber = 1, // Movie # 1
            partNumber = 0,
            franchiseOrder = 6.0, // Sort 6th on the timeline
            releaseYear = "2025",
            relationType = "Sequel • Movie",
            franchiseId = "demon-slayer",
            duration = "120 min"
        )

        val catalog = listOf(movie1, s3, s1, s5, s2, s4)
        val sorted = FranchiseManager.getFranchiseItems(s1, catalog)

        // 1. Sorting Invariant: Movie 1 with franchiseOrder 6.0 MUST sort 6th at the very end
        assertEquals("Should contain 6 items", 6, sorted.size)
        assertEquals("Last item must be Infinity Castle", "ds_infinity_castle_1", sorted.last().id)

        // 2. Poster Badge Invariant: Movie 1 with franchiseOrder 6.0 MUST display M1, NEVER M6!
        val posterBadge = FranchiseManager.getPosterBadge(movie1)
        assertEquals("Badge must be M1, NEVER M6", "M1", posterBadge)

        // 3. Subtitle Invariant: Subtitle must say Movie 1
        val subtitle = FranchiseManager.getSeasonCardSubtitle(movie1, includeDuration = true)
        assertTrue("Subtitle must contain Movie 1", subtitle.contains("Movie 1"))
        assertTrue("Subtitle must contain 2025", subtitle.contains("2025"))
        assertTrue("Subtitle must contain 120min or 120 min", subtitle.contains("120min") || subtitle.contains("120m") || subtitle.contains("120 min"))
    }

    @Test
    fun testCodeGeassLelouchOfTheResurrectionStandaloneMovie() {
        val s1 = MediaItem(id = "cg_s1", title = "Code Geass: Lelouch of the Rebellion", type = "SERIES", seasonNumber = 1, franchiseId = "code-geass")
        val s2 = MediaItem(id = "cg_s2", title = "Code Geass: Lelouch of the Rebellion R2", type = "SERIES", seasonNumber = 2, franchiseId = "code-geass")
        val movie = MediaItem(
            id = "cg_resurrection",
            title = "Code Geass: Lelouch of the Re;surrection",
            type = "MOVIE",
            seasonNumber = 0, // Standalone Movie
            partNumber = 0,
            franchiseOrder = 0.0,
            releaseYear = "2019",
            relationType = "Sequel • Movie",
            franchiseId = "code-geass",
            duration = "112 min"
        )

        // Standalone movie with seasonNumber = 0 MUST display MOVIE badge
        assertEquals("Code Geass movie badge must be MOVIE", "MOVIE", FranchiseManager.getPosterBadge(movie))
        val subtitle = FranchiseManager.getSeasonCardSubtitle(movie, includeDuration = true)
        assertTrue("Subtitle must contain 2019", subtitle.contains("2019"))
        assertFalse("Subtitle must not say Movie 0", subtitle.contains("Movie 0"))
    }

    @Test
    fun testDunePartOneAndPartTwoStandaloneMoviesWithParts() {
        val dune1 = MediaItem(
            id = "dune_pt1",
            title = "Dune: Part One",
            type = "MOVIE",
            seasonNumber = 0, // Standalone movie franchise
            partNumber = 1,   // Part 1
            franchiseOrder = 1.0,
            releaseYear = "2021",
            relationType = "Prequel • Movie",
            franchiseId = "dune"
        )
        val dune2 = MediaItem(
            id = "dune_pt2",
            title = "Dune: Part Two",
            type = "MOVIE",
            seasonNumber = 0,
            partNumber = 2,   // Part 2
            franchiseOrder = 2.0,
            releaseYear = "2024",
            relationType = "Sequel • Movie",
            franchiseId = "dune"
        )

        // Standalone movies with parts must display Pt 1 and Pt 2
        assertEquals("Dune 1 badge must be Pt 1", "Pt 1", FranchiseManager.getPosterBadge(dune1))
        assertEquals("Dune 2 badge must be Pt 2", "Pt 2", FranchiseManager.getPosterBadge(dune2))

        val subtitle1 = FranchiseManager.getSeasonCardSubtitle(dune1, includeDuration = false)
        val subtitle2 = FranchiseManager.getSeasonCardSubtitle(dune2, includeDuration = false)
        assertTrue("Subtitle 1 must contain Part 1", subtitle1.contains("Part 1"))
        assertTrue("Subtitle 2 must contain Part 2", subtitle2.contains("Part 2"))
    }

    @Test
    fun testMultiMovieSeriesWithParts() {
        val movie1Pt2 = MediaItem(
            id = "film_m1_p2",
            title = "Epic Movie 1 Part 2",
            type = "MOVIE",
            seasonNumber = 1, // Movie 1
            partNumber = 2,   // Part 2
            releaseYear = "2025",
            relationType = "Movie",
            franchiseId = "epic"
        )

        assertEquals("Multi-movie with parts must be M1 P2", "M1 P2", FranchiseManager.getPosterBadge(movie1Pt2))
        val subtitle = FranchiseManager.getSeasonCardSubtitle(movie1Pt2, includeDuration = false)
        assertTrue("Subtitle must contain Movie 1 Pt 2", subtitle.contains("Movie 1 Pt 2"))
    }

    @Test
    fun testSwordArtOnlineExtraEditionNoGhostExtraBadge() {
        // Show has 'EXTRA' in title, but creator selected Special relation
        val saoExtra = MediaItem(
            id = "sao_extra_edition",
            title = "Sword Art Online EXTRA EDITION",
            type = "MOVIE",
            seasonNumber = 0,
            relationType = "Sequel • Special",
            franchiseId = "sword-art-online"
        )

        // Must respect creator's explicit relation 'Special', NOT auto-hijack to EXTRA from title!
        val badge = FranchiseManager.getPosterBadge(saoExtra)
        assertEquals("Badge must be SPECIAL, not ghost EXTRA", "SPECIAL", badge)
    }

    @Test
    fun testDragonBallSuperBrolyAndSuperHeroFranchiseSortingWithoutM5M7() {
        val db = MediaItem(id = "db_1", title = "Dragon Ball", type = "SERIES", seasonNumber = 1, franchiseId = "dragon-ball")
        val dbz = MediaItem(id = "db_2", title = "Dragon Ball Z", type = "SERIES", seasonNumber = 2, franchiseId = "dragon-ball")
        val dbs = MediaItem(id = "db_3", title = "Dragon Ball Super", type = "SERIES", seasonNumber = 4, franchiseId = "dragon-ball")
        val broly = MediaItem(
            id = "db_broly",
            title = "Dragon Ball Super: Broly",
            type = "MOVIE",
            seasonNumber = 1, // Super Movie 1
            franchiseOrder = 5.0, // Sort 5th in franchise timeline
            relationType = "Sequel • Movie",
            franchiseId = "dragon-ball"
        )
        val superHero = MediaItem(
            id = "db_super_hero",
            title = "Dragon Ball Super: SUPER HERO",
            type = "MOVIE",
            seasonNumber = 2, // Super Movie 2
            franchiseOrder = 7.0, // Sort 7th in franchise timeline
            relationType = "Sequel • Movie",
            franchiseId = "dragon-ball"
        )

        // 1. Badges must be M1 and M2, NEVER M5 or M7!
        assertEquals("Broly badge must be M1, never M5", "M1", FranchiseManager.getPosterBadge(broly))
        assertEquals("Super Hero badge must be M2, never M7", "M2", FranchiseManager.getPosterBadge(superHero))

        // 2. Timeline scores must properly sort them
        val brolyScore = FranchiseManager.getChronologicalScore(broly)
        val shScore = FranchiseManager.getChronologicalScore(superHero)
        assertTrue("Super Hero (Order 7) must sort after Broly (Order 5)", shScore > brolyScore)
    }
}



