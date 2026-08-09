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

/**
 * ExoPlayer Download Manager — INTERNAL player component for stream caching.
 *
 * This is NOT the same as com.streamhub.app.data.DownloadManager (which handles
 * user-initiated episode downloads via the system DownloadManager). This class
 * manages ExoPlayer's built-in download/caching service for progressive downloads
 * and stream buffering. It is used internally by the player and should not be
 * called directly from UI code.
 */
@OptIn(UnstableApi::class)
object StreamDownloadManager {
    private const val TAG = "StreamDownloadManager"
    private var downloadManager: DownloadManager? = null
    private var downloadCache: SimpleCache? = null
    // FIX #28: Single StandaloneDatabaseProvider singleton
    private var databaseProvider: StandaloneDatabaseProvider? = null
    // FIX #29: Store ExecutorService reference for clean shutdown
    private var executor: ExecutorService? = null

    @Synchronized
    private fun getDatabaseProvider(context: Context): StandaloneDatabaseProvider {
        if (databaseProvider == null) {
            databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
        }
        return databaseProvider!!
    }

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        if (downloadManager == null) {
            val dbProvider = getDatabaseProvider(context)
            val cache = getDownloadCache(context)
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            if (executor == null) {
                executor = Executors.newFixedThreadPool(4)
            }

            downloadManager = DownloadManager(
                context.applicationContext,
                dbProvider,
                cache,
                httpDataSourceFactory,
                executor!!
            )
        }
        return downloadManager!!
    }

    @Synchronized
    fun getDownloadCache(context: Context): SimpleCache {
        if (downloadCache == null) {
            // FIX #3: Null check external storage directory with internal storage fallback
            val externalDir = context.getExternalFilesDir(null)
            val downloadContentDirectory = if (externalDir != null) {
                File(externalDir, "offline_downloads")
            } else {
                File(context.filesDir, "offline_downloads")
            }

            val dbProvider = getDatabaseProvider(context)
            val evictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(
                2L * 1024L * 1024L * 1024L
            )
            downloadCache = SimpleCache(downloadContentDirectory, evictor, dbProvider)
        }
        return downloadCache!!
    }

    /**
     * FIX #29: Release resources and shutdown thread pool on app termination.
     */
    @Synchronized
    fun release() {
        try {
            executor?.shutdown()
            executor = null
            downloadCache?.release()
            downloadCache = null
            downloadManager = null
            databaseProvider = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing StreamDownloadManager resources", e)
        }
    }
}
