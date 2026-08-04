package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists bookmarked media IDs in SharedPreferences.
 *
 * Initialized once by StreamHubApplication.onCreate(). Callers do NOT pass
 * context to any method — the applicationContext is captured in init().
 *
 * Implementation note: SharedPreferences StringSet does NOT preserve insertion
 * order. If you need ordered bookmarks (e.g. "recently added first"), migrate
 * to a JSON-serialized list in M10. For now, the Set is sufficient for
 * bookmark toggle/lookup.
 */
object MyListManager {

    private const val TAG = "MyListManager"
    private const val PREFS_NAME = "streamhub_my_list"
    private const val KEY_BOOKMARKS = "bookmarked_ids"

    private lateinit var appContext: Context

    private val _myListFlow = MutableStateFlow<Set<String>>(emptySet())
    val myListFlow: StateFlow<Set<String>> = _myListFlow.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val prefs = getPrefs()
        val savedSet = prefs.getStringSet(KEY_BOOKMARKS, emptySet()) ?: emptySet()
        _myListFlow.value = savedSet
    }

    /**
     * Toggle bookmark state for a media item.
     * @return true if the item was added, false if it was removed
     */
    fun toggleBookmark(mediaId: String): Boolean {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "toggleBookmark called before init — no-op")
            return false
        }
        val currentSet = _myListFlow.value.toMutableSet()
        val isAdded: Boolean = if (currentSet.contains(mediaId)) {
            currentSet.remove(mediaId)
            false
        } else {
            currentSet.add(mediaId)
            true
        }

        _myListFlow.value = currentSet
        getPrefs().edit().putStringSet(KEY_BOOKMARKS, currentSet).apply()
        return isAdded
    }

    fun isBookmarked(mediaId: String): Boolean {
        return _myListFlow.value.contains(mediaId)
    }

    private fun getPrefs(): SharedPreferences {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
