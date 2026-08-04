package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerSettings(
    val skipIntroSeconds: Int = 90,
    val nextEpisodeThresholdSeconds: Int = 45, // 65s, 45s, 40s, 30s, 0 (Disabled)
    val autoPlayNextEpisode: Boolean = true,
    val volumeOnRight: Boolean = true // Volume on right (Anim on left) vs Volume on left (Anim on right)
)

object PlayerSettingsManager {
    private const val PREFS_NAME = "streamhub_player_settings"
    private const val KEY_SKIP_INTRO = "skip_intro_sec"
    const val KEY_NEXT_EPISODE_THRESHOLD = "next_ep_threshold_sec"
    private const val KEY_AUTO_PLAY = "auto_play_next"
    private const val KEY_VOLUME_ON_RIGHT = "volume_on_right"

    private val _settingsFlow = MutableStateFlow(PlayerSettings())
    val settingsFlow: StateFlow<PlayerSettings> = _settingsFlow.asStateFlow()

    fun init(context: Context) {
        val prefs = getPrefs(context)
        _settingsFlow.value = PlayerSettings(
            skipIntroSeconds = prefs.getInt(KEY_SKIP_INTRO, 90),
            nextEpisodeThresholdSeconds = prefs.getInt(KEY_NEXT_EPISODE_THRESHOLD, 45),
            autoPlayNextEpisode = prefs.getBoolean(KEY_AUTO_PLAY, true),
            volumeOnRight = prefs.getBoolean(KEY_VOLUME_ON_RIGHT, true)
        )
    }

    fun updateSkipIntro(context: Context, seconds: Int) {
        _settingsFlow.value = _settingsFlow.value.copy(skipIntroSeconds = seconds)
        getPrefs(context).edit().putInt(KEY_SKIP_INTRO, seconds).apply()
    }

    fun updateNextEpisodeThreshold(context: Context, seconds: Int) {
        _settingsFlow.value = _settingsFlow.value.copy(nextEpisodeThresholdSeconds = seconds)
        getPrefs(context).edit().putInt(KEY_NEXT_EPISODE_THRESHOLD, seconds).apply()
    }

    fun updateAutoPlayNext(context: Context, autoPlay: Boolean) {
        _settingsFlow.value = _settingsFlow.value.copy(autoPlayNextEpisode = autoPlay)
        getPrefs(context).edit().putBoolean(KEY_AUTO_PLAY, autoPlay).apply()
    }

    fun updateVolumeSide(context: Context, volumeOnRight: Boolean) {
        _settingsFlow.value = _settingsFlow.value.copy(volumeOnRight = volumeOnRight)
        getPrefs(context).edit().putBoolean(KEY_VOLUME_ON_RIGHT, volumeOnRight).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
