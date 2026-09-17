package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Search History Manager:
 * - Persists recent search terms to SharedPreferences
 * - Automatically trims to latest 12 entries
 * - Exposes reactive StateFlow for Compose UI
 */
object SearchHistoryManager {

    private const val TAG = "SearchHistoryManager"
    private const val PREFS_NAME = "streamhub_search_history"
    private const val KEY_HISTORY = "recent_queries"
    private const val KEY_FREQUENCIES = "query_frequencies"
    private const val MAX_HISTORY_ITEMS = 12
    private const val MAX_FREQUENCY_ITEMS = 50

    private lateinit var appContext: Context
    private var prefs: SharedPreferences? = null

    private val _historyFlow = MutableStateFlow<List<String>>(emptyList())
    val historyFlow: StateFlow<List<String>> = _historyFlow.asStateFlow()

    private val _frequenciesFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    val frequenciesFlow: StateFlow<Map<String, Int>> = _frequenciesFlow.asStateFlow()

    private val _topQueriesFlow = MutableStateFlow<List<String>>(emptyList())
    val topQueriesFlow: StateFlow<List<String>> = _topQueriesFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val p = prefs ?: return
        try {
            val jsonStr = p.getString(KEY_HISTORY, "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val query = array.optString(i)
                if (query.isNotBlank() && !list.contains(query)) {
                    list.add(query)
                }
            }
            _historyFlow.value = list

            // Load frequencies
            val freqStr = p.getString(KEY_FREQUENCIES, "{}") ?: "{}"
            val freqObj = org.json.JSONObject(freqStr)
            val freqMap = mutableMapOf<String, Int>()
            val keys = freqObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                freqMap[k] = freqObj.optInt(k, 1)
            }
            _frequenciesFlow.value = freqMap
            updateTopQueries(freqMap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load search history", e)
            _historyFlow.value = emptyList()
            _frequenciesFlow.value = emptyMap()
            _topQueriesFlow.value = emptyList()
        }
    }

    private fun updateTopQueries(freqMap: Map<String, Int>) {
        _topQueriesFlow.value = freqMap.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { it.key }
    }

    fun addQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || trimmed.startsWith("#")) return

        // Update Recents
        val current = _historyFlow.value.toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val trimmedList = current.take(MAX_HISTORY_ITEMS)
        _historyFlow.value = trimmedList
        saveHistoryToDisk(trimmedList)

        // Update Frequencies
        val currentFreq = _frequenciesFlow.value.toMutableMap()
        // Find existing key ignoring case
        val existingKey = currentFreq.keys.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: trimmed
        val newCount = (currentFreq[existingKey] ?: 0) + 1
        currentFreq[existingKey] = newCount

        // Trim frequencies if too large
        val trimmedFreq = currentFreq.entries
            .sortedByDescending { it.value }
            .take(MAX_FREQUENCY_ITEMS)
            .associate { it.key to it.value }

        _frequenciesFlow.value = trimmedFreq
        updateTopQueries(trimmedFreq)
        saveFrequenciesToDisk(trimmedFreq)
    }

    fun removeQuery(query: String) {
        val current = _historyFlow.value.toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        _historyFlow.value = current
        saveHistoryToDisk(current)

        val currentFreq = _frequenciesFlow.value.toMutableMap()
        currentFreq.keys.filter { it.equals(query, ignoreCase = true) }.forEach { currentFreq.remove(it) }
        _frequenciesFlow.value = currentFreq
        updateTopQueries(currentFreq)
        saveFrequenciesToDisk(currentFreq)
    }

    fun clearAll() {
        _historyFlow.value = emptyList()
        _frequenciesFlow.value = emptyMap()
        _topQueriesFlow.value = emptyList()
        prefs?.edit()?.remove(KEY_HISTORY)?.remove(KEY_FREQUENCIES)?.apply()
    }

    /**
     * Restores search history from backup payload.
     */
    fun restoreFromBackup(queries: List<String>?, mergeMode: Boolean) {
        if (queries.isNullOrEmpty()) return
        val targetList = if (mergeMode) {
            (queries + _historyFlow.value).distinct().take(MAX_HISTORY_ITEMS)
        } else {
            queries.distinct().take(MAX_HISTORY_ITEMS)
        }
        _historyFlow.value = targetList
        saveHistoryToDisk(targetList)

        val currentFreq = if (mergeMode) _frequenciesFlow.value.toMutableMap() else mutableMapOf()
        queries.forEach { q ->
            val existing = currentFreq.keys.firstOrNull { it.equals(q, ignoreCase = true) } ?: q
            currentFreq[existing] = (currentFreq[existing] ?: 0) + 1
        }
        _frequenciesFlow.value = currentFreq
        updateTopQueries(currentFreq)
        saveFrequenciesToDisk(currentFreq)
    }

    private fun saveHistoryToDisk(list: List<String>) {
        val p = prefs ?: return
        try {
            val array = JSONArray()
            list.forEach { array.put(it) }
            p.edit().putString(KEY_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save search history", e)
        }
    }

    private fun saveFrequenciesToDisk(map: Map<String, Int>) {
        val p = prefs ?: return
        try {
            val obj = org.json.JSONObject()
            map.forEach { (k, v) -> obj.put(k, v) }
            p.edit().putString(KEY_FREQUENCIES, obj.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save search frequencies", e)
        }
    }
}
