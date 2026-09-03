package com.streamhub.app

import com.streamhub.app.data.EpisodeOrderingManager
import com.streamhub.app.data.models.Episode
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeOrderingManagerTest {

    @Test
    fun extractAbsoluteEpisodeNumber_detectsBleach356() {
        val title = "EP 356 Foe or Friend! Ginjō's Unseen Heart!"
        val epNum = EpisodeOrderingManager.extractAbsoluteEpisodeNumber(title)
        assertEquals(356, epNum)
    }

    @Test
    fun extractAbsoluteEpisodeNumber_handlesDualMarkers_prefersAbsolute() {
        val title = "EP - 14 - EP 356 Foe or Friend! Ginjō's Unseen Heart!"
        val epNum = EpisodeOrderingManager.extractAbsoluteEpisodeNumber(title)
        assertEquals(356, epNum)
    }

    @Test
    fun resolveEffectiveEpisode_promotesArcScoped14ToAbsolute356() {
        val corrupted = Episode(
            episodeNumber = 14,
            seasonNumber = 1,
            arcName = "Lost Agent Arc",
            title = "EP 356 Foe or Friend! Ginjō's Unseen Heart!"
        )
        val resolved = EpisodeOrderingManager.resolveEffectiveEpisode(corrupted)
        assertEquals(356, resolved.episodeNumber)
        assertEquals("Lost Agent Arc", resolved.arcName)
    }

    @Test
    fun normalizeAndSort_fixesInterleavedBleachQueue() {
        val ep13 = Episode(episodeNumber = 13, seasonNumber = 1, arcName = "Agent of the Shinigami Arc", title = "EP - 13 - Flower and Hollow")
        val ep14Shinigami = Episode(episodeNumber = 14, seasonNumber = 1, arcName = "Agent of the Shinigami Arc", title = "EP - 14 - Back to Back, a Fight to the Death!")
        val ep15Shinigami = Episode(episodeNumber = 15, seasonNumber = 1, arcName = "Agent of the Shinigami Arc", title = "EP - 15 - Kon's Great Plan")
        
        // This episode was mistakenly uploaded with episodeNumber = 14 in the Lost Agent Arc
        val ep356LostAgent = Episode(episodeNumber = 14, seasonNumber = 1, arcName = "Lost Agent Arc", title = "EP 356 Foe or Friend! Ginjō's Unseen Heart!")

        // Previously, sorting by { it.seasonNumber }, { it.episodeNumber } placed ep356 right next to ep14Shinigami!
        val input = listOf(ep13, ep356LostAgent, ep14Shinigami, ep15Shinigami)
        val sorted = EpisodeOrderingManager.normalizeAndSort(input)

        // Verify correct chronological order
        assertEquals(4, sorted.size)
        assertEquals("EP - 13 - Flower and Hollow", sorted[0].title)
        assertEquals(13, sorted[0].episodeNumber)

        assertEquals("EP - 14 - Back to Back, a Fight to the Death!", sorted[1].title)
        assertEquals(14, sorted[1].episodeNumber)

        assertEquals("EP - 15 - Kon's Great Plan", sorted[2].title)
        assertEquals(15, sorted[2].episodeNumber)

        assertEquals("EP 356 Foe or Friend! Ginjō's Unseen Heart!", sorted[3].title)
        assertEquals(356, sorted[3].episodeNumber)
    }

    @Test
    fun normalizeAndSort_preservesArcContinuity_whenArcEpisodesAreSequentiallyNumbered() {
        // Suppose Arc 1 has episodes 1..3 and Arc 2 has episodes 1..3
        val arc1Ep1 = Episode(episodeNumber = 1, seasonNumber = 1, arcName = "Arc 1", title = "Episode 1")
        val arc1Ep2 = Episode(episodeNumber = 2, seasonNumber = 1, arcName = "Arc 1", title = "Episode 2")
        val arc1Ep3 = Episode(episodeNumber = 3, seasonNumber = 1, arcName = "Arc 1", title = "Episode 3")

        val arc2Ep1 = Episode(episodeNumber = 1, seasonNumber = 1, arcName = "Arc 2", title = "Episode 1")
        val arc2Ep2 = Episode(episodeNumber = 2, seasonNumber = 1, arcName = "Arc 2", title = "Episode 2")
        val arc2Ep3 = Episode(episodeNumber = 3, seasonNumber = 1, arcName = "Arc 2", title = "Episode 3")

        val input = listOf(arc1Ep1, arc2Ep1, arc1Ep2, arc2Ep2, arc1Ep3, arc2Ep3)
        val sorted = EpisodeOrderingManager.normalizeAndSort(input, explicitArcOrder = listOf("Arc 1", "Arc 2"))

        // Must NOT be interleaved! All of Arc 1 must precede Arc 2
        val expectedArcs = listOf("Arc 1", "Arc 1", "Arc 1", "Arc 2", "Arc 2", "Arc 2")
        assertEquals(expectedArcs, sorted.map { it.arcName })
    }
}
