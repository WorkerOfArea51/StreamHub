package com.streamhub.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.StreamInfo

/**
 * High-performance, native YouTube Stream Extractor for StreamHub.
 * Powered by NewPipeExtractor — extracting direct HLS, DASH, and progressive streams
 * to bypass YouTube embed restrictions (Error 152) and provide ad-free native ExoPlayer playback.
 */
object YoutubeStreamExtractor {

    private const val TAG = "YoutubeStreamExtractor"
    private var isInitialized = false

    /**
     * Initializes NewPipeExtractor with our custom OkHttp downloader.
     */
    fun init(context: Context? = null) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            try {
                NewPipe.init(
                    NewPipeDownloader.getInstance(),
                    Localization("en", "US")
                )
                isInitialized = true
                Log.d(TAG, "NewPipeExtractor initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize NewPipeExtractor", e)
            }
        }
    }

    /**
     * Extracts direct playable stream URL for a given YouTube video ID.
     * Priority:
     *  1. HLS Stream (Best for adaptive streaming in ExoPlayer)
     *  2. DASH MPD Stream
     *  3. Progressive MP4 Stream (Combined video + audio)
     *  4. Video-only stream (ExoPlayer playback)
     */
    suspend fun extractStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            init()
        }

        val cleanVideoId = videoId.trim()
        if (cleanVideoId.isBlank()) return@withContext null

        Log.d(TAG, "Extracting YouTube stream for videoId: $cleanVideoId")
        val watchUrl = "https://www.youtube.com/watch?v=$cleanVideoId"

        try {
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, watchUrl)

            // Priority 1: HLS Stream
            val hls = streamInfo.hlsUrl
            if (!hls.isNullOrBlank()) {
                Log.d(TAG, "Found HLS stream URL: $hls")
                return@withContext hls
            }

            // Priority 2: DASH MPD Manifest
            val dash = streamInfo.dashMpdUrl
            if (!dash.isNullOrBlank()) {
                Log.d(TAG, "Found DASH MPD stream URL: $dash")
                return@withContext dash
            }

            // Priority 3: Combined Progressive Streams (Audio + Video)
            val progressive = streamInfo.videoStreams
                ?.filter { !it.isVideoOnly }
                ?.maxByOrNull { it.height }

            if (progressive != null && !progressive.url.isNullOrBlank()) {
                Log.d(TAG, "Found progressive MP4 stream: ${progressive.height}p, url=${progressive.url}")
                return@withContext progressive.url
            }

            // Priority 4: Video stream from videoStreams or videoOnlyStreams
            val videoStream = streamInfo.videoStreams?.maxByOrNull { it.height }
                ?: streamInfo.videoOnlyStreams?.maxByOrNull { it.height }

            if (videoStream != null && !videoStream.url.isNullOrBlank()) {
                Log.d(TAG, "Found video stream fallback: ${videoStream.height}p, url=${videoStream.url}")
                return@withContext videoStream.url
            }

            Log.w(TAG, "No streams found in StreamInfo for videoId: $cleanVideoId")
            null
        } catch (e: Exception) {
            Log.e(TAG, "NewPipe extraction failed for videoId: $cleanVideoId", e)
            null
        }
    }
}
