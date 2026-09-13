package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Encapsulates a persisted track preference (exact label and optional ISO language code).
 */
data class SavedTrackPreference(
    val trackLabel: String,
    val languageCode: String? = null
)

/**
 * Manages persistent audio and subtitle track selections across playback sessions,
 * app restarts, and Continue Watching entries.
 *
 * Persists preferences per-media (by mediaId) and retains a global last-used language fallback.
 */
object TrackPreferenceManager {

    private const val TAG = "TrackPreferenceManager"
    private const val PREFS_NAME = "streamhub_track_preferences"

    private const val KEY_PREFIX_AUDIO = "audio_pref_"
    private const val KEY_PREFIX_SUBTITLE = "sub_pref_"
    private const val KEY_GLOBAL_AUDIO_LANG = "global_audio_lang"
    private const val KEY_GLOBAL_SUBTITLE_LANG = "global_sub_lang"
    private const val DELIMITER = ":::"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        try {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            Log.i(TAG, "TrackPreferenceManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TrackPreferenceManager", e)
        }
    }

    @Synchronized
    fun saveAudioPreference(mediaId: String, trackLabel: String, languageCode: String?) {
        val p = prefs ?: return
        if (trackLabel.isBlank()) return
        try {
            val encoded = encodePreference(trackLabel, languageCode)
            val editor = p.edit()
            if (mediaId.isNotBlank()) {
                editor.putString(KEY_PREFIX_AUDIO + mediaId, encoded)
            }
            if (!languageCode.isNullOrBlank() && languageCode != "und") {
                editor.putString(KEY_GLOBAL_AUDIO_LANG, encodePreference(trackLabel, languageCode))
            }
            editor.apply()
            Log.d(TAG, "Saved audio preference for '$mediaId': label='$trackLabel', lang='$languageCode'")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save audio preference", e)
        }
    }

    @Synchronized
    fun saveSubtitlePreference(mediaId: String, trackLabel: String, languageCode: String?) {
        val p = prefs ?: return
        try {
            val encoded = encodePreference(trackLabel, languageCode)
            val editor = p.edit()
            if (mediaId.isNotBlank()) {
                editor.putString(KEY_PREFIX_SUBTITLE + mediaId, encoded)
            }
            // If explicitly turned Off or selected a valid language, remember it globally
            editor.putString(KEY_GLOBAL_SUBTITLE_LANG, encoded)
            editor.apply()
            Log.d(TAG, "Saved subtitle preference for '$mediaId': label='$trackLabel', lang='$languageCode'")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save subtitle preference", e)
        }
    }

    fun getAudioPreference(mediaId: String): SavedTrackPreference? {
        val p = prefs ?: return null
        if (mediaId.isNotBlank()) {
            val saved = p.getString(KEY_PREFIX_AUDIO + mediaId, null)
            if (!saved.isNullOrBlank()) {
                return decodePreference(saved)
            }
        }
        // Fallback to global last-used audio preference
        val global = p.getString(KEY_GLOBAL_AUDIO_LANG, null)
        return if (!global.isNullOrBlank()) decodePreference(global) else null
    }

    fun getSubtitlePreference(mediaId: String): SavedTrackPreference? {
        val p = prefs ?: return null
        if (mediaId.isNotBlank()) {
            val saved = p.getString(KEY_PREFIX_SUBTITLE + mediaId, null)
            if (!saved.isNullOrBlank()) {
                return decodePreference(saved)
            }
        }
        // Fallback to global last-used subtitle preference
        val global = p.getString(KEY_GLOBAL_SUBTITLE_LANG, null)
        return if (!global.isNullOrBlank()) decodePreference(global) else null
    }

    private fun encodePreference(label: String, lang: String?): String {
        return "$label$DELIMITER${lang.orEmpty()}"
    }

    private fun decodePreference(raw: String): SavedTrackPreference? {
        return try {
            val parts = raw.split(DELIMITER)
            if (parts.isNotEmpty()) {
                val label = parts[0]
                val lang = parts.getOrNull(1)?.ifBlank { null }
                SavedTrackPreference(trackLabel = label, languageCode = lang)
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
