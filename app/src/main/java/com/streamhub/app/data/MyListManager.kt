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

    val SYSTEM_COLLECTIONS = linkedSetOf("Watchlist", "Favorites", "Must Watch", "Rewatch")

    private lateinit var appContext: Context

    private val _itemsFlow = MutableStateFlow<Map<String, MyListItem>>(emptyMap())
    val itemsFlow: StateFlow<Map<String, MyListItem>> = _itemsFlow.asStateFlow()

    private val _myListFlow = MutableStateFlow<Set<String>>(emptySet())
    val myListFlow: StateFlow<Set<String>> = _myListFlow.asStateFlow()

    private val _collectionsFlow = MutableStateFlow<Set<String>>(SYSTEM_COLLECTIONS)
    val collectionsFlow: StateFlow<Set<String>> = _collectionsFlow.asStateFlow()

    fun isSystemCollection(collectionName: String): Boolean {
        return SYSTEM_COLLECTIONS.any { it.equals(collectionName.trim(), ignoreCase = true) }
    }

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
        _collectionsFlow.value = (SYSTEM_COLLECTIONS + customCollections).toSet()
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

    fun toggle(mediaId: String): Boolean = toggleBookmark(mediaId)

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

    fun getItemCollection(mediaId: String): String {
        return _itemsFlow.value[mediaId]?.collection ?: "Watchlist"
    }

    fun getItemsInCollectionCount(collectionName: String): Int {
        return _itemsFlow.value.values.count { it.collection.equals(collectionName.trim(), ignoreCase = true) }
    }

    @Synchronized
    fun addCustomCollection(collectionName: String): Boolean {
        if (!::appContext.isInitialized || collectionName.isBlank()) return false
        val trimmed = collectionName.trim()
        if (isSystemCollection(trimmed) || _collectionsFlow.value.any { it.equals(trimmed, ignoreCase = true) }) {
            return false
        }
        val set = _collectionsFlow.value.toMutableSet()
        set.add(trimmed)
        _collectionsFlow.value = set

        val customOnly = set.filterNot { isSystemCollection(it) }.toSet()
        getPrefs().edit().putStringSet(KEY_COLLECTIONS, customOnly).apply()
        return true
    }

    @Synchronized
    fun renameCustomCollection(oldName: String, newName: String): Boolean {
        if (!::appContext.isInitialized || oldName.isBlank() || newName.isBlank()) return false
        val trimmedNew = newName.trim()
        val trimmedOld = oldName.trim()
        if (isSystemCollection(trimmedOld) || isSystemCollection(trimmedNew)) return false
        if (_collectionsFlow.value.any { it.equals(trimmedNew, ignoreCase = true) }) return false

        val set = _collectionsFlow.value.toMutableSet()
        val match = set.firstOrNull { it.equals(trimmedOld, ignoreCase = true) } ?: return false
        set.remove(match)
        set.add(trimmedNew)
        _collectionsFlow.value = set

        val customOnly = set.filterNot { isSystemCollection(it) }.toSet()
        val prefs = getPrefs().edit()
        prefs.putStringSet(KEY_COLLECTIONS, customOnly)

        // Safe migration: update all items in this collection to the new name
        val currentMap = _itemsFlow.value.toMutableMap()
        var hasChanges = false
        currentMap.forEach { (id, item) ->
            if (item.collection.equals(trimmedOld, ignoreCase = true)) {
                val updated = item.copy(collection = trimmedNew)
                currentMap[id] = updated
                saveItemToPrefs(prefs, updated)
                hasChanges = true
            }
        }
        prefs.apply()

        if (hasChanges) {
            _itemsFlow.value = currentMap
        }
        return true
    }

    @Synchronized
    fun deleteCustomCollection(collectionName: String): Boolean {
        if (!::appContext.isInitialized || collectionName.isBlank()) return false
        val trimmed = collectionName.trim()
        if (isSystemCollection(trimmed)) return false

        val set = _collectionsFlow.value.toMutableSet()
        val match = set.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: return false
        set.remove(match)
        _collectionsFlow.value = set

        val customOnly = set.filterNot { isSystemCollection(it) }.toSet()
        val prefs = getPrefs().edit()
        prefs.putStringSet(KEY_COLLECTIONS, customOnly)

        // Safe migration: reassign items from deleted collection back to default "Watchlist"
        val currentMap = _itemsFlow.value.toMutableMap()
        var hasChanges = false
        currentMap.forEach { (id, item) ->
            if (item.collection.equals(trimmed, ignoreCase = true)) {
                val updated = item.copy(collection = "Watchlist")
                currentMap[id] = updated
                saveItemToPrefs(prefs, updated)
                hasChanges = true
            }
        }
        prefs.apply()

        if (hasChanges) {
            _itemsFlow.value = currentMap
        }
        return true
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

    /**
     * Restores watchlist items and custom collections from backup payload.
     * @param items List of MyListItem from backup.
     * @param customCollections Set of user custom folders from backup.
     * @param mergeMode If true, merges with existing; if false, performs clean replacement.
     * @return Number of watchlist items restored.
     */
    @Synchronized
    fun restoreFromBackup(
        items: List<MyListItem>?,
        customCollections: Set<String>?,
        mergeMode: Boolean
    ): Int {
        if (!::appContext.isInitialized) return 0
        val incomingItems = items ?: emptyList()
        val incomingCustom = (customCollections ?: emptySet()).filterNot { isSystemCollection(it) }.toSet()
        val prefs = getPrefs().edit()

        if (mergeMode) {
            val currentMap = _itemsFlow.value.toMutableMap()
            val existingCustom = (_collectionsFlow.value - SYSTEM_COLLECTIONS).toMutableSet()
            existingCustom.addAll(incomingCustom)

            incomingItems.forEach { inc ->
                val existing = currentMap[inc.mediaId]
                if (existing != null) {
                    val merged = existing.copy(
                        isFavorite = existing.isFavorite || inc.isFavorite,
                        collection = if (existing.collection != "Watchlist") existing.collection else inc.collection,
                        addedAt = minOf(existing.addedAt, inc.addedAt)
                    )
                    currentMap[inc.mediaId] = merged
                    saveItemToPrefs(prefs, merged)
                } else {
                    currentMap[inc.mediaId] = inc
                    saveItemToPrefs(prefs, inc)
                }
            }

            prefs.putStringSet(KEY_COLLECTIONS, existingCustom)
            prefs.putStringSet(KEY_BOOKMARKS, currentMap.keys)
            prefs.apply()

            _itemsFlow.value = currentMap
            _myListFlow.value = currentMap.keys
            _collectionsFlow.value = (SYSTEM_COLLECTIONS + existingCustom).toSet()
            return incomingItems.size
        } else {
            // Replace mode: clear previous items from prefs
            _itemsFlow.value.keys.forEach { id ->
                prefs.remove(KEY_ITEM_PREFIX + id)
            }
            val newMap = mutableMapOf<String, MyListItem>()
            incomingItems.forEach { item ->
                newMap[item.mediaId] = item
                saveItemToPrefs(prefs, item)
            }
            prefs.putStringSet(KEY_COLLECTIONS, incomingCustom)
            prefs.putStringSet(KEY_BOOKMARKS, newMap.keys)
            prefs.apply()

            _itemsFlow.value = newMap
            _myListFlow.value = newMap.keys
            _collectionsFlow.value = (SYSTEM_COLLECTIONS + incomingCustom).toSet()
            return incomingItems.size
        }
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
