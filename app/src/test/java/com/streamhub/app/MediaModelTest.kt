package com.streamhub.app

import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.isActivelyTrending
import com.streamhub.app.data.models.remainingTrendingDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaModelTest {

    @Test
    fun defaultMediaItem_hasEmptyDefaults() {
        val item = MediaItem()
        assertEquals("", item.id)
        assertEquals("", item.title)
        assertEquals("MOVIE", item.type)
        assertEquals("MOVIE", item.category)
        assertTrue(item.genres.isEmpty())
        assertEquals("", item.rating)
        assertFalse(item.isFeatured)
        assertFalse(item.isTrending)
    }

    @Test
    fun mediaItem_withEpisodes_maintainsIntegrity() {
        val ep1 = Episode(title = "Episode 1", episodeNumber = 1, streamUrl = "https://example.com/1.mp4")
        val ep2 = Episode(title = "Episode 2", episodeNumber = 2, streamUrl = "https://example.com/2.mp4")

        val show = MediaItem(
            id = "show_1",
            title = "Sample Series",
            type = "SERIES",
            category = "ANIME",
            episodes = listOf(ep1, ep2),
            mediaInfo = MediaInfo(resolution = "1080p", audioTracks = listOf("Japanese", "English"))
        )

        assertEquals(2, show.episodes.size)
        assertEquals("Episode 1", show.episodes[0].title)
        assertEquals("1080p", show.mediaInfo.resolution)
        assertEquals(listOf("Japanese", "English"), show.mediaInfo.audioTracks)
    }

    @Test
    fun mediaItem_trendingLifecycle_expiresStrictlyAfterSevenDays() {
        val now = 1_700_000_000_000L // arbitrary fixed epoch millis
        val sixDaysAgo = now - (6L * 24L * 60L * 60L * 1000L)
        val sevenDaysOneSecAgo = now - (7L * 24L * 60L * 60L * 1000L) - 1000L
        val eightDaysAgo = now - (8L * 24L * 60L * 60L * 1000L)

        // 1. Trending disabled: never actively trending
        val notTrending = MediaItem(isTrending = false, trendingAt = now)
        assertFalse(notTrending.isActivelyTrending(now))
        assertEquals(0, notTrending.remainingTrendingDays(now))

        // 2. Just added now: actively trending, 7 days remaining
        val freshTrending = MediaItem(isTrending = true, trendingAt = now)
        assertTrue(freshTrending.isActivelyTrending(now))
        assertEquals(7, freshTrending.remainingTrendingDays(now))

        // 3. Added 6 days ago: still actively trending, 1-2 days remaining
        val sixDaysOld = MediaItem(isTrending = true, trendingAt = sixDaysAgo)
        assertTrue(sixDaysOld.isActivelyTrending(now))
        assertEquals(1, sixDaysOld.remainingTrendingDays(now))

        // 4. Added 7 days + 1s ago: strictly expired!
        val expiredTrending = MediaItem(isTrending = true, trendingAt = sevenDaysOneSecAgo)
        assertFalse(expiredTrending.isActivelyTrending(now))
        assertEquals(0, expiredTrending.remainingTrendingDays(now))

        // 5. Fallback to updatedAt when trendingAt is 0 (legacy or edited items)
        val fallbackUpdated = MediaItem(isTrending = true, trendingAt = 0L, updatedAt = sixDaysAgo)
        assertTrue(fallbackUpdated.isActivelyTrending(now))

        val expiredFallback = MediaItem(isTrending = true, trendingAt = 0L, updatedAt = eightDaysAgo)
        assertFalse(expiredFallback.isActivelyTrending(now))

        // 6. Fallback to createdAt when trendingAt and updatedAt are 0
        val fallbackCreated = MediaItem(isTrending = true, trendingAt = 0L, updatedAt = 0L, createdAt = sixDaysAgo)
        assertTrue(fallbackCreated.isActivelyTrending(now))

        val expiredCreated = MediaItem(isTrending = true, trendingAt = 0L, updatedAt = 0L, createdAt = eightDaysAgo)
        assertFalse(expiredCreated.isActivelyTrending(now))
    }
}
