package com.streamhub.app

import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.data.models.MediaItem
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
}
