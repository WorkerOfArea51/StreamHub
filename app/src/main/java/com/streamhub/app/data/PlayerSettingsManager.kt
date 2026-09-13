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
    val nextEpisodeThresholdSeconds: Int = -1, // -1 (Smart Auto), 90s, 180s (3m), 300s (5m), 420s (7m), 0 (Disabled)
    val autoPlayNextEpisode: Boolean = true,
    val volumeOnRight: Boolean = true, // Volume on right (Anim on left) vs Volume on left (Anim on right)
    val defaultAspectRatioId: String = "FIT",
    val rememberAspectRatio: Boolean = true,
    val isAmbientEnabled: Boolean = true,
    val ambientMoodId: String = "COZY_CINEMA",
    val ambientIntensity: Float = 0.15f, // 0.05f to 0.50f (Default: 0.15f cozy)
    val smartPrewarmEnabled: Boolean = true,
    val bingePrecacheEnabled: Boolean = true,
    val doubleTapSeekSeconds: Int = 10, // 5s, 10s, 15s, 30s
    val seekbarStyle: SeekbarStyle = SeekbarStyle.Wavy,
    val rememberBrightness: Boolean = false,
    val savedBrightness: Float = -1f,
    val autoPiPOnNavigation: Boolean = true,
    val keepScreenOnWhenPaused: Boolean = false,
    val volumeNormalization: Boolean = false
)

/**
 * Persists player preferences (skip intro duration, next-ep threshold, ambient lighting, etc.)
 * in SharedPreferences.
 *
 * Initialized once by StreamHubApplication.onCreate(). Callers do NOT pass
 * Context into mutation functions; they safely use the internal reference.
 */
object PlayerSettingsManager {

    private const val TAG = "PlayerSettingsManager"
    private const val PREFS_NAME = "streamhub_player_settings"

    private const val KEY_SKIP_INTRO = "skip_intro_sec"
    const val KEY_NEXT_EPISODE_THRESHOLD = "next_ep_threshold_sec"
    private const val KEY_AUTO_PLAY = "auto_play_next"
    private const val KEY_VOLUME_ON_RIGHT = "volume_on_right"
    private const val KEY_DEFAULT_ASPECT_RATIO = "default_aspect_ratio_id"
    private const val KEY_REMEMBER_ASPECT_RATIO = "remember_aspect_ratio"
    private const val KEY_AMBIENT_ENABLED = "ambient_enabled"
    private const val KEY_AMBIENT_MOOD_ID = "ambient_mood_id"
    private const val KEY_AMBIENT_INTENSITY = "ambient_intensity"
    private const val KEY_SMART_PREWARM = "smart_prewarm_enabled"
    private const val KEY_BINGE_PRECACHE = "binge_precache_enabled"
    private const val KEY_DOUBLE_TAP_SEEK = "double_tap_seek_sec"
    private const val KEY_SEEKBAR_STYLE = "seekbar_style"
    private const val KEY_REMEMBER_BRIGHTNESS = "remember_brightness"
    private const val KEY_SAVED_BRIGHTNESS = "saved_brightness"
    private const val KEY_AUTO_PIP = "auto_pip_on_navigation"
    private const val KEY_KEEP_SCREEN_ON_PAUSED = "keep_screen_on_when_paused"
    private const val KEY_VOLUME_NORMALIZATION = "volume_normalization"

    private lateinit var appContext: Context

    private val _settingsFlow = MutableStateFlow(PlayerSettings())
    val settingsFlow: StateFlow<PlayerSettings> = _settingsFlow.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val prefs = getPrefs()
        try {
            _settingsFlow.value = PlayerSettings(
                skipIntroSeconds = prefs.getInt(KEY_SKIP_INTRO, 90),
                nextEpisodeThresholdSeconds = prefs.getInt(KEY_NEXT_EPISODE_THRESHOLD, -1),
                autoPlayNextEpisode = prefs.getBoolean(KEY_AUTO_PLAY, true),
                volumeOnRight = prefs.getBoolean(KEY_VOLUME_ON_RIGHT, true),
                defaultAspectRatioId = prefs.getString(KEY_DEFAULT_ASPECT_RATIO, "FIT") ?: "FIT",
                rememberAspectRatio = prefs.getBoolean(KEY_REMEMBER_ASPECT_RATIO, true),
                isAmbientEnabled = prefs.getBoolean(KEY_AMBIENT_ENABLED, true),
                ambientMoodId = prefs.getString(KEY_AMBIENT_MOOD_ID, "COZY_CINEMA") ?: "COZY_CINEMA",
                ambientIntensity = prefs.getFloat(KEY_AMBIENT_INTENSITY, 0.15f),
                smartPrewarmEnabled = prefs.getBoolean(KEY_SMART_PREWARM, true),
                bingePrecacheEnabled = prefs.getBoolean(KEY_BINGE_PRECACHE, true),
                doubleTapSeekSeconds = prefs.getInt(KEY_DOUBLE_TAP_SEEK, 10),
                seekbarStyle = when (prefs.getString(KEY_SEEKBAR_STYLE, "WAVY")?.uppercase()) {
                    "STANDARD" -> SeekbarStyle.Standard
                    "THICK" -> SeekbarStyle.Thick
                    else -> SeekbarStyle.Wavy
                },
                rememberBrightness = prefs.getBoolean(KEY_REMEMBER_BRIGHTNESS, false),
                savedBrightness = prefs.getFloat(KEY_SAVED_BRIGHTNESS, -1f),
                autoPiPOnNavigation = prefs.getBoolean(KEY_AUTO_PIP, true),
                keepScreenOnWhenPaused = prefs.getBoolean(KEY_KEEP_SCREEN_ON_PAUSED, false),
                volumeNormalization = prefs.getBoolean(KEY_VOLUME_NORMALIZATION, false)
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
        val clamped = seconds.coerceIn(0, 300)
        _settingsFlow.update { it.copy(skipIntroSeconds = clamped) }
        getPrefs().edit().putInt(KEY_SKIP_INTRO, clamped).apply()
    }

    @Synchronized
    fun updateNextEpisodeThreshold(seconds: Int) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateNextEpisodeThreshold called before init — no-op")
            return
        }
        val clamped = if (seconds == -1) -1 else seconds.coerceIn(0, 480)
        _settingsFlow.update { it.copy(nextEpisodeThresholdSeconds = clamped) }
        getPrefs().edit().putInt(KEY_NEXT_EPISODE_THRESHOLD, clamped).apply()
    }

    /**
     * Computes the effective countdown threshold in seconds for the next episode prompt.
     * If configured as -1 (Smart Auto):
     *  - Short form / Anime (<= 32 min): 90 seconds (standard anime ED).
     *  - Long form / Web series (> 32 min): 10% of total duration clamped between 180s (3m) and 420s (7m).
     * Otherwise returns the explicitly configured seconds (or 0 if disabled).
     */
    fun computeEffectiveNextEpThresholdSec(durationMs: Long, configuredSec: Int): Int {
        if (configuredSec == 0) return 0
        if (configuredSec > 0) return configuredSec
        // -1 = Smart Auto
        if (durationMs <= 0L) return 90
        val durationSec = durationMs / 1000L
        return if (durationSec <= 32 * 60) {
            90
        } else {
            (durationSec * 0.10).toInt().coerceIn(180, 420)
        }
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

    @Synchronized
    fun updateDefaultAspectRatio(aspectRatioId: String) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateDefaultAspectRatio called before init — no-op")
            return
        }
        _settingsFlow.update { it.copy(defaultAspectRatioId = aspectRatioId) }
        getPrefs().edit().putString(KEY_DEFAULT_ASPECT_RATIO, aspectRatioId).apply()
    }

    @Synchronized
    fun updateRememberAspectRatio(remember: Boolean) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateRememberAspectRatio called before init — no-op")
            return
        }
        _settingsFlow.update { it.copy(rememberAspectRatio = remember) }
        getPrefs().edit().putBoolean(KEY_REMEMBER_ASPECT_RATIO, remember).apply()
    }

    @Synchronized
    fun updateAmbientEnabled(enabled: Boolean) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateAmbientEnabled called before init — no-op")
            return
        }
        _settingsFlow.update { it.copy(isAmbientEnabled = enabled) }
        getPrefs().edit().putBoolean(KEY_AMBIENT_ENABLED, enabled).apply()
    }

    @Synchronized
    fun updateAmbientMood(moodId: String) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateAmbientMood called before init — no-op")
            return
        }
        _settingsFlow.update { it.copy(ambientMoodId = moodId) }
        getPrefs().edit().putString(KEY_AMBIENT_MOOD_ID, moodId).apply()
    }

    @Synchronized
    fun updateAmbientIntensity(intensity: Float) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateAmbientIntensity called before init — no-op")
            return
        }
        val clamped = intensity.coerceIn(0.05f, 0.50f)
        _settingsFlow.update { it.copy(ambientIntensity = clamped) }
        getPrefs().edit().putFloat(KEY_AMBIENT_INTENSITY, clamped).apply()
    }

    @Synchronized
    fun updateSmartPrewarmEnabled(enabled: Boolean) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateSmartPrewarmEnabled called before init — no-op")
            return
        }
        _settingsFlow.update { it.copy(smartPrewarmEnabled = enabled) }
        getPrefs().edit().putBoolean(KEY_SMART_PREWARM, enabled).apply()
    }

    @Synchronized
    fun updateBingePrecacheEnabled(enabled: Boolean) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateBingePrecacheEnabled called before init — no-op")
            return
        }
        _settingsFlow.update { it.copy(bingePrecacheEnabled = enabled) }
        getPrefs().edit().putBoolean(KEY_BINGE_PRECACHE, enabled).apply()
    }

    @Synchronized
    fun updateDoubleTapSeek(seconds: Int) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "updateDoubleTapSeek called before init — no-op")
            return
        }
        val clamped = seconds.coerceIn(5, 60)
        _settingsFlow.update { it.copy(doubleTapSeekSeconds = clamped) }
        getPrefs().edit().putInt(KEY_DOUBLE_TAP_SEEK, clamped).apply()
    }

    @Synchronized
    fun updateSeekbarStyle(style: SeekbarStyle) {
        if (!::appContext.isInitialized) return
        _settingsFlow.update { it.copy(seekbarStyle = style) }
        getPrefs().edit().putString(KEY_SEEKBAR_STYLE, style.name.uppercase()).apply()
    }

    @Synchronized
    fun updateRememberBrightness(remember: Boolean) {
        if (!::appContext.isInitialized) return
        _settingsFlow.update { it.copy(rememberBrightness = remember) }
        getPrefs().edit().putBoolean(KEY_REMEMBER_BRIGHTNESS, remember).apply()
    }

    @Synchronized
    fun updateSavedBrightness(brightness: Float) {
        if (!::appContext.isInitialized) return
        _settingsFlow.update { it.copy(savedBrightness = brightness) }
        getPrefs().edit().putFloat(KEY_SAVED_BRIGHTNESS, brightness).apply()
    }

    @Synchronized
    fun updateAutoPiP(enabled: Boolean) {
        if (!::appContext.isInitialized) return
        _settingsFlow.update { it.copy(autoPiPOnNavigation = enabled) }
        getPrefs().edit().putBoolean(KEY_AUTO_PIP, enabled).apply()
    }

    @Synchronized
    fun updateAutoPiPOnNavigation(enabled: Boolean) = updateAutoPiP(enabled)

    @Synchronized
    fun updateAutoPlayNextEpisode(autoPlay: Boolean) {
        if (!::appContext.isInitialized) return
        _settingsFlow.update { it.copy(autoPlayNextEpisode = autoPlay) }
        getPrefs().edit().putBoolean("auto_play_next", autoPlay).apply()
    }

    @Synchronized
    fun updateKeepScreenOnWhenPaused(keep: Boolean) {
        if (!::appContext.isInitialized) return
        _settingsFlow.update { it.copy(keepScreenOnWhenPaused = keep) }
        getPrefs().edit().putBoolean(KEY_KEEP_SCREEN_ON_PAUSED, keep).apply()
    }

    @Synchronized
    fun updateVolumeNormalization(normalized: Boolean) {
        if (!::appContext.isInitialized) return
        _settingsFlow.update { it.copy(volumeNormalization = normalized) }
        getPrefs().edit().putBoolean(KEY_VOLUME_NORMALIZATION, normalized).apply()
    }

    @Synchronized
    fun updateVolumeOnRight(onRight: Boolean) {
        if (!::appContext.isInitialized) return
        _settingsFlow.update { it.copy(volumeOnRight = onRight) }
        getPrefs().edit().putBoolean(KEY_VOLUME_ON_RIGHT, onRight).apply()
    }

    private fun getPrefs(): SharedPreferences {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
