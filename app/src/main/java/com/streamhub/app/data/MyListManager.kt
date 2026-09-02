package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class MyListItem(
    val mediaId: String,
    val addedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val collection: String = "Watchlist"
)

/**
 * Enhanced Netflix & Crunchyroll Grade MyList & Watchlist Manager.
 *
 * Persists:
 * - Added timestamp for chronological "Recently Added" sorting.
 * - Favorites status.
 * - Custom user collections & folders.
 * - Backward-compatible `myListFlow` for instant lookups.
 */
object MyListManager {

    private const val TAG = "MyListManager"
    private const val PREFS_NAME = "streamhub_my_list"
    private const val KEY_BOOKMARKS = "bookmarked_ids"
    private const val KEY_ITEM_PREFIX = "item_meta_"
    private const val KEY_COLLECTIONS = "user_collections_set"

    private lateinit var appContext: Context

    private val _itemsFlow = MutableStateFlow<Map<String, MyListItem>>(emptyMap())
    val itemsFlow: StateFlow<Map<String, MyListItem>> = _itemsFlow.asStateFlow()

    private val _myListFlow = MutableStateFlow<Set<String>>(emptySet())
    val myListFlow: StateFlow<Set<String>> = _myListFlow.asStateFlow()

    private val _collectionsFlow = MutableStateFlow<Set<String>>(setOf("Watchlist", "Favorites", "Must Watch", "Rewatch"))
    val collectionsFlow: StateFlow<Set<String>> = _collectionsFlow.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val prefs = getPrefs()
        val savedIds = (prefs.getStringSet(KEY_BOOKMARKS, emptySet()) ?: emptySet()).toSet()
        val customCollections = (prefs.getStringSet(KEY_COLLECTIONS, emptySet()) ?: emptySet()).toSet()

        val map = mutableMapOf<String, MyListItem>()
        savedIds.forEach { id ->
            val jsonStr = prefs.getString(KEY_ITEM_PREFIX + id, null)
            if (jsonStr != null) {
                try {
                    val json = JSONObject(jsonStr)
                    map[id] = MyListItem(
                        mediaId = id,
                        addedAt = json.optLong("addedAt", System.currentTimeMillis()),
                        isFavorite = json.optBoolean("isFavorite", false),
                        collection = json.optString("collection", "Watchlist")
                    )
                } catch (e: Exception) {
                    map[id] = MyListItem(mediaId = id)
                }
            } else {
                map[id] = MyListItem(mediaId = id)
            }
        }

        _itemsFlow.value = map
        _myListFlow.value = map.keys
        _collectionsFlow.value = (setOf("Watchlist", "Favorites", "Must Watch", "Rewatch") + customCollections).toSet()
    }

    /**
     * Toggle bookmark state for a media item.
     * @return true if the item was added, false if it was removed
     */
    @Synchronized
    fun toggleBookmark(mediaId: String): Boolean {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "toggleBookmark called before init — no-op")
            return false
        }
        val currentMap = _itemsFlow.value.toMutableMap()
        val prefs = getPrefs().edit()
        val isAdded: Boolean

        if (currentMap.containsKey(mediaId)) {
            currentMap.remove(mediaId)
            prefs.remove(KEY_ITEM_PREFIX + mediaId)
            isAdded = false
        } else {
            val item = MyListItem(mediaId = mediaId, addedAt = System.currentTimeMillis())
            currentMap[mediaId] = item
            saveItemToPrefs(prefs, item)
            isAdded = true
        }

        val idSet = currentMap.keys.toSet()
        _itemsFlow.value = currentMap
        _myListFlow.value = idSet
        prefs.putStringSet(KEY_BOOKMARKS, idSet).apply()
        return isAdded
    }

    @Synchronized
    fun toggleFavorite(mediaId: String): Boolean {
        if (!::appContext.isInitialized) return false
        val currentMap = _itemsFlow.value.toMutableMap()
        val existing = currentMap[mediaId] ?: MyListItem(mediaId = mediaId, addedAt = System.currentTimeMillis())
        val newFav = !existing.isFavorite
        val updated = existing.copy(isFavorite = newFav)
        currentMap[mediaId] = updated

        val prefs = getPrefs().edit()
        saveItemToPrefs(prefs, updated)
        prefs.putStringSet(KEY_BOOKMARKS, currentMap.keys.toSet()).apply()

        _itemsFlow.value = currentMap
        _myListFlow.value = currentMap.keys.toSet()
        return newFav
    }

    fun isBookmarked(mediaId: String): Boolean {
        return _myListFlow.value.contains(mediaId)
    }

    fun isFavorite(mediaId: String): Boolean {
        return _itemsFlow.value[mediaId]?.isFavorite == true
    }

    @Synchronized
    fun setCollection(mediaId: String, collectionName: String) {
        if (!::appContext.isInitialized) return
        val currentMap = _itemsFlow.value.toMutableMap()
        val existing = currentMap[mediaId] ?: MyListItem(mediaId = mediaId, addedAt = System.currentTimeMillis())
        val updated = existing.copy(collection = collectionName)
        currentMap[mediaId] = updated

        val prefs = getPrefs().edit()
        saveItemToPrefs(prefs, updated)
        val idSet = currentMap.keys.toSet()
        prefs.putStringSet(KEY_BOOKMARKS, idSet).apply()

        _itemsFlow.value = currentMap
        _myListFlow.value = idSet
    }

    @Synchronized
    fun removeFromList(mediaId: String) {
        if (!::appContext.isInitialized) return
        val currentMap = _itemsFlow.value.toMutableMap()
        if (currentMap.containsKey(mediaId)) {
            currentMap.remove(mediaId)
            val prefs = getPrefs().edit()
            prefs.remove(KEY_ITEM_PREFIX + mediaId)
            val idSet = currentMap.keys.toSet()
            prefs.putStringSet(KEY_BOOKMARKS, idSet).apply()
            _itemsFlow.value = currentMap
            _myListFlow.value = idSet
        }
    }

    @Synchronized
    fun addCustomCollection(collectionName: String) {
        if (!::appContext.isInitialized || collectionName.isBlank()) return
        val set = _collectionsFlow.value.toMutableSet()
        set.add(collectionName.trim())
        _collectionsFlow.value = set

        getPrefs().edit().putStringSet(KEY_COLLECTIONS, set).apply()
    }

    @Synchronized
    fun removeCompletedItems(completedMediaIds: Set<String>) {
        if (!::appContext.isInitialized || completedMediaIds.isEmpty()) return
        val currentMap = _itemsFlow.value.toMutableMap()
        val prefs = getPrefs().edit()

        completedMediaIds.forEach { id ->
            currentMap.remove(id)
            prefs.remove(KEY_ITEM_PREFIX + id)
        }

        val idSet = currentMap.keys.toSet()
        _itemsFlow.value = currentMap
        _myListFlow.value = idSet
        prefs.putStringSet(KEY_BOOKMARKS, idSet).apply()
    }

    @Synchronized
    fun clearAll() {
        if (!::appContext.isInitialized) return
        val prefs = getPrefs().edit()
        _itemsFlow.value.keys.forEach { id ->
            prefs.remove(KEY_ITEM_PREFIX + id)
        }
        prefs.remove(KEY_BOOKMARKS).apply()
        _itemsFlow.value = emptyMap()
        _myListFlow.value = emptySet()
    }

    private fun saveItemToPrefs(editor: SharedPreferences.Editor, item: MyListItem) {
        val json = JSONObject().apply {
            put("mediaId", item.mediaId)
            put("addedAt", item.addedAt)
            put("isFavorite", item.isFavorite)
            put("collection", item.collection)
        }
        editor.putString(KEY_ITEM_PREFIX + item.mediaId, json.toString())
    }

    private fun getPrefs(): SharedPreferences {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
