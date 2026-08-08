package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlayerSettings(
    val skipIntroSeconds: Int = 90,
    val nextEpisodeThresholdSeconds: Int = 45, // 65s, 45s, 40s, 30s, 0 (Disabled)
    val autoPlayNextEpisode: Boolean = true,
    val volumeOnRight: Boolean = true // Volume on right (Anim on left) vs Volume on left (Anim on right)
)

/**
 * Persists player preferences (skip intro duration, next-ep threshold, etc.)
 * in SharedPreferences.
 *
 * Initialized once by StreamHubApplication.onCreate(). Callers do NOT pass
 * context to any method.
 */
object PlayerSettingsManager {

    private const val TAG = "PlayerSettingsManager"
    private const val PREFS_NAME = "streamhub_player_settings"
    private const val KEY_SKIP_INTRO = "skip_intro_sec"
    const val KEY_NEXT_EPISODE_THRESHOLD = "next_ep_threshold_sec"
    private const val KEY_AUTO_PLAY = "auto_play_next"
    private const val KEY_VOLUME_ON_RIGHT = "volume_on_right"

    private lateinit var appContext: Context

    private val _settingsFlow = MutableStateFlow(PlayerSettings())
    val settingsFlow: StateFlow<PlayerSettings> = _settingsFlow.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val prefs = getPrefs()
        try {
            _settingsFlow.value = PlayerSettings(
                skipIntroSeconds = prefs.getInt(KEY_SKIP_INTRO, 90),
                nextEpisodeThresholdSeconds = prefs.getInt(KEY_NEXT_EPISODE_THRESHOLD, 45),
                autoPlayNextEpisode = prefs.getBoolean(KEY_AUTO_PLAY, true),
                volumeOnRight = prefs.getBoolean(KEY_VOLUME_ON_RIGHT, true)
            )
        } catch (e: Exception) {
            prefs.edit().clear().apply()
            _settingsFlow.value = PlayerSettings()
        }
    }

    @Synchronized
    fun updateSkipIntro(seconds: Int) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateSkipIntro called before init — no-op")
            return
        }
        _settingsFlow.update { it.copy(skipIntroSeconds = seconds) }
        getPrefs().edit().putInt(KEY_SKIP_INTRO, seconds).apply()
    }

    @Synchronized
    fun updateNextEpisodeThreshold(seconds: Int) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateNextEpisodeThreshold called before init — no-op")
            return
        }
        _settingsFlow.update { it.copy(nextEpisodeThresholdSeconds = seconds) }
        getPrefs().edit().putInt(KEY_NEXT_EPISODE_THRESHOLD, seconds).apply()
    }

    @Synchronized
    fun updateAutoPlayNext(autoPlay: Boolean) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateAutoPlayNext called before init — no-op")
            return
        }
        _settingsFlow.update { it.copy(autoPlayNextEpisode = autoPlay) }
        getPrefs().edit().putBoolean(KEY_AUTO_PLAY, autoPlay).apply()
    }

    @Synchronized
    fun updateVolumeSide(volumeOnRight: Boolean) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateVolumeSide called before init — no-op")
            return
        }
        _settingsFlow.update { it.copy(volumeOnRight = volumeOnRight) }
        getPrefs().edit().putBoolean(KEY_VOLUME_ON_RIGHT, volumeOnRight).apply()
    }

    private fun getPrefs(): SharedPreferences {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
