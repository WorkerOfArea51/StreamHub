package com.streamhub.app.ui.screens.mylist

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * [MyListTypes.kt] defines the data types, filters, sorting options, and color palettes
 * used across the entire My List & Watchlist feature in StreamHub.
 *
 * 1. [MyListStatusCategory]:
 *    - ALL: Shows everything saved in the user's library.
 *    - FAVORITES: Titles the user has starred/favorited with a heart.
 *    - IN_PROGRESS: Titles with active watch progress (>5s watched, not finished).
 *    - WATCH_LATER: Titles saved but not yet started.
 *    - COMPLETED: Titles completely watched (all episodes finished).
 *    - COLLECTIONS: Custom user-created folders (e.g. "Anime Classics", "Weekend Binges").
 *
 * 2. [MyListSortOption]:
 *    - Smart sorting criteria: Date Added, Rating, Release Year, Alphabetical (A-Z).
 *    - Supports bidirectional toggling (High to Low vs Low to High).
 *
 * 3. [MyListTypeFilter]:
 *    - Filters between All Types, Movies only, or TV Series / Anime only.
 * =========================================================================================
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Main status categories represented in the 3x2 Asymmetric Hero Dock.
 */
enum class MyListStatusCategory(val label: String, val icon: ImageVector) {
    ALL("All Saved", Icons.Default.Bookmark),
    FAVORITES("Favorites", Icons.Default.Favorite),
    IN_PROGRESS("In Progress", Icons.Default.PlayCircleOutline),
    WATCH_LATER("Watch Later", Icons.Default.HourglassEmpty),
    COMPLETED("Completed", Icons.Default.CheckCircle),
    COLLECTIONS("Folders", Icons.Default.Folder)
}

/**
 * Sorting criteria with bidirectional ascending/descending controls.
 */
enum class MyListSortOption(val label: String, val shortLabel: String) {
    RECENTLY_ADDED("Recently Saved", "Recent"),
    OLDEST_ADDED("Oldest Saved", "Oldest"),
    RATING_DESC("Rating: High to Low", "Rating \u2193"),
    RATING_ASC("Rating: Low to High", "Rating \u2191"),
    RELEASE_YEAR_DESC("Release Year: Newest", "Year \u2193"),
    RELEASE_YEAR_ASC("Release Year: Oldest", "Year \u2191"),
    ALPHABETICAL_ASC("Title: (A - Z)", "A - Z"),
    ALPHABETICAL_DESC("Title: (Z - A)", "Z - A");

    val isAscending: Boolean
        get() = this == OLDEST_ADDED || this == RATING_ASC || this == RELEASE_YEAR_ASC || this == ALPHABETICAL_DESC

    fun toggleDirection(): MyListSortOption = when (this) {
        RECENTLY_ADDED -> OLDEST_ADDED
        OLDEST_ADDED -> RECENTLY_ADDED
        RATING_DESC -> RATING_ASC
        RATING_ASC -> RATING_DESC
        RELEASE_YEAR_DESC -> RELEASE_YEAR_ASC
        RELEASE_YEAR_ASC -> RELEASE_YEAR_DESC
        ALPHABETICAL_ASC -> ALPHABETICAL_DESC
        ALPHABETICAL_DESC -> ALPHABETICAL_ASC
    }

    fun withDirection(ascending: Boolean): MyListSortOption = when (this) {
        RECENTLY_ADDED, OLDEST_ADDED -> if (ascending) OLDEST_ADDED else RECENTLY_ADDED
        RATING_DESC, RATING_ASC -> if (ascending) RATING_ASC else RATING_DESC
        RELEASE_YEAR_DESC, RELEASE_YEAR_ASC -> if (ascending) RELEASE_YEAR_ASC else RELEASE_YEAR_DESC
        ALPHABETICAL_ASC, ALPHABETICAL_DESC -> if (ascending) ALPHABETICAL_DESC else ALPHABETICAL_ASC
    }

    val criterionKey: String
        get() = when (this) {
            RECENTLY_ADDED, OLDEST_ADDED -> "DATE"
            RATING_DESC, RATING_ASC -> "RATING"
            RELEASE_YEAR_DESC, RELEASE_YEAR_ASC -> "YEAR"
            ALPHABETICAL_ASC, ALPHABETICAL_DESC -> "TITLE"
        }

    companion object {
        fun forCriterion(criterionKey: String, ascending: Boolean = false): MyListSortOption = when (criterionKey) {
            "DATE" -> if (ascending) OLDEST_ADDED else RECENTLY_ADDED
            "RATING" -> if (ascending) RATING_ASC else RATING_DESC
            "YEAR" -> if (ascending) RELEASE_YEAR_ASC else RELEASE_YEAR_DESC
            "TITLE" -> if (ascending) ALPHABETICAL_DESC else ALPHABETICAL_ASC
            else -> RECENTLY_ADDED
        }
    }
}

/**
 * Filter by content category (Movies vs TV/Anime).
 */
enum class MyListTypeFilter(val label: String) {
    ALL("All types"),
    MOVIES("Movies"),
    SERIES("TV & Anime")
}

// Inviolable Figma & Expressive Color Constants
val CreamyPeach = Color(0xFFFFD0B8)
val DarkCreamText = Color(0xFF330A02)
val CoralAccent = Color(0xFFFF725E)
val LavenderPebble = Color(0xFFDDD8FC)
