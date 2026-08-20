package com.streamhub.app.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
object StreamDownloadManager {
    private const val TAG = "StreamDownloadManager"
    private const val DOWNLOAD_CACHE_SIZE_BYTES = 2L * 1024 * 1024 * 1024 // 2 GB

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

            // FIX: Use a dedicated thread pool with DiscardOldestPolicy so caller thread is NEVER blocked.
            // ExoPlayer calls into this from its load thread — CallerRunsPolicy would freeze playback.
            val exec = ThreadPoolExecutor(
                4, 4, 60L, TimeUnit.SECONDS,
                LinkedBlockingQueue(32),
                { r ->
                    val t = Thread(r, "StreamHub-Downloader-${System.nanoTime()}")
                    t.isDaemon = true
                    t
                },
                ThreadPoolExecutor.DiscardOldestPolicy()
            ).also { executor = it }

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
            val evictor = LeastRecentlyUsedCacheEvictor(DOWNLOAD_CACHE_SIZE_BYTES)
            downloadCache = SimpleCache(cacheDir, evictor, getDatabaseProvider(context))
        }
        return downloadCache!!
    }

    /**
     * FIX: pauseDownloads() — pauses active downloads without destroying the cache.
     * Called on app background instead of release().
     */
    @Synchronized
    fun pauseDownloads() {
        try {
            downloadManager?.pauseDownloads()
            Log.i(TAG, "All downloads paused (cache preserved)")
        } catch (e: Exception) {
            Log.w(TAG, "pauseDownloads failed: ${e.message}")
        }
    }

    /**
     * FIX: resumeDownloads() — called when app returns to foreground.
     */
    @Synchronized
    fun resumeDownloads(context: Context) {
        try {
            val dm = downloadManager ?: getDownloadManager(context)
            dm.resumeDownloads()
            Log.i(TAG, "All downloads resumed")
        } catch (e: Exception) {
            Log.w(TAG, "resumeDownloads failed: ${e.message}")
        }
    }

    /**
     * Full teardown — call ONLY from Application.onTerminate() or process death handler.
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
