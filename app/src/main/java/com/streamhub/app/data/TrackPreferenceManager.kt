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

    private const val MAX_TRACK_ENTRIES = 500
    private const val KEY_PREFIX_AUDIO = "audio_pref_"
    private const val KEY_PREFIX_SUBTITLE = "sub_pref_"
    private const val KEY_GLOBAL_AUDIO_LANG = "global_audio_lang"
    private const val KEY_GLOBAL_SUBTITLE_LANG = "global_sub_lang"
    private const val DELIMITER = ":::"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        try {
            val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = sp
            // Clean up any legacy global subtitle key to permanently eliminate cross-media subtitle bleeding
            if (sp.contains(KEY_GLOBAL_SUBTITLE_LANG)) {
                sp.edit().remove(KEY_GLOBAL_SUBTITLE_LANG).apply()
            }
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
            pruneOldEntriesIfNeeded(editor, p)
            editor.apply()
            Log.d(TAG, "Saved audio preference for '$mediaId': label='$trackLabel', lang='$languageCode'")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save audio preference", e)
        }
    }

    @Synchronized
    fun saveSubtitlePreference(mediaId: String, trackLabel: String, languageCode: String?) {
        val p = prefs ?: return
        if (mediaId.isBlank()) return
        try {
            val encoded = encodePreference(trackLabel, languageCode)
            val editor = p.edit()
            editor.putString(KEY_PREFIX_SUBTITLE + mediaId, encoded)
            // Ensure no legacy global subtitle key lingers to prevent cross-media auto-selection
            editor.remove(KEY_GLOBAL_SUBTITLE_LANG)
            pruneOldEntriesIfNeeded(editor, p)
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

    /**
     * Retrieves the subtitle preference strictly for [mediaId].
     * Returns null if the user has not explicitly selected a subtitle for this media/series,
     * guaranteeing subtitles remain strictly Off by default for any new video.
     */
    fun getSubtitlePreference(mediaId: String): SavedTrackPreference? {
        val p = prefs ?: return null
        if (mediaId.isBlank()) return null
        val saved = p.getString(KEY_PREFIX_SUBTITLE + mediaId, null)
        return if (!saved.isNullOrBlank()) decodePreference(saved) else null
    }

    private fun pruneOldEntriesIfNeeded(editor: SharedPreferences.Editor, p: SharedPreferences) {
        val allKeys = p.all.keys.filter { it.startsWith(KEY_PREFIX_AUDIO) || it.startsWith(KEY_PREFIX_SUBTITLE) }
        if (allKeys.size > MAX_TRACK_ENTRIES) {
            // Prune oldest entries based on timestamp to keep storage strictly under 25 KB
            val parsed = allKeys.mapNotNull { key ->
                val raw = p.getString(key, null) ?: return@mapNotNull null
                val parts = raw.split(DELIMITER)
                val time = parts.getOrNull(2)?.toLongOrNull() ?: 0L
                key to time
            }.sortedBy { it.second }

            val toRemoveCount = allKeys.size - (MAX_TRACK_ENTRIES - 50)
            if (toRemoveCount > 0) {
                parsed.take(toRemoveCount).forEach { (key, _) ->
                    editor.remove(key)
                }
                Log.d(TAG, "Pruned $toRemoveCount old track preference entries to maintain strict storage limits")
            }
        }
    }

    private fun encodePreference(label: String, lang: String?, timestamp: Long = System.currentTimeMillis()): String {
        return "$label$DELIMITER${lang.orEmpty()}$DELIMITER$timestamp"
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
