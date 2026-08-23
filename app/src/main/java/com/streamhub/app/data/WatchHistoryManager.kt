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

    private fun parseProgress(jsonStr: String): PlaybackProgress? {
        return try {
            val json = JSONObject(jsonStr)
            PlaybackProgress(
                mediaId = json.getString("mediaId"),
                episodeNumber = json.optInt("episodeNumber", 0),
                positionMs = json.optLong("positionMs", 0L),
                durationMs = json.optLong("durationMs", 0L),
                lastUpdated = json.optLong("lastUpdated", System.currentTimeMillis()),
                title = json.optString("title", ""),
                posterUrl = json.optString("posterUrl", ""),
                backdropUrl = json.optString("backdropUrl", ""),
                mediaType = json.optString("mediaType", ""),
                episodeTitle = json.optString("episodeTitle", ""),
                seasonNumber = json.optInt("seasonNumber", 0),
                isCompleted = json.optBoolean("isCompleted", false)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse progress JSON", e)
            null
        }
    }

    private fun loadFromDisk() {
        val prefs = getPrefs()
        val historyMap = mutableMapOf<String, PlaybackProgress>()
        val allKeys = (prefs.getStringSet(KEY_ALL_HISTORY_IDS, emptySet()) ?: emptySet()).toSet()

        if (allKeys.isNotEmpty()) {
            for (mediaId in allKeys) {
                val jsonStr = prefs.getString(mediaId, null) ?: continue
                parseProgress(jsonStr)?.let { historyMap[mediaId] = it }
            }
        } else {
            // Fallback for migration from old prefs.all
            val allEntries = prefs.all
            for ((mediaId, jsonStr) in allEntries) {
                if (mediaId == KEY_ALL_HISTORY_IDS) continue
                if (jsonStr is String) {
                    parseProgress(jsonStr)?.let { historyMap[mediaId] = it }
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
        durationMs: Long,
        title: String = "",
        posterUrl: String = "",
        backdropUrl: String = "",
        mediaType: String = "",
        episodeTitle: String = "",
        seasonNumber: Int = 0
    ) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "saveProgress called before init — no-op")
            return
        }
        if (mediaId.isBlank()) return

        val existing = _historyFlow.value[mediaId]
        val effectiveDuration = when {
            durationMs > 0L -> durationMs
            (existing?.durationMs ?: 0L) > 0L -> existing!!.durationMs
            positionMs > 0L -> positionMs
            else -> 1L
        }
        val isCompleted = if (effectiveDuration > 0) (positionMs.toFloat() / effectiveDuration.toFloat()) >= 0.92f else false

        val progress = PlaybackProgress(
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            positionMs = positionMs,
            durationMs = effectiveDuration,
            lastUpdated = System.currentTimeMillis(),
            title = title.ifEmpty { existing?.title ?: "" },
            posterUrl = posterUrl.ifEmpty { existing?.posterUrl ?: "" },
            backdropUrl = backdropUrl.ifEmpty { existing?.backdropUrl ?: "" },
            mediaType = mediaType.ifEmpty { existing?.mediaType ?: "" },
            episodeTitle = episodeTitle.ifEmpty { existing?.episodeTitle ?: "" },
            seasonNumber = if (seasonNumber >= 0) seasonNumber else (existing?.seasonNumber ?: 0),
            isCompleted = isCompleted || (existing?.isCompleted == true && positionMs < 10_000L)
        )

        val updatedMap = _historyFlow.value.toMutableMap()
        updatedMap[mediaId] = progress

        val evictedKeys = mutableListOf<String>()
        if (updatedMap.size > 200) {
            val sortedByAge = updatedMap.entries.sortedBy { it.value.lastUpdated }
            val toRemove = sortedByAge.take(updatedMap.size - 200)
            for ((oldKey, _) in toRemove) {
                updatedMap.remove(oldKey)
                evictedKeys.add(oldKey)
            }
        }

        _historyFlow.value = updatedMap

        try {
            val json = JSONObject().apply {
                put("mediaId", progress.mediaId)
                put("episodeNumber", progress.episodeNumber)
                put("positionMs", progress.positionMs)
                put("durationMs", progress.durationMs)
                put("lastUpdated", progress.lastUpdated)
                put("title", progress.title)
                put("posterUrl", progress.posterUrl)
                put("backdropUrl", progress.backdropUrl)
                put("mediaType", progress.mediaType)
                put("episodeTitle", progress.episodeTitle)
                put("seasonNumber", progress.seasonNumber)
                put("isCompleted", progress.isCompleted)
            }
            val prefs = getPrefs()
            val currentIds = (prefs.getStringSet(KEY_ALL_HISTORY_IDS, emptySet()) ?: emptySet()).toMutableSet()
            currentIds.add(mediaId)
            currentIds.removeAll(evictedKeys)

            val editor = prefs.edit()
                .putString(mediaId, json.toString())
                .putStringSet(KEY_ALL_HISTORY_IDS, currentIds)
            for (evictedKey in evictedKeys) {
                editor.remove(evictedKey)
            }
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist progress for $mediaId", e)
        }
    }

    fun getProgress(mediaId: String): PlaybackProgress? {
        return _historyFlow.value[mediaId]
    }

    /**
     * Returns history list sorted newest to oldest, optionally filtered.
     */
    fun getHistoryList(filterType: String = "All", searchQuery: String = ""): List<PlaybackProgress> {
        return _historyFlow.value.values
            .sortedByDescending { it.lastUpdated }
            .filter { item ->
                val matchesType = when (filterType.lowercase()) {
                    "all" -> true
                    "movies" -> item.mediaType.equals("Movie", ignoreCase = true) || item.mediaType.equals("Movies", ignoreCase = true)
                    "anime" -> item.mediaType.equals("Anime", ignoreCase = true)
                    "series" -> item.mediaType.equals("Series", ignoreCase = true) || item.mediaType.equals("Web Series", ignoreCase = true)
                    else -> true
                }
                val matchesSearch = searchQuery.isBlank() ||
                        item.title.contains(searchQuery, ignoreCase = true) ||
                        item.episodeTitle.contains(searchQuery, ignoreCase = true)
                matchesType && matchesSearch
            }
    }

    /**
     * Groups watch history chronologically into Today, Yesterday, This Week, and Older.
     */
    fun getGroupedHistory(filterType: String = "All", searchQuery: String = ""): Map<String, List<PlaybackProgress>> {
        val items = getHistoryList(filterType, searchQuery)
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val calendar = java.util.Calendar.getInstance()

        // Today start midnight
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val todayMidnight = calendar.timeInMillis
        val yesterdayMidnight = todayMidnight - oneDayMs
        val weekMidnight = todayMidnight - (6 * oneDayMs)

        val groups = linkedMapOf<String, MutableList<PlaybackProgress>>()
        groups["Today"] = mutableListOf()
        groups["Yesterday"] = mutableListOf()
        groups["This Week"] = mutableListOf()
        groups["Older"] = mutableListOf()

        for (item in items) {
            when {
                item.lastUpdated >= todayMidnight -> groups["Today"]?.add(item)
                item.lastUpdated >= yesterdayMidnight -> groups["Yesterday"]?.add(item)
                item.lastUpdated >= weekMidnight -> groups["This Week"]?.add(item)
                else -> groups["Older"]?.add(item)
            }
        }

        return groups.filterValues { it.isNotEmpty() }
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

    private var prefs: SharedPreferences? = null

    private fun getPrefs(): SharedPreferences {
        if (prefs == null && ::appContext.isInitialized) {
            prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return prefs ?: throw IllegalStateException("WatchHistoryManager not initialized — call init(context) first")
    }
}
