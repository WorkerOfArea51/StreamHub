package com.streamhub.app

import android.app.Application
import android.util.Log
import com.streamhub.app.data.AppUpdateManager
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.NotificationAlertManager
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.data.SubtitleSettingsManager
import com.streamhub.app.data.UserStatsManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.telegram.TelegramAuthManager
import com.streamhub.app.data.telegram.TelegramProxyManager
import com.streamhub.app.ui.theme.ThemeManager

/**
 * Custom Application class — the single source of truth for app initialization.
 *
 * Created by the system before any Activity. Its onCreate() runs exactly once
 * per process. All singleton managers are initialized here with applicationContext,
 * so by the time any Composable is rendered, every StateFlow is already populated.
 *
 * Contract: screens must NOT call Manager.init(context) themselves. The managers
 * are guaranteed ready before any UI code runs.
 *
 * To add a new manager:
 *   1. Add its init() call in initializeManagers() in the correct order
 *   2. Remove any LaunchedEffect { Manager.init(context) } from screens
 *   3. Document the dependency order in the KDoc below
 *
 * Dependency order:
 *   Layer 1 — Core preferences (no dependencies):
 *     PlayerSettingsManager, ThemeManager, MyListManager, WatchHistoryManager,
 *     SubtitleSettingsManager, HomeScreenLayoutManager
 *   Layer 2 — Data managers (depend on SharedPreferences from Layer 1):
 *     UserStatsManager, NotificationAlertManager
 *   Layer 3 — Network managers (depend on Secrets, which is BuildConfig — always ready):
 *     TelegramAuthManager, TelegramProxyManager
 *   Layer 4 — Background services (depend on Layer 3 being initialized):
 *     AppUpdateManager.checkForUpdate()
 */
class StreamHubApplication : Application() {

    companion object {
        private const val TAG = "StreamHubApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate — initializing managers")
        initializeManagers()
        startBackgroundServices()
    }

    /**
     * Initialize all singleton managers in dependency order.
     *
     * If any init throws, we log but do NOT crash — the app should still launch
     * with degraded functionality (empty StateFlows) rather than brick entirely.
     * The manager itself is responsible for catching its own internal errors
     * (e.g. corrupted JSON in prefs) and emitting an empty StateFlow as fallback.
     */
    private fun initializeManagers() {
        // Layer 1 — Core preferences (no cross-dependencies)
        runCatching { PlayerSettingsManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "PlayerSettingsManager.init failed", it) }

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

        // Layer 2 — Data managers
        runCatching { UserStatsManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "UserStatsManager.init failed", it) }

        runCatching { NotificationAlertManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "NotificationAlertManager.init failed", it) }

        // Layer 3 — Network managers
        runCatching { TelegramAuthManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "TelegramAuthManager.init failed", it) }

        runCatching { TelegramProxyManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "TelegramProxyManager.init failed", it) }
    }

    /**
     * Start background services that should run from app launch.
     *
     * Separated from manager init so that a failure here doesn't block
     * the UI from rendering. These are fire-and-forget coroutines.
     */
    private fun startBackgroundServices() {
        runCatching {
            AppUpdateManager.checkForUpdate(
                currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
                currentVersionName = BuildConfig.VERSION_NAME
            )
        }.onFailure { Log.e(TAG, "AppUpdateManager.checkForUpdate failed", it) }
    }
}
