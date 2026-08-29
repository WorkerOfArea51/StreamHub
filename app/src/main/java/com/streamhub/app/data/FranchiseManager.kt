package com.streamhub.app.data

import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import java.util.Locale

/**
 * Representation of a season, arc, or sequel option for the interactive selector.
 */
data class SeasonArcOption(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val shortLabel: String = "",
    val badge: String = "",
    val episodeCount: Int = 0,
    val isExternalMedia: Boolean = false,
    val targetMediaItem: MediaItem? = null,
    val internalSeasonNumber: Int = 1,
    val internalArcName: String = "",
    val isCurrent: Boolean = false
)

/**
 * Utility for intelligent Franchise & Sequel discovery, chronological grouping,
 * and multi-arc/saga organization.
 */
object FranchiseManager {

    /**
     * Extracts or generates a normalized franchise slug ID.
     * e.g., "Solo Leveling Season 2: Arise from the Shadow" -> "solo-leveling"
     * "Naruto Shippuden" -> "naruto"
     * "Bleach: Thousand-Year Blood War" -> "bleach"
     */
    fun getFranchiseId(item: MediaItem): String {
        if (item.franchiseId.isNotBlank()) {
            return item.franchiseId.trim().lowercase(Locale.ROOT).replace(" ", "-")
        }

        return normalizeTitleToFranchiseSlug(item.title)
    }

    /**
     * Extracts a clean human-readable franchise title.
     * e.g. "Solo Leveling", "Naruto", "Attack on Titan"
     */
    fun getFranchiseTitle(item: MediaItem): String {
        if (item.franchiseTitle.isNotBlank()) {
            return item.franchiseTitle.trim()
        }

        val raw = item.title.trim()
        val cleaned = cleanFranchiseBaseTitle(raw)
        return cleaned.ifBlank { raw }
    }

    /**
     * Finds all MediaItems in the catalog belonging to the same franchise as [currentItem].
     * Returns them sorted chronologically:
     * - seasonNumber ascending (1, 2, 3...)
     * - releaseYear ascending (2024, 2025...)
     * - Movies / Specials placed contextually or at end
     */
    fun getFranchiseItems(currentItem: MediaItem, catalog: List<MediaItem>): List<MediaItem> {
        val targetFranchiseId = getFranchiseId(currentItem)
        if (targetFranchiseId.isBlank()) return listOf(currentItem)

        val matched = catalog.filter { candidate ->
            val candidateFranchiseId = getFranchiseId(candidate)
            candidateFranchiseId == targetFranchiseId || 
                (candidate.relatedMediaIds.contains(currentItem.id) || currentItem.relatedMediaIds.contains(candidate.id))
        }

        if (matched.isEmpty()) return listOf(currentItem)

        return matched.sortedWith(
            compareBy<MediaItem> { item ->
                // Sort main numbered seasons first (1, 2, 3), movies/specials with season 0 after
                if (item.seasonNumber > 0) item.seasonNumber else 999
            }.thenBy { item ->
                item.releaseYear.toIntOrNull() ?: 9999
            }.thenBy { item ->
                item.title
            }
        )
    }

    fun getEffectiveSeasonNumber(item: MediaItem): Int {
        if (item.seasonNumber > 1) return item.seasonNumber
        val detected = detectSeasonNumber(item.title)
        return if (detected > 1) detected else (item.seasonNumber.takeIf { it > 0 } ?: 1)
    }

    fun detectChapterOrPartNumber(title: String): Int? {
        val regex = Regex("""(?i)(?:chapter|part|vol|volume|season|\bch\b|\bpt\b)\s*[-:]?\s*0*(\d+)""")
        val match = regex.find(title)
        if (match != null) {
            return match.groupValues[1].toIntOrNull()
        }
        val romanRegex = Regex("""(?i)\b(II|III|IV|V|VI|VII|VIII|IX|X)\b\s*$""")
        val romanMatch = romanRegex.find(title.trim())
        if (romanMatch != null) {
            return when (romanMatch.groupValues[1].uppercase(Locale.ROOT)) {
                "II" -> 2
                "III" -> 3
                "IV" -> 4
                "V" -> 5
                "VI" -> 6
                "VII" -> 7
                "VIII" -> 8
                "IX" -> 9
                "X" -> 10
                else -> null
            }
        }
        return null
    }

    fun getChronologicalScore(item: MediaItem): Double {
        val chapterNum = detectChapterOrPartNumber(item.title)
        if (chapterNum != null) return chapterNum.toDouble()

        val seasonNum = getEffectiveSeasonNumber(item)
        if (seasonNum > 1) return seasonNum.toDouble()

        val year = item.releaseYear.trim().toIntOrNull()
        if (year != null && year > 1900) return year.toDouble()

        return 1.0
    }

    /**
     * Formats a relation tag for displaying on season cards.
     * e.g. "CURRENT", "SEQUEL", "PREQUEL", "MOVIE", "SIDE STORY", "SEASON 2"
     */
    fun getFranchiseTag(candidate: MediaItem, currentItem: MediaItem): String {
        if (candidate.id == currentItem.id) return "CURRENT"

        val explicitRelation = candidate.relationType.trim().uppercase(Locale.ROOT)
        if (explicitRelation in listOf("SIDE STORY", "SPIN-OFF", "SPINOFF", "OVA", "SPECIAL")) {
            return explicitRelation
        }

        val isMovieCandidate = candidate.category.equals("MOVIE", ignoreCase = true) || candidate.type.equals("MOVIE", ignoreCase = true)
        val isMovieCurrent = currentItem.category.equals("MOVIE", ignoreCase = true) || currentItem.type.equals("MOVIE", ignoreCase = true)

        if (isMovieCandidate && !isMovieCurrent) {
            return "MOVIE"
        }

        val candidateRank = getChronologicalScore(candidate)
        val currentRank = getChronologicalScore(currentItem)

        return when {
            candidateRank > currentRank -> "SEQUEL"
            candidateRank < currentRank -> "PREQUEL"
            else -> {
                val candYear = candidate.releaseYear.toIntOrNull() ?: 0
                val currYear = currentItem.releaseYear.toIntOrNull() ?: 0
                if (candYear > 0 && currYear > 0) {
                    if (candYear > currYear) "SEQUEL" else if (candYear < currYear) "PREQUEL" else "RELATED"
                } else {
                    "RELATED"
                }
            }
        }
    }

    /**
     * Computes the display title for a franchise card.
     * e.g. "2017 • 122m" for movies, "Season 1 • 2024 • 12 Eps" for series.
     */
    fun getSeasonCardSubtitle(item: MediaItem): String {
        val isMovie = item.category.equals("MOVIE", ignoreCase = true) || item.type.equals("MOVIE", ignoreCase = true)
        val sNum = getEffectiveSeasonNumber(item)
        val parts = mutableListOf<String>()
        if (isMovie) {
            if (item.releaseYear.isNotBlank()) {
                parts.add(item.releaseYear)
            }
            if (item.duration.isNotBlank()) {
                parts.add(item.duration)
            } else if (item.episodes.isNotEmpty()) {
                val durMs = item.episodes.first().durationMs
                if (durMs > 0) parts.add("${durMs / 60000}m")
            }
        } else {
            if (sNum > 0) {
                parts.add("Season $sNum")
            }
            if (item.releaseYear.isNotBlank()) {
                parts.add(item.releaseYear)
            }
            if (item.totalEpisodes.isNotBlank()) {
                parts.add(item.totalEpisodes)
            } else if (item.episodes.isNotEmpty()) {
                parts.add("${item.episodes.size} Eps")
            }
        }

        return parts.joinToString(" • ")
    }

    /**
     * Builds unified options for the interactive Season & Arc Dropdown in the EPISODES tab.
     * Merges internal arcs/seasons inside [currentItem] with sibling franchise entries in [catalog].
     */
    fun buildSeasonArcOptions(
        currentItem: MediaItem,
        catalog: List<MediaItem>,
        selectedSeasonNumber: Int
    ): List<SeasonArcOption> {
        val franchiseList = getFranchiseItems(currentItem, catalog)
        val options = mutableListOf<SeasonArcOption>()

        // 1. Check if current item has internal multi-arc or multi-season episodes
        val internalSeasons = currentItem.episodes.map { it.seasonNumber }.distinct().sorted()
        val internalArcs = currentItem.episodes.mapNotNull { it.arcName.trim().takeIf { arc -> arc.isNotEmpty() } }.distinct()

        if (internalArcs.isNotEmpty()) {
            // Group by arc
            internalArcs.forEachIndexed { index, arcName ->
                val arcEps = currentItem.episodes.filter { it.arcName.equals(arcName, ignoreCase = true) }
                options.add(
                    SeasonArcOption(
                        id = "internal_arc_$index",
                        title = "Arc ${index + 1}: $arcName",
                        subtitle = "${arcEps.size} Episodes",
                        shortLabel = "Arc ${index + 1}",
                        badge = "ARC ${index + 1}",
                        episodeCount = arcEps.size,
                        isExternalMedia = false,
                        targetMediaItem = currentItem,
                        internalSeasonNumber = arcEps.firstOrNull()?.seasonNumber ?: 1,
                        internalArcName = arcName,
                        isCurrent = true
                    )
                )
            }
        } else if (internalSeasons.size > 1) {
            // Multiple internal seasons
            internalSeasons.forEach { sNum ->
                val sEps = currentItem.episodes.filter { it.seasonNumber == sNum }
                options.add(
                    SeasonArcOption(
                        id = "internal_season_$sNum",
                        title = "Season $sNum",
                        subtitle = "${sEps.size} Episodes",
                        shortLabel = "Season $sNum",
                        badge = "S$sNum",
                        episodeCount = sEps.size,
                        isExternalMedia = false,
                        targetMediaItem = currentItem,
                        internalSeasonNumber = sNum,
                        isCurrent = sNum == selectedSeasonNumber
                    )
                )
            }
        }

        // 2. Add all franchise items (seasons/sequels/movies)
        franchiseList.forEach { fItem ->
            val isThisItem = fItem.id == currentItem.id
            if (!isThisItem || options.isEmpty()) {
                val sNum = getEffectiveSeasonNumber(fItem)
                val isMovie = fItem.category.equals("MOVIE", ignoreCase = true) || fItem.type.equals("MOVIE", ignoreCase = true)
                val epCount = fItem.episodes.size.takeIf { it > 0 } 
                    ?: fItem.totalEpisodes.filter { it.isDigit() }.toIntOrNull() 
                    ?: 0

                val badgeStr = if (isMovie) "MOVIE" else "S$sNum"
                val shortLabelStr = if (isMovie) "Movie" else "Season $sNum"

                val titleStr = when {
                    fItem.seasonTitle.isNotBlank() -> fItem.seasonTitle
                    isMovie -> fItem.title
                    fItem.title.contains("Season $sNum", ignoreCase = true) -> fItem.title
                    sNum > 1 -> "Season $sNum: ${fItem.title}"
                    else -> "Season 1: ${fItem.title}"
                }

                val subtitleStr = buildString {
                    if (fItem.releaseYear.isNotBlank()) append("${fItem.releaseYear} • ")
                    append(if (epCount > 0) "$epCount Episodes" else "Episodes Pending")
                }

                options.add(
                    SeasonArcOption(
                        id = fItem.id,
                        title = titleStr,
                        subtitle = subtitleStr,
                        shortLabel = shortLabelStr,
                        badge = badgeStr,
                        episodeCount = epCount,
                        isExternalMedia = !isThisItem,
                        targetMediaItem = fItem,
                        internalSeasonNumber = sNum,
                        isCurrent = isThisItem
                    )
                )
            }
        }

        return options.distinctBy { it.id }
    }

    /**
     * Automatically derives a normalized franchise slug from an anime or series title.
     */
    private fun normalizeTitleToFranchiseSlug(rawTitle: String): String {
        var clean = rawTitle.lowercase(Locale.ROOT)

        // Remove typical season / arc suffixes
        clean = clean
            .replace(Regex("(:?\\s*season\\s*\\d+.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*\\d+(st|nd|rd|th)\\s*season.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*part\\s*\\d+.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*cour\\s*\\d+.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*the\\s*final\\s*season.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*arise\\s*from\\s*the\\s*shadow.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*reawakening.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*mugen\\s*train\\s*arc.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*entertainment\\s*district\\s*arc.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*swordsmith\\s*village\\s*arc.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*hashira\\s*training\\s*arc.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*thousand-year\\s*blood\\s*war.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*shippuuden.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*shippuden.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*brotherhood.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*alicia.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[:\\-–—].*"), "") // Cut off subtitles after colons/dashes
            .trim()

        return clean.replace(Regex("[^a-z0-9]+"), "-").trim('-')
    }

    private fun cleanFranchiseBaseTitle(rawTitle: String): String {
        var clean = rawTitle
            .replace(Regex("(:?\\s*Season\\s*\\d+.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*\\d+(st|nd|rd|th)\\s*Season.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*Part\\s*\\d+.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*Cour\\s*\\d+.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*The\\s*Final\\s*Season.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(:?\\s*Arise\\s*from\\s*the\\s*Shadow.*)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[:\\-–—].*"), "")
            .trim()

        return clean
    }

    /**
     * Auto-detects season number from title string.
     * e.g. "Solo Leveling Season 2" -> 2, "Attack on Titan Season 3 Part 2" -> 3
     */
    fun detectSeasonNumber(title: String): Int {
        val sMatch = Regex("Season\\s*(\\d+)", RegexOption.IGNORE_CASE).find(title)
        if (sMatch != null) {
            return sMatch.groupValues[1].toIntOrNull() ?: 1
        }
        val sShortMatch = Regex("\\bS(\\d+)\\b", RegexOption.IGNORE_CASE).find(title)
        if (sShortMatch != null) {
            return sShortMatch.groupValues[1].toIntOrNull() ?: 1
        }
        val thMatch = Regex("(\\d+)(st|nd|rd|th)\\s*Season", RegexOption.IGNORE_CASE).find(title)
        if (thMatch != null) {
            return thMatch.groupValues[1].toIntOrNull() ?: 1
        }
        val partMatch = Regex("Part\\s*(\\d+)", RegexOption.IGNORE_CASE).find(title)
        if (partMatch != null) {
            return partMatch.groupValues[1].toIntOrNull() ?: 1
        }
        return 1
    }
}
