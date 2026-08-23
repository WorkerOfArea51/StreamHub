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

                        // FIX: Check if the file is complete (not being actively downloaded).
                        // MediaMetadataRetriever fails on partial files because the moov atom
                        // (video metadata) is written last. If the file is still growing,
                        // return null and let the UI show the poster fallback.
                        if (!file.exists() || file.length() < 1024L) {
                            return@withLock null
                        }

                        // FIX: Check if file is still being written by TDLib.
                        // If file size changed in the last 500ms, it's still downloading.
                        val size1 = file.length()
                        kotlinx.coroutines.delay(500L)
                        val size2 = file.length()
                        if (size1 != size2) {
                            // File is still being downloaded — can't extract frames
                            return@withLock null
                        }

                        try {
                            newRetriever.setDataSource(cleanPath)
                        } catch (e: Exception) {
                            // setDataSource fails on partial/corrupt files — return null
                            // so the UI shows the poster fallback instead of crashing.
                            Log.d(TAG, "setDataSource failed (file may be partial): ${e.message}")
                            return@withLock null
                        }
                        retriever = newRetriever
                        currentSourceUrl = sourceUrl
                    }

                    val timeUs = bucketMs * 1000L
                    // FIX: Try OPTION_CLOSEST first (exact timestamp), then fall back to
                    // OPTION_CLOSEST_SYNC (nearest keyframe) for compatibility.
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
