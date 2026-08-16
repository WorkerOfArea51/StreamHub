package com.streamhub.app.player

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-performance Video Scrubber Thumbnail Retriever:
 * - Caches video frame bitmaps in memory for instant seekbar previews (like YouTube/Netflix).
 * - Extracts frames asynchronously on IO dispatcher at 3-second keyframe buckets.
 */
object VideoThumbnailHelper {

    private const val TAG = "VideoThumbnailHelper"

    // 16MB LRU Cache for scrubbing preview thumbnails
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(30) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private var currentSourceUrl: String? = null
    private var retriever: MediaMetadataRetriever? = null

    suspend fun getThumbnail(sourceUrl: String, positionMs: Long): Bitmap? {
        if (sourceUrl.isBlank()) return null

        // Bucket to nearest 3-second keyframe to maximize cache hit rate during continuous scrub
        val bucketMs = (positionMs / 3000L) * 3000L
        val cacheKey = "${sourceUrl}_$bucketMs"

        val cached = memoryCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return withContext(Dispatchers.IO) {
            try {
                if (currentSourceUrl != sourceUrl || retriever == null) {
                    try {
                        retriever?.release()
                    } catch (_: Exception) {}

                    val newRetriever = MediaMetadataRetriever()
                    val file = File(sourceUrl)
                    if (file.exists()) {
                        newRetriever.setDataSource(sourceUrl)
                    } else if (sourceUrl.startsWith("http://") || sourceUrl.startsWith("https://")) {
                        newRetriever.setDataSource(sourceUrl, HashMap())
                    } else {
                        return@withContext null
                    }
                    retriever = newRetriever
                    currentSourceUrl = sourceUrl
                }

                val timeUs = bucketMs * 1000L
                val frame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                    retriever?.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        240, // 240px width for sharp lightweight thumbnail
                        135  // 135px height (16:9)
                    )
                } else {
                    val fullFrame = retriever?.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (fullFrame != null) {
                        Bitmap.createScaledBitmap(fullFrame, 240, 135, true)
                    } else null
                } ?: retriever?.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                if (frame != null) {
                    memoryCache.put(cacheKey, frame)
                }
                frame
            } catch (e: Exception) {
                Log.d(TAG, "Thumbnail extraction skipped: ${e.message}")
                null
            }
        }
    }

    fun release() {
        try {
            retriever?.release()
            retriever = null
            currentSourceUrl = null
            memoryCache.evictAll()
        } catch (_: Exception) {}
    }
}
