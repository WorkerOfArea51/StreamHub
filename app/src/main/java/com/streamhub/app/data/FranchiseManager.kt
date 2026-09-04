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
                getChronologicalScore(item)
            }.thenBy { item ->
                item.releaseYear.toIntOrNull() ?: 9999
            }.thenBy { item ->
                item.title
            }
        )
    }

    fun getEffectiveSeasonNumber(item: MediaItem): Int {
        // Full manual control: If creator explicitly set partNumber > 0 and seasonNumber > 0,
        // respect seasonNumber unconditionally (e.g. Season 1 Part 2 is Season 1, NOT Season 2).
        if (item.partNumber > 0 && item.seasonNumber > 0) {
            return item.seasonNumber
        }
        if (item.seasonNumber > 1) return item.seasonNumber
        val detected = detectSeasonNumber(item.title)
        return if (detected > 1) detected else (item.seasonNumber.takeIf { it > 0 } ?: 1)
    }

    fun getEffectivePartNumber(item: MediaItem): Int? {
        // Full manual control: ONLY use part number if creator explicitly entered partNumber > 0 in Creator Mode!
        return if (item.partNumber > 0) item.partNumber else null
    }

    fun detectChapterOrPartNumber(title: String): Int? {
        val regex = Regex("""(?i)(?:chapter|cour|vol|volume|\bch\b|\bpt\b)\s*[-:]?\s*0*(\d+)""")
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
        // Priority 0: Explicit Creator Master Order (e.g. 1.0, 2.0, 3.0 or 1.5)
        if (item.franchiseOrder > 0.0) {
            return item.franchiseOrder * 100_000_000.0
        }

        val explicitSNum = item.seasonNumber.takeIf { it > 0 }
        val detectedSNum = detectSeasonNumber(item.title).takeIf { it > 1 }
        val partNum = getEffectivePartNumber(item) ?: 0
        val year = item.releaseYear.trim().toIntOrNull() ?: 0
        val format = getMediaFormatLabel(item)
        val isExplicitPrequel = item.relationType.trim().equals("PREQUEL", ignoreCase = true) || item.relationType.trim().startsWith("PREQUEL •", ignoreCase = true)
        val isExplicitSequel = item.relationType.trim().equals("SEQUEL", ignoreCase = true) || item.relationType.trim().startsWith("SEQUEL •", ignoreCase = true)

        val baseScore = when {
            // Priority 1: Admin explicitly specified a Season / Sequence Number (e.g. 1, 2, 3...)
            explicitSNum != null -> {
                (explicitSNum * 10_000_000.0) + (if (year > 1900) year * 10.0 else 0.0) + (partNum * 0.1)
            }

            // Priority 2: Title has detected Season number (e.g. "Season 2", "Part 3")
            detectedSNum != null -> {
                (detectedSNum * 10_000_000.0) + (if (year > 1900) year * 10.0 else 0.0) + (partNum * 0.1)
            }

            // Priority 3: No explicit sequence number -> Release Year timeline anchor
            year > 1900 -> {
                val yearBase = year * 10_000.0
                val formatOffset = when (format) {
                    "TV" -> 10.0
                    "MOVIE" -> if (isExplicitSequel) 50.0 else 25.0
                    "OVA", "ONA" -> 30.0
                    "SPECIAL", "TV SPECIAL" -> 40.0
                    else -> 20.0
                }
                yearBase + formatOffset
            }

            // Priority 4: Chapter/Part detection fallback
            else -> {
                val chapter = detectChapterOrPartNumber(item.title) ?: 1
                chapter * 1_000.0
            }
        }

        return when {
            isExplicitPrequel -> baseScore - 100_000_000.0
            isExplicitSequel -> baseScore + 500_000.0
            else -> baseScore
        }
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
                titleUpper.contains(" MOVIE") ||
                titleUpper.endsWith("MOVIE") ||
                titleUpper.contains(" THE MOVIE")

        if (isExplicitMovie) {
            return "MOVIE"
        }

        if (catUpper == "WEB_SERIES" || catUpper == "SERIES" || typeUpper == "SERIES" ||
            titleUpper.contains("SEASON") || relUpper.contains("TV") || item.episodes.size > 1
        ) {
            return "TV"
        }

        if (typeUpper == "MOVIE" || catUpper == "MOVIE" || catUpper == "MOVIES") {
            return "MOVIE"
        }

        return "TV"
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
        val isMovie = format == "MOVIE" || format == "OVA" || format == "ONA" || format == "SPECIAL" || format == "TV SPECIAL"
        val sNum = getEffectiveSeasonNumber(item)
        val pNum = getEffectivePartNumber(item)
        val parts = mutableListOf<String>()

        if (isMovie) {
            if (pNum != null && pNum > 1) {
                parts.add("Movie $pNum")
            } else if (item.releaseYear.isNotBlank()) {
                parts.add(item.releaseYear)
            }
            if (format != "MOVIE") {
                parts.add(format)
            }
            if (item.duration.isNotBlank()) {
                val dur = item.duration.trim()
                parts.add(if (dur.endsWith("min", ignoreCase = true) || dur.endsWith("m", ignoreCase = true)) dur else "${dur}m")
            } else if (item.episodes.isNotEmpty()) {
                val durMs = item.episodes.first().durationMs
                if (durMs > 0) parts.add("${durMs / 60000}m")
                else if (item.episodes.size > 1) parts.add("${item.episodes.size} Eps")
            }
        } else {
            if (sNum > 0) {
                if (pNum != null && pNum > 0) {
                    parts.add("Season $sNum Pt $pNum")
                } else {
                    parts.add("Season $sNum")
                }
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
    /**
     * Builds list of Season/Media options for the franchise universe selector.
     * Exclusively contains actual Seasons, Sequels, Prequels, and Movies.
     */
    fun buildSeasonOptions(
        currentItem: MediaItem,
        catalog: List<MediaItem>
    ): List<SeasonArcOption> {
        val franchiseList = getFranchiseItems(currentItem, catalog)
        val options = mutableListOf<SeasonArcOption>()

        franchiseList.forEach { fItem ->
            val isThisItem = fItem.id == currentItem.id
            val sNum = getEffectiveSeasonNumber(fItem)
            val pNum = getEffectivePartNumber(fItem)
            val isMovie = fItem.category.equals("MOVIE", ignoreCase = true) || fItem.type.equals("MOVIE", ignoreCase = true)
            val epCount = fItem.episodes.size.takeIf { it > 0 } 
                ?: fItem.totalEpisodes.filter { it.isDigit() }.toIntOrNull() 
                ?: 0

            val badgeStr = when {
                isMovie && pNum != null && pNum > 1 -> "M$pNum"
                isMovie -> "MOVIE"
                pNum != null && pNum > 0 -> "S$sNum P$pNum"
                else -> "S$sNum"
            }

            val shortLabelStr = when {
                isMovie && pNum != null && pNum > 1 -> "Movie $pNum"
                isMovie -> "Movie"
                pNum != null && pNum > 0 -> "Season $sNum Pt $pNum"
                else -> "Season $sNum"
            }

            val titleStr = when {
                fItem.seasonTitle.isNotBlank() -> fItem.seasonTitle
                isMovie -> fItem.title
                pNum != null && pNum > 0 && fItem.title.contains("Part", ignoreCase = true) -> fItem.title
                pNum != null && pNum > 0 -> "Season $sNum Part $pNum: ${fItem.title}"
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

        return options.distinctBy { it.id }
    }

    /**
     * Builds list of internal story arc options for the currently open season.
     */
    fun buildArcOptions(currentItem: MediaItem): List<SeasonArcOption> {
        val internalArcs = currentItem.episodes.mapNotNull { it.arcName.trim().takeIf { arc -> arc.isNotEmpty() } }.distinct()
        if (internalArcs.isEmpty()) return emptyList()

        return internalArcs.mapIndexed { index, arcName ->
            val arcEps = currentItem.episodes.filter { it.arcName.equals(arcName, ignoreCase = true) }
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
                isCurrent = false
            )
        }
    }

    /**
     * Legacy / backward-compatible builder combining seasons and internal splits.
     */
    fun buildSeasonArcOptions(
        currentItem: MediaItem,
        catalog: List<MediaItem>,
        selectedSeasonNumber: Int
    ): List<SeasonArcOption> {
        return buildSeasonOptions(currentItem, catalog)
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
        return 1
    }
}
