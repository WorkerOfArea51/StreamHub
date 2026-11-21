package com.streamhub.app.ui.screens.search

import com.streamhub.app.data.models.MediaItem

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the foundational types and intelligence engine for StreamHub Search:
 *
 * 1. [SortOption] - Enumeration of all sorting strategies available in the search catalog
 *    (Latest, Oldest, Rating High/Low, Release Year New/Old, Title A-Z/Z-A).
 *
 * 2. [ScoredMediaItem] - Data holder pairing a [MediaItem] with its calculated relevance score.
 *
 * 3. [SearchRelevanceEvaluator] - Intelligent token-based scoring algorithm. Computes exact,
 *    prefix, and fuzzy relevance across Titles, Synonyms, Franchise names, Studios, Cast, and Genres.
 *
 * 4. [formatTrendingLabel] - Helper function that strips verbose subtitle metadata (like "Season 2",
 *    "Cour 1", "The Series") from titles so trending discovery badges remain clean and readable.
 * =========================================================================================
 */

enum class SortOption(val label: String, val shortLabel: String) {
    LATEST("Latest Added", "Latest"),
    OLDEST("Oldest Added", "Oldest"),
    RATING_DESC("Rating: High to Low", "Rating ↓"),
    RATING_ASC("Rating: Low to High", "Rating ↑"),
    TITLE_ASC("Title: (A - Z)", "A - Z"),
    TITLE_DESC("Title: (Z - A)", "Z - A"),
    YEAR_DESC("Release Year: Newest", "Year ↓"),
    YEAR_ASC("Release Year: Oldest", "Year ↑");

    val isAscending: Boolean
        get() = this == OLDEST || this == RATING_ASC || this == TITLE_DESC || this == YEAR_ASC

    fun toggleDirection(): SortOption = when (this) {
        LATEST -> OLDEST
        OLDEST -> LATEST
        RATING_DESC -> RATING_ASC
        RATING_ASC -> RATING_DESC
        TITLE_ASC -> TITLE_DESC
        TITLE_DESC -> TITLE_ASC
        YEAR_DESC -> YEAR_ASC
        YEAR_ASC -> YEAR_DESC
    }

    fun withDirection(ascending: Boolean): SortOption = when (this) {
        LATEST, OLDEST -> if (ascending) OLDEST else LATEST
        RATING_DESC, RATING_ASC -> if (ascending) RATING_ASC else RATING_DESC
        TITLE_ASC, TITLE_DESC -> if (ascending) TITLE_DESC else TITLE_ASC
        YEAR_DESC, YEAR_ASC -> if (ascending) YEAR_ASC else YEAR_DESC
    }

    val criterionKey: String
        get() = when (this) {
            LATEST, OLDEST -> "DATE"
            RATING_DESC, RATING_ASC -> "RATING"
            TITLE_ASC, TITLE_DESC -> "TITLE"
            YEAR_DESC, YEAR_ASC -> "YEAR"
        }

    companion object {
        fun forCriterion(criterionKey: String, ascending: Boolean = false): SortOption = when (criterionKey) {
            "DATE" -> if (ascending) OLDEST else LATEST
            "RATING" -> if (ascending) RATING_ASC else RATING_DESC
            "TITLE" -> if (ascending) TITLE_DESC else TITLE_ASC
            "YEAR" -> if (ascending) YEAR_ASC else YEAR_DESC
            else -> LATEST
        }
    }
}

/**
 * Pairs a media item with its relevance match score for search query ranking.
 */
data class ScoredMediaItem(
    val item: MediaItem,
    val relevanceScore: Int
)

/**
 * High-performance search relevance engine. Matches user tokens against titles,
 * alternative names, franchises, cast members, and studios with progressive weights.
 */
object SearchRelevanceEvaluator {
    private val STOP_WORDS = setOf(
        "to", "be", "a", "an", "the", "in", "on", "of", "and", "is", "at", "by", "for", "with", "no"
    )

    fun computeScore(item: MediaItem, rawQuery: String): Int {
        val trimmedQuery = rawQuery.trim().lowercase()
        if (trimmedQuery.isEmpty()) return 0

        val queryTokens = trimmedQuery.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        val normQuery = trimmedQuery.replace(Regex("[^a-z0-9]"), "")

        val titleLower = item.title.trim().lowercase()
        val synonymsLower = item.synonyms.trim().lowercase()
        val franchiseLower = item.franchiseTitle.trim().lowercase()
        val seasonTitleLower = item.seasonTitle.trim().lowercase()
        val studioLower = item.studio.trim().lowercase()
        val normTitle = titleLower.replace(Regex("[^a-z0-9]"), "")

        var score = 0

        // 1. Exact 100% Title Match (Punctuation-insensitive)
        if (titleLower == trimmedQuery || (normQuery.isNotEmpty() && normTitle == normQuery)) {
            score += 100_000
        }
        // 2. Title Starts With Full Query
        else if (titleLower.startsWith(trimmedQuery) || (normQuery.isNotEmpty() && normTitle.startsWith(normQuery))) {
            score += 50_000
        }
        // 3. Exact Full Query Phrase in Title
        else if (titleLower.contains(trimmedQuery)) {
            score += 30_000
        }

        // 4. Exact match or phrase in Synonyms (Romaji/Alternative English titles)
        if (synonymsLower == trimmedQuery || (normQuery.isNotEmpty() && synonymsLower.replace(Regex("[^a-z0-9]"), "") == normQuery)) {
            score += 25_000
        } else if (synonymsLower.contains(trimmedQuery)) {
            score += 15_000
        }

        // 5. Multi-Word Token Matching in Title
        if (queryTokens.isNotEmpty()) {
            val titleTokens = titleLower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
            val titleTokensSet = titleTokens.toSet()

            val exactWordMatches = queryTokens.count { token -> titleTokensSet.contains(token) }
            val prefixMatches = queryTokens.count { token ->
                token.length >= 3 && titleTokens.any { titleWord -> titleWord.startsWith(token) }
            }
            val totalHits = (exactWordMatches + prefixMatches).coerceAtMost(queryTokens.size)

            if (totalHits == queryTokens.size) {
                score += 20_000
                if (titleTokens.isNotEmpty()) {
                    val compactnessRatio = (queryTokens.size.toFloat() / titleTokens.size.toFloat()).coerceAtMost(1f)
                    score += (compactnessRatio * 5_000).toInt()
                }
            } else if (queryTokens.size >= 2 && totalHits >= 2 && (totalHits.toFloat() / queryTokens.size) >= 0.5f) {
                val hasMeaningfulMatch = queryTokens.any { token ->
                    token !in STOP_WORDS && token.length >= 3 && (titleTokensSet.contains(token) || titleTokens.any { it.startsWith(token) })
                }
                if (hasMeaningfulMatch) {
                    score += totalHits * 2_500
                }
            }
        }

        // 6. Synonym token matching
        if (queryTokens.isNotEmpty() && synonymsLower.isNotBlank()) {
            val synTokens = synonymsLower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()
            val synHits = queryTokens.count { synTokens.contains(it) }
            if (synHits == queryTokens.size) {
                score += 12_000
            } else if (queryTokens.size >= 2 && synHits >= 2 && (synHits.toFloat() / queryTokens.size) >= 0.5f) {
                val hasMeaningfulMatch = queryTokens.any { it !in STOP_WORDS && it.length >= 3 && synTokens.contains(it) }
                if (hasMeaningfulMatch) {
                    score += synHits * 1_500
                }
            }
        }

        // 7. Season Title Match
        if (seasonTitleLower.isNotBlank()) {
            if (seasonTitleLower.contains(trimmedQuery)) {
                score += 8_000
            } else if (queryTokens.isNotEmpty() && queryTokens.all { seasonTitleLower.contains(it) }) {
                score += 4_000
            }
        }

        // 8. Franchise Title Match
        if (franchiseLower.isNotBlank()) {
            if (franchiseLower == trimmedQuery) {
                score += 10_000
            } else if (franchiseLower.contains(trimmedQuery)) {
                score += 5_000
            } else if (queryTokens.isNotEmpty() && queryTokens.all { franchiseLower.contains(it) }) {
                score += 3_000
            }
        }

        // 9. Studio / Producer Match
        if (studioLower.isNotBlank() && (studioLower == trimmedQuery || studioLower.contains(trimmedQuery))) {
            score += 2_000
        }

        // 10. Cast List Match
        if (item.castList.isNotEmpty() && item.castList.any { castMember ->
            val castLower = castMember.lowercase()
            castLower.contains(trimmedQuery) || (queryTokens.size >= 2 && queryTokens.all { castLower.contains(it) })
        }) {
            score += 1_500
        }

        // 11. Genre Match
        if (item.genres.any { it.equals(trimmedQuery, ignoreCase = true) }) {
            score += 1_000
        }

        return score
    }
}

/**
 * Strips verbose subtitles, season numbers, and bracketed tags to produce clean discovery pills.
 */
fun formatTrendingLabel(title: String): String {
    var cleaned = title
        .replace(Regex("(?i):\\s*Season\\s*\\d+.*"), "")
        .replace(Regex("(?i)-\\s*Season\\s*\\d+.*"), "")
        .replace(Regex("(?i):\\s*The Series.*"), "")
        .replace(Regex("(?i)\\s*Cour\\s*\\d+.*"), "")
        .replace(Regex("(?i)\\s*Part\\s*\\d+.*"), "")
        .replace(Regex("(?i)\\(.*\\)"), "")
        .replace(Regex("(?i)\\[.*\\]"), "")
        .trim()

    if (cleaned.contains(":")) {
        val part = cleaned.split(":").first().trim()
        if (part.length >= 3) cleaned = part
    }
    if (cleaned.contains(" - ")) {
        val part = cleaned.split(" - ").first().trim()
        if (part.length >= 3) cleaned = part
    }

    return cleaned
}
