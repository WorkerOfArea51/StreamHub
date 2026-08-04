package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import com.streamhub.app.data.models.PlaybackProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object WatchHistoryManager {
    private const val PREFS_NAME = "streamhub_watch_history"
    private val _historyFlow = MutableStateFlow<Map<String, PlaybackProgress>>(emptyMap())
    val historyFlow: StateFlow<Map<String, PlaybackProgress>> = _historyFlow.asStateFlow()

    fun init(context: Context) {
        val prefs = getPrefs(context)
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
                    e.printStackTrace()
                }
            }
        }
        _historyFlow.value = historyMap
    }

    fun saveProgress(
        context: Context,
        mediaId: String,
        episodeNumber: Int,
        positionMs: Long,
        durationMs: Long
    ) {
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
            getPrefs(context).edit().putString(mediaId, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getProgress(mediaId: String): PlaybackProgress? {
        return _historyFlow.value[mediaId]
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
