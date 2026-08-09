package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.streamhub.app.data.models.PlaybackProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Persists per-media playback progress in SharedPreferences.
 */
object WatchHistoryManager {

    private const val TAG = "WatchHistoryManager"
    private const val PREFS_NAME = "streamhub_watch_history"
    private const val KEY_ALL_HISTORY_IDS = "all_history_ids_set"

    private lateinit var appContext: Context

    private val _historyFlow = MutableStateFlow<Map<String, PlaybackProgress>>(emptyMap())
    val historyFlow: StateFlow<Map<String, PlaybackProgress>> = _historyFlow.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val prefs = getPrefs()
        val historyMap = mutableMapOf<String, PlaybackProgress>()
        val allKeys = (prefs.getStringSet(KEY_ALL_HISTORY_IDS, emptySet()) ?: emptySet()).toSet()

        if (allKeys.isNotEmpty()) {
            for (mediaId in allKeys) {
                val jsonStr = prefs.getString(mediaId, null) ?: continue
                try {
                    val json = JSONObject(jsonStr)
                    val progress = PlaybackProgress(
                        mediaId = json.getString("mediaId"),
                        episodeNumber = json.getInt("episodeNumber"),
                        positionMs = json.getLong("positionMs"),
                        durationMs = json.getLong("durationMs"),
                        lastUpdated = json.getLong("lastUpdated")
                    )
                    historyMap[mediaId] = progress
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse progress for $mediaId", e)
                }
            }
        } else {
            // Fallback for migration from old prefs.all
            val allEntries = prefs.all
            for ((mediaId, jsonStr) in allEntries) {
                if (mediaId == KEY_ALL_HISTORY_IDS) continue
                if (jsonStr is String) {
                    try {
                        val json = JSONObject(jsonStr)
                        val progress = PlaybackProgress(
                            mediaId = json.getString("mediaId"),
                            episodeNumber = json.getInt("episodeNumber"),
                            positionMs = json.getLong("positionMs"),
                            durationMs = json.getLong("durationMs"),
                            lastUpdated = json.getLong("lastUpdated")
                        )
                        historyMap[mediaId] = progress
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse progress for $mediaId", e)
                    }
                }
            }
        }
        _historyFlow.value = historyMap
    }

    @Synchronized
    fun saveProgress(
        mediaId: String,
        episodeNumber: Int,
        positionMs: Long,
        durationMs: Long
    ) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "saveProgress called before init — no-op")
            return
        }
        if (mediaId.isEmpty() || durationMs <= 0) return

        val progress = PlaybackProgress(
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            positionMs = positionMs,
            durationMs = durationMs,
            lastUpdated = System.currentTimeMillis()
        )

        val updatedMap = _historyFlow.value.toMutableMap()
        updatedMap[mediaId] = progress

        // M9 FIX: Evict oldest entries if history exceeds MAX_HISTORY_ENTRIES (100)
        if (updatedMap.size > 100) {
            val sortedByAge = updatedMap.entries.sortedBy { it.value.lastUpdated }
            val toRemove = sortedByAge.take(updatedMap.size - 100)
            for ((oldKey, _) in toRemove) {
                updatedMap.remove(oldKey)
            }
        }

        _historyFlow.value = updatedMap

        try {
            val json = JSONObject().apply {
                put("mediaId", mediaId)
                put("episodeNumber", episodeNumber)
                put("positionMs", positionMs)
                put("durationMs", durationMs)
                put("lastUpdated", progress.lastUpdated)
            }
            val prefs = getPrefs()
            val currentIds = (prefs.getStringSet(KEY_ALL_HISTORY_IDS, emptySet()) ?: emptySet()).toMutableSet()
            currentIds.add(mediaId)

            prefs.edit()
                .putString(mediaId, json.toString())
                .putStringSet(KEY_ALL_HISTORY_IDS, currentIds)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist progress for $mediaId", e)
        }
    }

    fun getProgress(mediaId: String): PlaybackProgress? {
        return _historyFlow.value[mediaId]
    }

    @Synchronized
    fun removeMediaProgress(mediaId: String) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "removeMediaProgress called before init — no-op")
            return
        }
        val updatedMap = _historyFlow.value.toMutableMap()
        updatedMap.remove(mediaId)
        _historyFlow.value = updatedMap

        val prefs = getPrefs()
        val currentIds = (prefs.getStringSet(KEY_ALL_HISTORY_IDS, emptySet()) ?: emptySet()).toMutableSet()
        currentIds.remove(mediaId)

        prefs.edit()
            .remove(mediaId)
            .putStringSet(KEY_ALL_HISTORY_IDS, currentIds)
            .apply()
    }

    @Synchronized
    fun clearAllHistory() {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "clearAllHistory called before init — no-op")
            return
        }
        _historyFlow.value = emptyMap()
        getPrefs().edit().clear().apply()
    }

    private fun getPrefs(): SharedPreferences {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
