package com.streamhub.app

import com.streamhub.app.data.CatalogCacheManager
import com.streamhub.app.data.models.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogCacheManagerTest {

    @Test
    fun testSmartCapRetainsTrendingAndFeatured() {
        // Create 400 dummy items
        val items = (1..400).map { i ->
            MediaItem(
                id = "item_$i",
                title = "Item $i",
                category = if (i % 3 == 0) "Anime" else if (i % 3 == 1) "Movies" else "Series",
                type = if (i % 3 == 0) "Anime" else if (i % 3 == 1) "Movie" else "Series",
                isFeatured = i == 399,
                isTrending = i == 400
            )
        }

        // Invoke private smartCapCatalog via reflection
        val method = CatalogCacheManager::class.java.getDeclaredMethod("smartCapCatalog", List::class.java)
        method.isAccessible = true
        val result = method.invoke(CatalogCacheManager, items) as List<*>

        assertTrue(result.size <= 302)
        val ids = result.map { (it as MediaItem).id }.toSet()
        assertTrue(ids.contains("item_399")) // Featured must be preserved
        assertTrue(ids.contains("item_400")) // Trending must be preserved
    }

    @Test
    fun testSmartCapKeepsAllUnderThreshold() {
        val items = (1..200).map { i ->
            MediaItem(id = "item_$i", title = "Item $i", category = "Anime")
        }

        val method = CatalogCacheManager::class.java.getDeclaredMethod("smartCapCatalog", List::class.java)
        method.isAccessible = true
        val result = method.invoke(CatalogCacheManager, items) as List<*>

        assertEquals(200, result.size)
    }
}
