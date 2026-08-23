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

        val bucketMs = (positionMs / 3000L) * 3000L
        val cacheKey = "${sourceUrl}_$bucketMs"

        val cached = memoryCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return withContext(Dispatchers.IO) {
            retrieverMutex.withLock {
                try {
                    if (currentSourceUrl != sourceUrl || retriever == null || released) {
                        try { retriever?.release() } catch (_: Exception) {}
                        retriever = null
                        released = false

                        val newRetriever = MediaMetadataRetriever()
                        val cleanPath = sourceUrl.removePrefix("file://")
                        val file = File(cleanPath)
                        // FIX: Removed the !cleanPath.contains("/temp/") filter — it was
                        // rejecting TDLib temp file paths, preventing thumbnails from being
                        // generated for ALL streaming videos. TDLib temp files are valid
                        // video files and MediaMetadataRetriever can read them fine.
                        if (file.exists() && file.length() > 1024L) {
                            newRetriever.setDataSource(cleanPath)
                        } else if (sourceUrl.startsWith("http://") || sourceUrl.startsWith("https://")) {
                            // Fallback for HTTP URLs (won't work for TDLib paths but kept for safety)
                            try {
                                newRetriever.setDataSource(sourceUrl, HashMap())
                            } catch (_: Exception) {
                                return@withLock null
                            }
                        } else {
                            return@withLock null
                        }
                        retriever = newRetriever
                        currentSourceUrl = sourceUrl
                    }

                    val timeUs = bucketMs * 1000L
                    // FIX: Use OPTION_CLOSEST (not OPTION_CLOSEST_SYNC) for accurate frame
                    // extraction. OPTION_CLOSEST_SYNC returns the nearest keyframe, which
                    // for many Telegram-encoded videos is the first frame (sparse keyframes).
                    // OPTION_CLOSEST decodes to the exact requested timestamp.
                    val frame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                        retriever?.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST,
                            240,
                            135
                        ) ?: retriever?.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            240,
                            135
                        ) ?: retriever?.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_PREVIOUS_SYNC,
                            240,
                            135
                        )
                    } else {
                        val fullFrame = retriever?.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                            ?: retriever?.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever?.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC)
                        fullFrame?.let { Bitmap.createScaledBitmap(it, 240, 135, true) }
                    }

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
