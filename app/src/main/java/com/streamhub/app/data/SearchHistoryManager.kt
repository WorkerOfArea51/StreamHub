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
    private const val MAX_HISTORY_ITEMS = 12

    private lateinit var appContext: Context
    private var prefs: SharedPreferences? = null

    private val _historyFlow = MutableStateFlow<List<String>>(emptyList())
    val historyFlow: StateFlow<List<String>> = _historyFlow.asStateFlow()

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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load search history", e)
            _historyFlow.value = emptyList()
        }
    }

    fun addQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || trimmed.startsWith("#")) return

        val current = _historyFlow.value.toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val trimmedList = current.take(MAX_HISTORY_ITEMS)

        _historyFlow.value = trimmedList
        saveToDisk(trimmedList)
    }

    fun removeQuery(query: String) {
        val current = _historyFlow.value.toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        _historyFlow.value = current
        saveToDisk(current)
    }

    fun clearAll() {
        _historyFlow.value = emptyList()
        prefs?.edit()?.remove(KEY_HISTORY)?.apply()
    }

    private fun saveToDisk(list: List<String>) {
        val p = prefs ?: return
        try {
            val array = JSONArray()
            list.forEach { array.put(it) }
            p.edit().putString(KEY_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save search history", e)
        }
    }
}
