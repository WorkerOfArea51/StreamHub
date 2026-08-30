package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CatalogSortOrder(val displayName: String, val shortName: String) {
    NEWEST_FIRST("Newest Uploads ⚡", "Newest ⚡"),
    OLDEST_FIRST("Oldest Uploads ⏳", "Oldest ⏳"),
    HIGHEST_RATED("Top Rated ⭐", "Top Rated ⭐"),
    RELEASE_YEAR("Release Year 📅", "Year 📅"),
    ALPHABETICAL("Alphabetical (A - Z)", "A - Z")
}

data class HomeLayoutConfig(
    val showHeroCarousel: Boolean = true,
    val showContinueWatching: Boolean = true,
    val continueWatchingFirst: Boolean = false,
    val showRecentlyAdded: Boolean = true,
    val showBecauseYouWatched: Boolean = true,
    val showTrendingSection: Boolean = true,
    val showCategoryShelves: Boolean = true,
    val showMicroGenreShelves: Boolean = true,
    val showAnimeSection: Boolean = true,
    val showMoviesSection: Boolean = true,
    val catalogSortOrder: CatalogSortOrder = CatalogSortOrder.NEWEST_FIRST
)

/**
 * Production Home Screen Layout Preferences Manager:
 * - Allows users to customize Home Screen section order, sorting, and visibility
 * - Persists preferences in SharedPreferences (streamhub_home_layout_prefs)
 */
object HomeScreenLayoutManager {

    private const val PREFS_NAME = "streamhub_home_layout_prefs"
    private const val KEY_SHOW_HERO = "show_hero"
    private const val KEY_SHOW_CONTINUE = "show_continue"
    private const val KEY_CONTINUE_FIRST = "continue_first"
    private const val KEY_SHOW_RECENTLY_ADDED = "show_recently_added"
    private const val KEY_SHOW_BECAUSE_WATCHED = "show_because_watched"
    private const val KEY_SHOW_TRENDING = "show_trending"
    private const val KEY_SHOW_CATEGORY_SHELVES = "show_category_shelves"
    private const val KEY_SHOW_MICRO_GENRES = "show_micro_genres"
    private const val KEY_SHOW_ANIME = "show_anime"
    private const val KEY_SHOW_MOVIES = "show_movies"
    private const val KEY_SORT_ORDER = "catalog_sort_order"

    private var prefs: SharedPreferences? = null

    private val _layoutConfig = MutableStateFlow(HomeLayoutConfig())
    val layoutConfig: StateFlow<HomeLayoutConfig> = _layoutConfig.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val p = prefs ?: return
        try {
            val sortOrderName = p.getString(KEY_SORT_ORDER, CatalogSortOrder.NEWEST_FIRST.name) ?: CatalogSortOrder.NEWEST_FIRST.name
            val sortOrder = runCatching { CatalogSortOrder.valueOf(sortOrderName) }.getOrDefault(CatalogSortOrder.NEWEST_FIRST)

            _layoutConfig.value = HomeLayoutConfig(
                showHeroCarousel = p.getBoolean(KEY_SHOW_HERO, true),
                showContinueWatching = p.getBoolean(KEY_SHOW_CONTINUE, true),
                continueWatchingFirst = p.getBoolean(KEY_CONTINUE_FIRST, false),
                showRecentlyAdded = p.getBoolean(KEY_SHOW_RECENTLY_ADDED, true),
                showBecauseYouWatched = p.getBoolean(KEY_SHOW_BECAUSE_WATCHED, true),
                showTrendingSection = p.getBoolean(KEY_SHOW_TRENDING, true),
                showCategoryShelves = p.getBoolean(KEY_SHOW_CATEGORY_SHELVES, true),
                showMicroGenreShelves = p.getBoolean(KEY_SHOW_MICRO_GENRES, true),
                showAnimeSection = p.getBoolean(KEY_SHOW_ANIME, true),
                showMoviesSection = p.getBoolean(KEY_SHOW_MOVIES, true),
                catalogSortOrder = sortOrder
            )
        } catch (e: Exception) {
            p.edit().clear().apply()
            _layoutConfig.value = HomeLayoutConfig()
        }
    }

    fun updateConfig(newConfig: HomeLayoutConfig) {
        _layoutConfig.value = newConfig
        prefs?.edit()?.apply {
            putBoolean(KEY_SHOW_HERO, newConfig.showHeroCarousel)
            putBoolean(KEY_SHOW_CONTINUE, newConfig.showContinueWatching)
            putBoolean(KEY_CONTINUE_FIRST, newConfig.continueWatchingFirst)
            putBoolean(KEY_SHOW_RECENTLY_ADDED, newConfig.showRecentlyAdded)
            putBoolean(KEY_SHOW_BECAUSE_WATCHED, newConfig.showBecauseYouWatched)
            putBoolean(KEY_SHOW_TRENDING, newConfig.showTrendingSection)
            putBoolean(KEY_SHOW_CATEGORY_SHELVES, newConfig.showCategoryShelves)
            putBoolean(KEY_SHOW_MICRO_GENRES, newConfig.showMicroGenreShelves)
            putBoolean(KEY_SHOW_ANIME, newConfig.showAnimeSection)
            putBoolean(KEY_SHOW_MOVIES, newConfig.showMoviesSection)
            putString(KEY_SORT_ORDER, newConfig.catalogSortOrder.name)
            apply()
        }
    }

    fun setSortOrder(order: CatalogSortOrder) {
        updateConfig(_layoutConfig.value.copy(catalogSortOrder = order))
    }
}
