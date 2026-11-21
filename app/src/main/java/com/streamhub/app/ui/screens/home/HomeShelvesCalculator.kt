package com.streamhub.app.ui.screens.home

import com.streamhub.app.data.CatalogSortOrder
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.data.models.matchesCategory
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.data.models.isActivelyTrending

/**
 * 🗄️ WHAT IS THIS FILE?
 * This is the Shelf Organization Engine for the Home Screen.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * It takes the raw list of all 554+ shows from the database and intelligently sorts them into:
 * 1. Top Featured Carousel (5 titles)
 * 2. Continue Watching (shows you started but haven't finished)
 * 3. "Because You Watched [Title]" personalized recommendations
 * 4. Trending Now (titles with active 7-day hype)
 * 5. Recently Added (titles uploaded in the last 72 hours)
 * 6. Category Shelves (Anime, Movies, Series, sorted with newest category on top)
 * 7. Micro-Genre Shelves (Action, Fantasy, Mystery, Romance, Sci-Fi, Comedy)
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Used by: `HomeScreen.kt` to compute what to draw on screen.
 * - Respects the user's active filter (ALL, MOVIES, ANIME, SERIES) and sort order (Newest, Rating, A-Z).
 */
data class CategoryShelf(
    val key: String,
    val title: String,
    val items: List<MediaItem>,
    val newestIndex: Int
)

data class HomeShelvesData(
    val sortedCatalog: List<MediaItem>,
    val filteredCatalog: List<MediaItem>,
    val featuredItems: List<MediaItem>,
    val continueWatchingList: List<Pair<MediaItem, PlaybackProgress>>,
    val recentlyAddedItems: List<MediaItem>,
    val becauseYouWatchedData: Pair<String, List<MediaItem>>?,
    val microGenreShelves: List<Pair<String, List<MediaItem>>>,
    val trendingItems: List<MediaItem>,
    val dynamicCategoryShelves: List<CategoryShelf>
)

object HomeShelvesCalculator {

    /**
     * Sorts the raw catalog according to the user's selected sort criterion.
     */
    fun sortCatalog(
        catalog: List<MediaItem>,
        sortOrder: CatalogSortOrder
    ): List<MediaItem> {
        return when (sortOrder) {
            CatalogSortOrder.NEWEST_FIRST -> {
                catalog.sortedWith(
                    compareByDescending<MediaItem> { it.createdAt }
                        .thenByDescending { it.releaseYear.toIntOrNull() ?: 0 }
                        .thenByDescending { it.id }
                )
            }
            CatalogSortOrder.OLDEST_FIRST -> {
                catalog.sortedWith(
                    compareBy<MediaItem> { if (it.createdAt > 0L) it.createdAt else Long.MAX_VALUE }
                        .thenBy { it.releaseYear.toIntOrNull() ?: 9999 }
                        .thenBy { it.id }
                )
            }
            CatalogSortOrder.HIGHEST_RATED -> catalog.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            CatalogSortOrder.LOWEST_RATED -> catalog.sortedWith(
                compareBy<MediaItem> { val r = it.rating.toDoubleOrNull(); if (r != null && r > 0.0) r else 999.0 }
                    .thenBy { it.title.lowercase() }
            )
            CatalogSortOrder.RELEASE_YEAR -> catalog.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
            CatalogSortOrder.RELEASE_YEAR_ASC -> catalog.sortedWith(
                compareBy<MediaItem> { val y = it.releaseYear.toIntOrNull(); if (y != null && y > 1900) y else 9999 }
                    .thenBy { it.title.lowercase() }
            )
            CatalogSortOrder.ALPHABETICAL -> catalog.sortedBy { it.title.lowercase().trim() }
            CatalogSortOrder.ALPHABETICAL_DESC -> catalog.sortedByDescending { it.title.lowercase().trim() }
        }
    }

    /**
     * Filters the sorted catalog based on the active category pill ("ALL", "ANIME", "MOVIES", "SERIES").
     */
    fun filterCatalog(
        sortedCatalog: List<MediaItem>,
        selectedCategoryFilter: String
    ): List<MediaItem> {
        return if (selectedCategoryFilter == "ALL") sortedCatalog
        else sortedCatalog.filter { it.matchesCategory(selectedCategoryFilter) }
    }

    /**
     * Picks up to 5 featured titles for the top sliding Hero Banner.
     */
    fun calculateFeatured(filteredCatalog: List<MediaItem>): List<MediaItem> {
        val explicitFeatured = filteredCatalog.filter { it.isFeatured }
        val pool = (explicitFeatured + filteredCatalog).distinctBy { it.id }
        return pool.sortedWith(
            compareByDescending<MediaItem> { it.createdAt }
                .thenByDescending { it.id }
        ).take(5)
    }

    /**
     * Finds incomplete movies or episodes that the user recently started watching.
     */
    fun calculateContinueWatching(
        catalog: List<MediaItem>,
        watchHistoryMap: Map<String, PlaybackProgress>,
        selectedCategoryFilter: String
    ): List<Pair<MediaItem, PlaybackProgress>> {
        val catalogMap = catalog.associateBy { it.id }
        return watchHistoryMap.values
            .mapNotNull { progress ->
                val media = catalogMap[progress.mediaId] ?: return@mapNotNull null
                val isMatchingCategory = media.matchesCategory(selectedCategoryFilter)
                if (!isMatchingCategory) return@mapNotNull null
                val isMovie = media.category.equals("Movie", ignoreCase = true) ||
                        media.category.equals("Movies", ignoreCase = true) ||
                        progress.mediaType.equals("Movie", ignoreCase = true) ||
                        progress.mediaType.equals("Movies", ignoreCase = true) ||
                        media.episodes.size <= 1
                val isCompleted = if (isMovie) {
                    progress.isFullyCompleted(1) || (progress.durationMs > 0 && progress.positionMs >= (progress.durationMs * 0.92f))
                } else {
                    progress.isFullyCompleted(media.episodes.size)
                }
                if (isCompleted) null else Pair(media, progress)
            }
            .sortedByDescending { it.second.lastUpdated }
    }

    /**
     * Generates titles uploaded in the last 72 hours.
     */
    fun calculateRecentlyAdded(filteredCatalog: List<MediaItem>): List<MediaItem> {
        val now = System.currentTimeMillis()
        val threeDaysAgo = now - (3L * 24L * 60L * 60L * 1000L)
        return filteredCatalog.filter { it.createdAt > 0L && it.createdAt in (threeDaysAgo..now) }
    }

    /**
     * Finds smart recommendations based on the genres of the last show the user watched.
     */
    fun calculateBecauseYouWatched(
        catalog: List<MediaItem>,
        watchHistoryMap: Map<String, PlaybackProgress>,
        filteredCatalog: List<MediaItem>
    ): Pair<String, List<MediaItem>>? {
        val lastWatchedProgress = watchHistoryMap.values.maxByOrNull { it.lastUpdated } ?: return null
        val sourceMedia = catalog.find { it.id == lastWatchedProgress.mediaId } ?: return null
        val sourceGenres = sourceMedia.genres.map { it.trim().lowercase() }.filter { it.isNotBlank() }

        val candidates = filteredCatalog.filter { it.id != sourceMedia.id }.mapNotNull { candidate ->
            var score = 0
            if (candidate.franchiseId.isNotBlank() && candidate.franchiseId == sourceMedia.franchiseId) {
                score += 4
            }
            val candidateGenres = candidate.genres.map { it.trim().lowercase() }
            val commonGenres = candidateGenres.intersect(sourceGenres.toSet()).size
            score += commonGenres * 2

            if (candidate.studio.isNotBlank() && candidate.studio.equals(sourceMedia.studio, ignoreCase = true)) {
                score += 1
            }
            if ((candidate.rating.toDoubleOrNull() ?: 0.0) >= 7.5) {
                score += 1
            }

            if (score >= 2) Pair(candidate, score) else null
        }.sortedWith(compareByDescending<Pair<MediaItem, Int>> { it.second }.thenByDescending { it.first.rating.toDoubleOrNull() ?: 0.0 })
        .map { it.first }

        return if (candidates.size >= 2) {
            Pair("💡 Because You Watched ${sourceMedia.title}", candidates.take(12))
        } else null
    }

    /**
     * Groups shows into themed micro-genres (Action, Fantasy, Mystery, Romance, Sci-Fi, Comedy).
     */
    fun calculateMicroGenres(filteredCatalog: List<MediaItem>): List<Pair<String, List<MediaItem>>> {
        val shelves = mutableListOf<Pair<String, List<MediaItem>>>()

        fun addGenreShelf(title: String, vararg keywords: String) {
            val matching = filteredCatalog.filter { item ->
                val allText = (item.genres + listOf(item.category, item.description)).joinToString(" ").lowercase()
                keywords.any { kw -> allText.contains(kw.lowercase()) }
            }.distinctBy { it.id }

            if (matching.size >= 2) {
                shelves.add(Pair(title, matching))
            }
        }

        val acclaimed = filteredCatalog.filter { (it.rating.toDoubleOrNull() ?: 0.0) >= 8.0 }
            .sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
        if (acclaimed.size >= 2) {
            shelves.add(Pair("⭐ Critically Acclaimed (8.0+)", acclaimed))
        }

        addGenreShelf("💥 Action & High Stakes", "action", "adventure", "martial arts", "military")
        addGenreShelf("🔮 Fantasy & Supernatural", "fantasy", "supernatural", "magic", "isekai", "super power", "mythology")
        addGenreShelf("🕵️ Mystery & Thrillers", "mystery", "thriller", "psychological", "crime", "detective", "suspense")
        addGenreShelf("💖 Romance & Drama", "romance", "drama", "school", "shoujo", "slice of life")
        addGenreShelf("🚀 Sci-Fi & Cyberpunk", "sci-fi", "science fiction", "mecha", "space", "cyberpunk")
        addGenreShelf("😂 Comedy & Satire", "comedy", "parody", "gag", "funny")

        return shelves
    }

    /**
     * Calculates trending titles and ranks Movies, Anime, and Series shelves dynamically.
     */
    fun calculateTrendingAndCategories(filteredCatalog: List<MediaItem>): Pair<List<MediaItem>, List<CategoryShelf>> {
        val trending = mutableListOf<MediaItem>()
        val anime = mutableListOf<MediaItem>()
        val movies = mutableListOf<MediaItem>()
        val series = mutableListOf<MediaItem>()

        var firstAnimeIndex = Int.MAX_VALUE
        var firstMovieIndex = Int.MAX_VALUE
        var firstSeriesIndex = Int.MAX_VALUE

        filteredCatalog.forEachIndexed { index, item ->
            if (item.isActivelyTrending()) trending.add(item)
            if (item.matchesCategory("ANIME")) {
                anime.add(item)
                if (firstAnimeIndex == Int.MAX_VALUE) firstAnimeIndex = index
            } else if (item.matchesCategory("MOVIES")) {
                movies.add(item)
                if (firstMovieIndex == Int.MAX_VALUE) firstMovieIndex = index
            } else if (item.matchesCategory("SERIES")) {
                series.add(item)
                if (firstSeriesIndex == Int.MAX_VALUE) firstSeriesIndex = index
            }
        }

        trending.sortWith(
            compareByDescending<MediaItem> {
                if (it.trendingAt > 0L) it.trendingAt
                else if (it.updatedAt > 0L) it.updatedAt
                else it.createdAt
            }.thenByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
        )

        val shelves = mutableListOf<CategoryShelf>()
        if (movies.isNotEmpty()) {
            shelves.add(CategoryShelf("MOVIES", "🎬 Blockbuster Movies", movies, firstMovieIndex))
        }
        if (anime.isNotEmpty()) {
            shelves.add(CategoryShelf("ANIME", "🌸 Top Rated Anime", anime, firstAnimeIndex))
        }
        if (series.isNotEmpty()) {
            shelves.add(CategoryShelf("SERIES", "📺 Popular Web Series", series, firstSeriesIndex))
        }

        // Dynamically rank shelves so the category with the most recently added title appears on top
        shelves.sortBy { it.newestIndex }

        return Pair(trending, shelves)
    }
}
