package com.streamhub.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import java.io.File
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
object StreamDownloadManager {
    private var downloadManager: DownloadManager? = null
    private var downloadCache: SimpleCache? = null

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        if (downloadManager == null) {
            val databaseProvider = StandaloneDatabaseProvider(context)
            val cache = getDownloadCache(context)
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            val executor = Executors.newFixedThreadPool(4)

            downloadManager = DownloadManager(
                context,
                databaseProvider,
                cache,
                httpDataSourceFactory,
                executor
            )
        }
        return downloadManager!!
    }

    @Synchronized
    fun getDownloadCache(context: Context): SimpleCache {
        if (downloadCache == null) {
            val downloadContentDirectory = File(context.getExternalFilesDir(null), "offline_downloads")
            val databaseProvider = StandaloneDatabaseProvider(context)
            downloadCache = SimpleCache(downloadContentDirectory, NoOpCacheEvictor(), databaseProvider)
        }
        return downloadCache!!
    }
}
