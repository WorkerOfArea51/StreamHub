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
 *
 * Initialized once by StreamHubApplication.onCreate(). Callers do NOT pass
 * context to any method — the applicationContext is captured in init().
 *
 * Thread-safety: all public methods are safe to call from any thread.
 * SharedPreferences.edit().apply() is async and thread-safe.
 */
object WatchHistoryManager {

    private const val TAG = "WatchHistoryManager"
    private const val PREFS_NAME = "streamhub_watch_history"

    private lateinit var appContext: Context

    private val _historyFlow = MutableStateFlow<Map<String, PlaybackProgress>>(emptyMap())
    val historyFlow: StateFlow<Map<String, PlaybackProgress>> = _historyFlow.asStateFlow()

    /**
     * Initialize the manager with the application context. Called once by
     * StreamHubApplication.onCreate(). Subsequent calls are no-ops (idempotent).
     */
    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val prefs = getPrefs()
        val allEntries = prefs.all
        val historyMap = mutableMapOf<String, PlaybackProgress>()

        for ((mediaId, jsonStr) in allEntries) {
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
        _historyFlow.value = historyMap
    }

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
        _historyFlow.value = updatedMap

        try {
            val json = JSONObject().apply {
                put("mediaId", mediaId)
                put("episodeNumber", episodeNumber)
                put("positionMs", positionMs)
                put("durationMs", durationMs)
                put("lastUpdated", progress.lastUpdated)
            }
            getPrefs().edit().putString(mediaId, json.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist progress for $mediaId", e)
        }
    }

    fun getProgress(mediaId: String): PlaybackProgress? {
        return _historyFlow.value[mediaId]
    }

    fun removeMediaProgress(mediaId: String) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "removeMediaProgress called before init — no-op")
            return
        }
        val updatedMap = _historyFlow.value.toMutableMap()
        updatedMap.remove(mediaId)
        _historyFlow.value = updatedMap
        getPrefs().edit().remove(mediaId).apply()
    }

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
