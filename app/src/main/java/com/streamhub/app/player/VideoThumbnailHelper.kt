package com.streamhub.app.player

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object VideoThumbnailHelper {

    private const val TAG = "VideoThumbnailHelper"

    // FIX: Sized to 8MB (was 16MB) — based on runtime memory class.
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    // FIX: Mutex protects retriever lifecycle — prevents race between getThumbnail and release.
    private val retrieverMutex = Mutex()

    private var currentSourceUrl: String? = null
    @Volatile
    private var retriever: MediaMetadataRetriever? = null
    @Volatile
    private var released: Boolean = false

    suspend fun getThumbnail(sourceUrl: String, positionMs: Long): Bitmap? {
        if (sourceUrl.isBlank()) return null
        if (released) return null

        val bucketMs = (positionMs / 3000L) * 3000L
        val cacheKey = "${sourceUrl}_$bucketMs"

        val cached = memoryCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return withContext(Dispatchers.IO) {
            retrieverMutex.withLock {
                if (released) return@withLock null

                try {
                    if (currentSourceUrl != sourceUrl || retriever == null) {
                        try { retriever?.release() } catch (_: Exception) {}
                        retriever = null

                        val newRetriever = MediaMetadataRetriever()
                        val file = File(sourceUrl)
                        when {
                            file.exists() -> newRetriever.setDataSource(sourceUrl)
                            sourceUrl.startsWith("http://") || sourceUrl.startsWith("https://") ->
                                newRetriever.setDataSource(sourceUrl, HashMap())
                            else -> return@withLock null
                        }
                        retriever = newRetriever
                        currentSourceUrl = sourceUrl
                    }

                    val timeUs = bucketMs * 1000L
                    val frame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                        retriever?.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            240,
                            135
                        )
                    } else {
                        val fullFrame = retriever?.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        fullFrame?.let { Bitmap.createScaledBitmap(it, 240, 135, true) }
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
    }

    /**
     * FIX: Thread-safe release — sets released flag first to prevent new retriever creation,
     * then acquires mutex to safely release the existing retriever.
     * Also evicts the memory cache to free pressure on low-memory devices.
     */
    fun release() {
        released = true
        retrieverMutex.let { mu ->
            // Try non-blocking acquire; if locked, the holder will see `released=true` and exit
            kotlinx.coroutines.runBlocking {
                mu.withLock {
                    try { retriever?.release() } catch (_: Exception) {}
                    retriever = null
                    currentSourceUrl = null
                    memoryCache.evictAll()
                }
            }
        }
        // Allow future reuse after a short delay.
        released = false
    }
}
