package com.streamhub.app.data

import android.content.Context
import android.util.Log
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 120Hz Zero-Jank Coil Image & Thumbnail Prefetching Manager:
 * - Asynchronously pre-warms Coil's MemoryCache and DiskCache with poster and banner images
 * - Eliminates placeholder pop-in during rapid 120Hz flings and category switches
 */
object ThumbnailPrefetchManager {

    private const val TAG = "ThumbnailPrefetch"
    private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefetchedUrls = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * Prefetches top media item posters and banners from the catalog.
     */
    fun prefetchCatalog(context: Context, items: List<MediaItem>, limit: Int = 30) {
        if (items.isEmpty()) return
        val appContext = context.applicationContext

        val urlsToPrefetch = items.take(limit).flatMap { media ->
            listOfNotNull(
                media.posterUrl.takeIf { it.isNotBlank() },
                media.bannerUrl.takeIf { it.isNotBlank() },
                media.episodes.firstOrNull()?.thumbnailUrl?.takeIf { it.isNotBlank() }
            )
        }.distinct()

        prefetchUrls(appContext, urlsToPrefetch)
    }

    /**
     * Prefetches a list of image URLs asynchronously into the Coil image cache.
     */
    fun prefetchUrls(context: Context, urls: List<String>) {
        if (urls.isEmpty()) return
        val appContext = context.applicationContext

        prefetchScope.launch {
            val imageLoader = Coil.imageLoader(appContext)
            var count = 0

            for (url in urls) {
                if (url.isBlank() || prefetchedUrls.contains(url)) continue
                prefetchedUrls.add(url)

                try {
                    val request = ImageRequest.Builder(appContext)
                        .data(url)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()

                    imageLoader.enqueue(request)
                    count++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to enqueue prefetch for $url", e)
                }
            }

            if (count > 0) {
                Log.d(TAG, "Prefetched $count images into Coil cache")
            }
        }
    }

    /**
     * Clears prefetched URL history on low memory.
     */
    fun clearHistory() {
        prefetchedUrls.clear()
    }
}
