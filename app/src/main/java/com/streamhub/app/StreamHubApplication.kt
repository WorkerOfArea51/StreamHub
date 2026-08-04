package com.streamhub.app

import android.app.Application
import android.util.Log
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.data.WatchHistoryManager
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
 */
class StreamHubApplication : Application() {

    companion object {
        private const val TAG = "StreamHubApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate — initializing managers")
        initializeManagers()
    }

    /**
     * Initialize all singleton managers in dependency order.
     *
     * Order matters:
     *   1. PlayerSettingsManager — no dependencies
     *   2. ThemeManager — no dependencies (UI layer, but needed by SettingsScreen)
     *   3. MyListManager — no dependencies
     *   4. WatchHistoryManager — no dependencies, but conceptually depends on
     *      PlayerSettingsManager existing (the player that writes progress
     *      reads its config from PlayerSettingsManager)
     *
     * If any init throws, we log but do NOT crash — the app should still launch
     * with degraded functionality (empty StateFlows) rather than brick entirely.
     * The manager itself is responsible for catching its own internal errors
     * (e.g. corrupted JSON in prefs) and emitting an empty StateFlow as fallback.
     */
    private fun initializeManagers() {
        runCatching { PlayerSettingsManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "PlayerSettingsManager.init failed", it) }

        runCatching { ThemeManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "ThemeManager.init failed", it) }

        runCatching { MyListManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "MyListManager.init failed", it) }

        runCatching { WatchHistoryManager.init(applicationContext) }
            .onFailure { Log.e(TAG, "WatchHistoryManager.init failed", it) }
    }
}
