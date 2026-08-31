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
                val sNum = getEffectiveSeasonNumber(item)
                if (sNum > 0) sNum else 999
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
        val sNum = getEffectiveSeasonNumber(item)
        val year = item.releaseYear.trim().toIntOrNull() ?: 0
        val chapter = detectChapterOrPartNumber(item.title) ?: sNum.takeIf { it > 0 } ?: 1
        val isExplicitPrequel = item.relationType.trim().equals("PREQUEL", ignoreCase = true) && sNum <= 1

        val baseScore = when {
            sNum > 0 && year > 1900 -> (sNum * 100000.0) + (year * 10.0)
            sNum > 0 -> sNum * 100000.0
            year > 1900 -> (year * 100.0) + chapter
            else -> chapter.toDouble()
        }

        return if (isExplicitPrequel) baseScore - 1_000_000.0 else baseScore
    }

    /**
     * Extracts the format type of a media item: "TV", "MOVIE", "OVA", "ONA", "SPECIAL"
     */
    fun getMediaFormatLabel(item: MediaItem): String {
        val relUpper = item.relationType.trim().uppercase(Locale.ROOT)
        val catUpper = item.category.trim().uppercase(Locale.ROOT)
        val typeUpper = item.type.trim().uppercase(Locale.ROOT)
        val titleUpper = item.title.trim().uppercase(Locale.ROOT)

        if (relUpper.contains("TV SPECIAL") || titleUpper.contains("TV SPECIAL") || relUpper.contains("TV-SPECIAL")) return "TV SPECIAL"
        if (relUpper.contains("OVA") || titleUpper.contains(" OVA") || titleUpper.endsWith("OVA")) return "OVA"
        if (relUpper.contains("ONA") || titleUpper.contains(" ONA") || titleUpper.endsWith("ONA")) return "ONA"
        if (relUpper.contains("SPECIAL") || titleUpper.contains(" SPECIAL") || titleUpper.endsWith("SPECIAL")) return "SPECIAL"

        val isExplicitMovie = relUpper.contains("MOVIE") ||
                catUpper == "MOVIE" ||
                catUpper == "MOVIES" ||
                typeUpper == "MOVIE" ||
                titleUpper.contains(" MOVIE") ||
                titleUpper.endsWith("MOVIE") ||
                titleUpper.contains(" THE MOVIE")

        if (catUpper == "ANIME" || catUpper == "ANIMES" || catUpper == "WEB_SERIES" || catUpper == "SERIES" || typeUpper == "SERIES") {
            // It's anime/series category — check if it's explicitly a franchise film/movie
            return if (isExplicitMovie && item.seasonNumber <= 1 && item.episodes.size <= 1 && !titleUpper.contains("SEASON")) "MOVIE" else "TV"
        }

        if (isExplicitMovie) {
            return "MOVIE"
        }

        if (item.seasonNumber > 1 || titleUpper.contains("SEASON") || relUpper.contains("TV")) {
            return "TV"
        }

        return if (catUpper == "MOVIE" || typeUpper == "MOVIE") "MOVIE" else "TV"
    }

    /**
     * Formats a relation tag for displaying on season cards.
     * Combines relation role (CURRENT, SEQUEL, PREQUEL, SIDE STORY, SPIN-OFF)
     * with format type (TV, TV SPECIAL, MOVIE, OVA, ONA, SPECIAL).
     *
     * Relative to [currentItem]:
     * - Past releases (lower season / earlier release year) -> PREQUEL
     * - Future releases (higher season / later release year) -> SEQUEL
     * - Currently open item -> CURRENT
     */
    fun getFranchiseTag(candidate: MediaItem, currentItem: MediaItem): String {
        val format = getMediaFormatLabel(candidate)
        val isCurrent = candidate.id == currentItem.id

        if (isCurrent) {
            return "CURRENT • $format"
        }

        val explicitRelation = candidate.relationType.trim().uppercase(Locale.ROOT)
        val role = when {
            // Non-linear / Side stories / Spin-offs maintain their distinct non-linear role
            explicitRelation.contains("SIDE STORY") -> "SIDE STORY"
            explicitRelation.contains("SPIN-OFF") || explicitRelation.contains("SPINOFF") -> "SPIN-OFF"
            explicitRelation.contains("ALTERNATIVE") -> "ALTERNATIVE"
            explicitRelation.contains("PARODY") -> "PARODY"
            explicitRelation.contains("RECAP") -> "RECAP"
            else -> {
                val candidateScore = getChronologicalScore(candidate)
                val currentScore = getChronologicalScore(currentItem)
                when {
                    candidateScore > currentScore -> "SEQUEL"
                    candidateScore < currentScore -> "PREQUEL"
                    else -> "RELATED"
                }
            }
        }

        return when {
            role == format -> role
            role == "RELATED" -> format
            else -> "$role • $format"
        }
    }

    /**
     * Computes the display title for a franchise card.
     * e.g. "2017 • 122m" for movies, "Season 1 • 2024 • 12 Eps" for series.
     */
    fun getSeasonCardSubtitle(item: MediaItem): String {
        val format = getMediaFormatLabel(item)
        val isMovie = format == "MOVIE"
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
                val epText = item.totalEpisodes.trim()
                parts.add(if (epText.endsWith("Eps", ignoreCase = true) || epText.endsWith("Episodes", ignoreCase = true)) epText else "$epText Eps")
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
