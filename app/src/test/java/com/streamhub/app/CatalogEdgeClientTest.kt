package com.streamhub.app

import com.streamhub.app.data.api.CatalogEdgeClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CatalogEdgeClientTest {

    @Test
    fun testParseCatalogJsonDirectArray() {
        val json = """
            [
                {
                    "id": "pok_mon_1997",
                    "title": "Pokémon",
                    "category": "Anime",
                    "type": "Anime",
                    "episodes": [
                        {
                            "id": "ep1",
                            "title": "Pokémon - I Choose You!",
                            "episodeNumber": 1,
                            "seasonNumber": 1,
                            "arcName": "Indigo League",
                            "streamUrl": "https://midnighthawk.serv00.net/stream/abc"
                        }
                    ]
                }
            ]
        """.trimIndent()

        // Test via reflection or direct call
        val method = CatalogEdgeClient::class.java.getDeclaredMethod("parseCatalogJson", String::class.java)
        method.isAccessible = true
        val result = method.invoke(CatalogEdgeClient, json) as List<*>

        assertEquals(1, result.size)
        val item = result[0] as com.streamhub.app.data.models.MediaItem
        assertEquals("pok_mon_1997", item.id)
        assertEquals("Pokémon", item.title)
        assertEquals(1, item.episodes.size)
        assertEquals("Indigo League", item.episodes[0].arcName)
    }

    @Test
    fun testParseCatalogJsonObjectWrapper() {
        val json = """
            {
                "success": true,
                "count": 1,
                "items": [
                    {
                        "id": "movie_1",
                        "title": "Inception",
                        "category": "Movies",
                        "type": "Movie",
                        "episodes": []
                    }
                ]
            }
        """.trimIndent()

        val method = CatalogEdgeClient::class.java.getDeclaredMethod("parseCatalogJson", String::class.java)
        method.isAccessible = true
        val result = method.invoke(CatalogEdgeClient, json) as List<*>

        assertEquals(1, result.size)
        val item = result[0] as com.streamhub.app.data.models.MediaItem
        assertEquals("movie_1", item.id)
        assertEquals("Inception", item.title)
    }
}
