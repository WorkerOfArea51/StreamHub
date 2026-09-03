package com.streamhub.app.player

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object VideoThumbnailHelper {

    private const val TAG = "VideoThumbnailHelper"

    // FIX: Sized to 4MB max — safe for all Android memory classes.
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(4 * 1024) {
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
                        val isHttp = sourceUrl.startsWith("http://") || sourceUrl.startsWith("https://")

                        if (isHttp) {
                            try {
                                newRetriever.setDataSource(sourceUrl, HashMap())
                            } catch (e: Exception) {
                                Log.d(TAG, "setDataSource for HTTP URL failed: ${e.message}")
                                return@withLock null
                            }
                        } else {
                            val cleanPath = sourceUrl.removePrefix("file://")
                            val file = File(cleanPath)

                            if (!file.exists() || file.length() < 1024L) {
                                return@withLock null
                            }

                            try {
                                newRetriever.setDataSource(cleanPath)
                            } catch (e: Exception) {
                                Log.d(TAG, "setDataSource failed for local file: ${e.message}")
                                return@withLock null
                            }
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

    private val helperScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    /**
     * Non-blocking release: sets released flag first to prevent new retriever creation,
     * evicts the memory cache immediately, and releases the existing retriever asynchronously
     * on a background thread without blocking the Android UI Looper.
     */
    fun release() {
        released = true
        currentSourceUrl = null
        memoryCache.evictAll()
        helperScope.launch {
            retrieverMutex.withLock {
                try {
                    retriever?.release()
                } catch (_: Exception) {}
                retriever = null
            }
        }
    }
}
