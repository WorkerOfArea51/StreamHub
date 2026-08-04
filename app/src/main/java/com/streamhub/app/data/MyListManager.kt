package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MyListManager {
    private const val PREFS_NAME = "streamhub_my_list"
    private const val KEY_BOOKMARKS = "bookmarked_ids"

    private val _myListFlow = MutableStateFlow<Set<String>>(emptySet())
    val myListFlow: StateFlow<Set<String>> = _myListFlow.asStateFlow()

    fun init(context: Context) {
        val prefs = getPrefs(context)
        val savedSet = prefs.getStringSet(KEY_BOOKMARKS, emptySet()) ?: emptySet()
        _myListFlow.value = savedSet
    }

    fun toggleBookmark(context: Context, mediaId: String): Boolean {
        val currentSet = _myListFlow.value.toMutableSet()
        val isAdded: Boolean
        if (currentSet.contains(mediaId)) {
            currentSet.remove(mediaId)
            isAdded = false
        } else {
            currentSet.add(mediaId)
            isAdded = true
        }

        _myListFlow.value = currentSet
        getPrefs(context).edit().putStringSet(KEY_BOOKMARKS, currentSet).apply()
        return isAdded
    }

    fun isBookmarked(mediaId: String): Boolean {
        return _myListFlow.value.contains(mediaId)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
