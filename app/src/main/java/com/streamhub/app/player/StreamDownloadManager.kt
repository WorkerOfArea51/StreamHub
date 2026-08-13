package com.streamhub.app.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * ExoPlayer Download Manager — INTERNAL player component for stream caching.
 *
 * This is NOT the same as com.streamhub.app.data.DownloadManager (which handles
 * user-initiated episode downloads via the system DownloadManager). This class
 * manages ExoPlayer's offline DownloadManager instance for pre-caching and
 * offline playback support.
 */
@OptIn(UnstableApi::class)
object StreamDownloadManager {
    private const val TAG = "StreamDownloadManager"

    @Volatile
    private var downloadManager: DownloadManager? = null

    @Volatile
    private var downloadCache: SimpleCache? = null

    @Volatile
    private var databaseProvider: StandaloneDatabaseProvider? = null

    private var executor: ExecutorService? = null

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        if (downloadManager == null) {
            val cache = getDownloadCache(context)
            val dbProvider = getDatabaseProvider(context)
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("StreamHub/${com.streamhub.app.BuildConfig.VERSION_NAME}")
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(15_000)

            val exec = Executors.newFixedThreadPool(4).also { executor = it }

            downloadManager = DownloadManager(
                context.applicationContext,
                dbProvider,
                cache,
                dataSourceFactory,
                exec
            ).apply {
                maxParallelDownloads = 3
            }
        }
        return downloadManager!!
    }

    @Synchronized
    fun getDatabaseProvider(context: Context): StandaloneDatabaseProvider {
        if (databaseProvider == null) {
            databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
        }
        return databaseProvider!!
    }

    @Synchronized
    fun getDownloadCache(context: Context): SimpleCache {
        if (downloadCache == null) {
            val cacheDir = File(context.applicationContext.cacheDir, "exoplayer_downloads")
            val evictor = NoOpCacheEvictor()
            downloadCache = SimpleCache(cacheDir, evictor, getDatabaseProvider(context))
        }
        return downloadCache!!
    }

    /**
     * FIX #29: Release resources and shutdown thread pool on app termination.
     */
    @Synchronized
    fun release() {
        try {
            downloadManager?.release()
            downloadManager = null
            executor?.shutdown()
            if (executor?.awaitTermination(5, TimeUnit.SECONDS) == false) {
                executor?.shutdownNow()
            }
            executor = null
            downloadCache?.release()
            downloadCache = null
            databaseProvider = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing StreamDownloadManager resources", e)
        }
    }
}
