package com.streamhub.app

import android.app.Application
import android.util.Log
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.AppUpdateManager
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.NotificationAlertManager
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.data.SubtitleSettingsManager
import com.streamhub.app.data.UserStatsManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.YoutubeStreamExtractor
import com.streamhub.app.data.telegram.TelegramAuthManager
import com.streamhub.app.data.telegram.TdLibManager
import com.streamhub.app.data.telegram.TelegramProxyManager
import com.streamhub.app.player.StreamCacheManager
import com.streamhub.app.player.StreamDownloadManager
import com.streamhub.app.ui.theme.ThemeManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StreamHubApplication : Application() {

    companion object {
        private const val TAG = "StreamHubApplication"
    }

    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var lastForegroundTimeMs: Long = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()
        runCatching {
            com.google.firebase.FirebaseApp.initializeApp(this)
            Log.d(TAG, "FirebaseApp successfully initialized")
        }.onFailure { Log.e(TAG, "FirebaseApp.initializeApp failed", it) }

        if (BuildConfig.DEBUG) {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
        initializeManagers()
        startBackgroundServices()

        // FIX: Track foreground/background transitions WITHOUT releasing caches on every stop.
        // StreamDownloadManager / StreamCacheManager are kept warm so video resumes instantly.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                lastForegroundTimeMs = System.currentTimeMillis()
                Log.d(TAG, "App returned to foreground — caches preserved")
                runCatching { DownloadManager.resumeProgressPolling() }
                runCatching { StreamDownloadManager.resumeDownloads(this@StreamHubApplication) }
            }

            override fun onStop(owner: LifecycleOwner) {
                val backgroundDuration = System.currentTimeMillis() - lastForegroundTimeMs
                Log.d(TAG, "App moved to background (foreground lasted ${backgroundDuration}ms)")
                // FIX: Only pause download progress and active downloads, do NOT wipe media caches.
                runCatching { DownloadManager.pauseProgressPolling() }
                runCatching { StreamDownloadManager.pauseDownloads() }
            }
        })
    }

    /**
     * Public entry-point for explicit cache flush — called ONLY from the user-initiated
     * "Clear Cache" button in StorageManagementScreen, never automatically on app switch.
     */
    fun performEmergencyCacheFlush() {
        Log.w(TAG, "Emergency cache flush invoked by user/system pressure")
        runCatching { StreamDownloadManager.release() }
        runCatching { StreamCacheManager.release() }
        runCatching { DownloadManager.pauseProgressPolling() }
    }

    private fun initializeManagers() {
        runCatching { PlayerSettingsManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "PlayerSettingsManager.init failed", it) }
        runCatching { AdminManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "AdminManager.init failed", it) }
        runCatching { ThemeManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "ThemeManager.init failed", it) }
        runCatching { MyListManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "MyListManager.init failed", it) }
        runCatching { WatchHistoryManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "WatchHistoryManager.init failed", it) }
        runCatching { SubtitleSettingsManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "SubtitleSettingsManager.init failed", it) }
        runCatching { HomeScreenLayoutManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "HomeScreenLayoutManager.init failed", it) }
        runCatching { DownloadManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "DownloadManager.init failed", it) }
        runCatching { UserStatsManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "UserStatsManager.init failed", it) }
        runCatching { YoutubeStreamExtractor.init(applicationContext) }
            .onFailure { Log.e(TAG, "YoutubeStreamExtractor.init failed", it) }
        runCatching { StorageCacheManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "StorageCacheManager.init failed", it) }

        initScope.launch {
            val tdLibOk = runCatching { TdLibManager.initialize(applicationContext) }
                .getOrDefault(false)
            if (tdLibOk) {
                runCatching { TelegramAuthManager.init(applicationContext) }
                    .onFailure { Log.e(TAG, "TelegramAuthManager.init failed", it) }
                runCatching { TelegramProxyManager.init(applicationContext) }
                    .onFailure { Log.e(TAG, "TelegramProxyManager.init failed", it) }
            } else {
                Log.w(TAG, "TDLib initialization failed — Telegram features will be unavailable")
            }
        }
    }

    private fun startBackgroundServices() {
        runCatching {
            AppUpdateManager.checkForUpdate(
                currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
                currentVersionName = BuildConfig.VERSION_NAME
            )
        }.onFailure { Log.e(TAG, "AppUpdateManager.checkForUpdate failed", it) }
    }
}
