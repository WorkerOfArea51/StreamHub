package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CatalogSortOrder(val displayName: String, val shortName: String) {
    NEWEST_FIRST("Newest Uploads", "Newest"),
    OLDEST_FIRST("Oldest Uploads", "Oldest"),
    HIGHEST_RATED("Rating: High to Low", "Rating ↓"),
    LOWEST_RATED("Rating: Low to High", "Rating ↑"),
    RELEASE_YEAR("Release Year: Newest", "Year ↓"),
    RELEASE_YEAR_ASC("Release Year: Oldest", "Year ↑"),
    ALPHABETICAL("Title: (A - Z)", "A - Z"),
    ALPHABETICAL_DESC("Title: (Z - A)", "Z - A");

    val shortLabel: String get() = shortName

    val isAscending: Boolean
        get() = this == OLDEST_FIRST || this == LOWEST_RATED || this == RELEASE_YEAR_ASC || this == ALPHABETICAL_DESC

    fun toggleDirection(): CatalogSortOrder = when (this) {
        NEWEST_FIRST -> OLDEST_FIRST
        OLDEST_FIRST -> NEWEST_FIRST
        HIGHEST_RATED -> LOWEST_RATED
        LOWEST_RATED -> HIGHEST_RATED
        RELEASE_YEAR -> RELEASE_YEAR_ASC
        RELEASE_YEAR_ASC -> RELEASE_YEAR
        ALPHABETICAL -> ALPHABETICAL_DESC
        ALPHABETICAL_DESC -> ALPHABETICAL
    }

    fun withDirection(ascending: Boolean): CatalogSortOrder = when (this) {
        NEWEST_FIRST, OLDEST_FIRST -> if (ascending) OLDEST_FIRST else NEWEST_FIRST
        HIGHEST_RATED, LOWEST_RATED -> if (ascending) LOWEST_RATED else HIGHEST_RATED
        RELEASE_YEAR, RELEASE_YEAR_ASC -> if (ascending) RELEASE_YEAR_ASC else RELEASE_YEAR
        ALPHABETICAL, ALPHABETICAL_DESC -> if (ascending) ALPHABETICAL_DESC else ALPHABETICAL
    }

    val criterionKey: String
        get() = when (this) {
            NEWEST_FIRST, OLDEST_FIRST -> "DATE"
            HIGHEST_RATED, LOWEST_RATED -> "RATING"
            RELEASE_YEAR, RELEASE_YEAR_ASC -> "YEAR"
            ALPHABETICAL, ALPHABETICAL_DESC -> "TITLE"
        }

    companion object {
        fun forCriterion(criterionKey: String, ascending: Boolean = false): CatalogSortOrder = when (criterionKey) {
            "DATE" -> if (ascending) OLDEST_FIRST else NEWEST_FIRST
            "RATING" -> if (ascending) LOWEST_RATED else HIGHEST_RATED
            "YEAR" -> if (ascending) RELEASE_YEAR_ASC else RELEASE_YEAR
            "TITLE" -> if (ascending) ALPHABETICAL_DESC else ALPHABETICAL
            else -> NEWEST_FIRST
        }

        fun fromLegacyOrName(name: String?): CatalogSortOrder = when (name) {
            "RELEASE_YEAR_DESC" -> RELEASE_YEAR
            "ALPHABETICAL_ASC" -> ALPHABETICAL
            null -> NEWEST_FIRST
            else -> runCatching { valueOf(name) }.getOrDefault(NEWEST_FIRST)
        }
    }
}

enum class PosterSize(val displayName: String, val cardWidthDp: Int, val landscapeWidthDp: Int) {
    COMPACT("Compact", 105, 145),
    REGULAR("Regular", 120, 168),
    LARGE("Large", 140, 195)
}

enum class PosterLayout(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
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
    val catalogSortOrder: CatalogSortOrder = CatalogSortOrder.NEWEST_FIRST,
    val catalogGridColumns: Int = 3,
    val posterSize: PosterSize = PosterSize.REGULAR,
    val posterLayout: PosterLayout = PosterLayout.PORTRAIT,
    val homeShelfRows: Int = 1
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
    private const val KEY_SELECTED_CATEGORY = "selected_category_filter"
    private const val KEY_CATALOG_GRID_COLUMNS = "catalog_grid_columns"
    private const val KEY_POSTER_SIZE = "poster_size"
    private const val KEY_POSTER_LAYOUT = "poster_layout"
    private const val KEY_HOME_SHELF_ROWS = "home_shelf_rows"

    private var prefs: SharedPreferences? = null

    private val _layoutConfig = MutableStateFlow(HomeLayoutConfig())
    val layoutConfig: StateFlow<HomeLayoutConfig> = _layoutConfig.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("ALL")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val p = prefs ?: return
        try {
            val sortOrderName = p.getString(KEY_SORT_ORDER, CatalogSortOrder.NEWEST_FIRST.name)
            val sortOrder = CatalogSortOrder.fromLegacyOrName(sortOrderName)

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
                catalogSortOrder = sortOrder,
                catalogGridColumns = p.getInt(KEY_CATALOG_GRID_COLUMNS, 3).coerceIn(2, 4),
                posterSize = when (p.getString(KEY_POSTER_SIZE, "REGULAR")?.uppercase()) {
                    "COMPACT" -> PosterSize.COMPACT
                    "LARGE" -> PosterSize.LARGE
                    else -> PosterSize.REGULAR
                },
                posterLayout = when (p.getString(KEY_POSTER_LAYOUT, "PORTRAIT")?.uppercase()) {
                    "LANDSCAPE" -> PosterLayout.LANDSCAPE
                    else -> PosterLayout.PORTRAIT
                },
                homeShelfRows = p.getInt(KEY_HOME_SHELF_ROWS, 1).coerceIn(1, 2)
            )
            _selectedCategoryFilter.value = p.getString(KEY_SELECTED_CATEGORY, "ALL") ?: "ALL"
        } catch (e: Exception) {
            p.edit().clear().apply()
            _layoutConfig.value = HomeLayoutConfig()
            _selectedCategoryFilter.value = "ALL"
        }
    }

    fun setSelectedCategoryFilter(category: String) {
        val sanitized = category.uppercase().trim().ifBlank { "ALL" }
        _selectedCategoryFilter.value = sanitized
        prefs?.edit()?.putString(KEY_SELECTED_CATEGORY, sanitized)?.apply()
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
            putInt(KEY_CATALOG_GRID_COLUMNS, newConfig.catalogGridColumns)
            putString(KEY_POSTER_SIZE, newConfig.posterSize.name)
            putString(KEY_POSTER_LAYOUT, newConfig.posterLayout.name)
            putInt(KEY_HOME_SHELF_ROWS, newConfig.homeShelfRows)
            apply()
        }
    }

    fun setSortOrder(order: CatalogSortOrder) {
        updateConfig(_layoutConfig.value.copy(catalogSortOrder = order))
    }

    fun updateHeroCarousel(enabled: Boolean) = updateConfig(_layoutConfig.value.copy(showHeroCarousel = enabled))
    fun updateContinueWatching(enabled: Boolean) = updateConfig(_layoutConfig.value.copy(showContinueWatching = enabled))
    fun updateContinueWatchingFirst(enabled: Boolean) = updateConfig(_layoutConfig.value.copy(continueWatchingFirst = enabled))
    fun updateTrendingSection(enabled: Boolean) = updateConfig(_layoutConfig.value.copy(showTrendingSection = enabled))
    fun updateTrending(enabled: Boolean) = updateTrendingSection(enabled)
    fun updateCategoryShelves(enabled: Boolean) = updateConfig(_layoutConfig.value.copy(showCategoryShelves = enabled))
    fun updateMicroGenres(enabled: Boolean) = updateConfig(_layoutConfig.value.copy(showMicroGenreShelves = enabled))
    fun updateAnime(enabled: Boolean) = updateConfig(_layoutConfig.value.copy(showAnimeSection = enabled))
    fun updateMovies(enabled: Boolean) = updateConfig(_layoutConfig.value.copy(showMoviesSection = enabled))
    fun updateRecentlyAdded(enabled: Boolean) = updateConfig(_layoutConfig.value.copy(showRecentlyAdded = enabled))
    fun updateBecauseYouWatched(enabled: Boolean) = updateConfig(_layoutConfig.value.copy(showBecauseYouWatched = enabled))
    fun updateSortOrder(order: CatalogSortOrder) = setSortOrder(order)
    fun updateCatalogGridColumns(columns: Int) = updateConfig(_layoutConfig.value.copy(catalogGridColumns = columns.coerceIn(2, 4)))
    fun updatePosterSize(size: PosterSize) = updateConfig(_layoutConfig.value.copy(posterSize = size))
    fun updatePosterLayout(layout: PosterLayout) = updateConfig(_layoutConfig.value.copy(posterLayout = layout))
    fun updateHomeShelfRows(rows: Int) = updateConfig(_layoutConfig.value.copy(homeShelfRows = rows.coerceIn(1, 2)))
}
