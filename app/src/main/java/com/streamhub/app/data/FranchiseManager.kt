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

data class FranchiseTag(
    val label: String,
    val type: FranchiseTagType
)

enum class FranchiseTagType {
    CURRENT,
    SEQUEL,
    PREQUEL,
    MOVIE,
    SIDE_STORY,
    SPIN_OFF,
    SPECIAL,
    FORMAT,
    ENCORE,
    EXTENDED_CUT,
    DIRECTORS_CUT,
    ALTERNATE_VERSION,
    REMASTER,
    EXTRA,
    MAIN_STORY
}

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

        val format = getMediaFormatLabel(item)
        val relUpper = item.relationType.trim().uppercase(Locale.ROOT)
        val isNonSeason = format in listOf("MOVIE", "OVA", "ONA", "SPECIAL", "TV SPECIAL", "EXTRA") ||
                relUpper.contains("EXTRA") ||
                relUpper.contains("SPECIAL") ||
                relUpper.contains("OVA") ||
                relUpper.contains("ONA") ||
                relUpper.contains("ENCORE") ||
                relUpper.contains("EXTENDED") ||
                relUpper.contains("DIRECTOR") ||
                relUpper.contains("REMASTER")

        // Zero-Season Invariant: If creator explicitly saved 0 on a movie, special, extra, OVA, ONA, or edition, preserve 0!
        if (isNonSeason && item.seasonNumber == 0) {
            return 0
        }

        val detected = detectSeasonNumber(item.title)
        if (detected > 1) return detected

        // If creator explicitly saved seasonNumber = 0, preserve 0 (never force 0 to 1)!
        return item.seasonNumber
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
        val isExplicitPrequel = item.relationType.contains("PREQUEL", ignoreCase = true)
        val isExplicitSequel = item.relationType.contains("SEQUEL", ignoreCase = true)
        val isExplicitEdition = item.relationType.contains("ENCORE", ignoreCase = true) ||
                item.relationType.contains("EXTENDED", ignoreCase = true) ||
                item.relationType.contains("DIRECTOR", ignoreCase = true) ||
                item.relationType.contains("REMASTER", ignoreCase = true)

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
                    "SPECIAL", "TV SPECIAL", "EXTRA" -> 40.0
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
            isExplicitEdition -> baseScore + 5_000.0
            else -> baseScore
        }
    }

    /**
     * Determines whether a media item is fundamentally a Movie based on the Creator's
     * Overview setting (type == "MOVIE" or category == "MOVIE").
     * Story role tags (e.g. TV Special, Side Story) NEVER change a Series into a Movie.
     */
    fun isMovie(item: MediaItem): Boolean {
        val typeUpper = item.type.trim().uppercase(Locale.ROOT)
        val catUpper = item.category.trim().uppercase(Locale.ROOT)
        val relUpper = item.relationType.trim().uppercase(Locale.ROOT)
        val titleUpper = item.title.trim().uppercase(Locale.ROOT)

        // 1. Explicit Series: If creator set type to SERIES, or category is SERIES / WEB_SERIES
        if (typeUpper == "SERIES" || catUpper == "SERIES" || catUpper == "WEB_SERIES") return false

        // 2. Explicit Movie: If category is MOVIE, or relationType explicitly contains MOVIE
        if (catUpper == "MOVIE" || catUpper == "MOVIES" || relUpper.contains("MOVIE")) return true

        // 3. If title explicitly indicates a Season (e.g. "Season 1", "Season 2"), it's a TV series even if type defaulted to MOVIE
        if (titleUpper.contains("SEASON ") || titleUpper.endsWith(" SEASON") || titleUpper.contains(" S1") || titleUpper.contains(" S2")) return false

        // 4. Default type check
        if (typeUpper == "MOVIE") return true

        return titleUpper.contains(" MOVIE") || titleUpper.endsWith(" MOVIE") || titleUpper.contains(" THE MOVIE")
    }

    /**
     * Extracts the format type of a media item: "TV", "MOVIE", "OVA", "ONA", "SPECIAL"
     */
    fun getMediaFormatLabel(item: MediaItem): String {
        val relUpper = item.relationType.trim().uppercase(Locale.ROOT)
        val catUpper = item.category.trim().uppercase(Locale.ROOT)
        val typeUpper = item.type.trim().uppercase(Locale.ROOT)
        val titleUpper = item.title.trim().uppercase(Locale.ROOT)
        val title = item.title.trim()

        val hasTitleWord = { word: String -> Regex("""\b${Regex.escape(word)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(title) }
        val hasRelWord = { word: String -> Regex("""\b${Regex.escape(word)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(relUpper) }

        if (hasRelWord("TV SPECIAL") || hasTitleWord("TV SPECIAL") || hasRelWord("TV-SPECIAL")) return "TV SPECIAL"
        if (hasRelWord("OVA") || hasTitleWord("OVA")) return "OVA"
        if (hasRelWord("ONA") || hasTitleWord("ONA")) return "ONA"
        if (hasRelWord("EXTRA")) return "EXTRA"
        if (hasRelWord("SPECIAL") || hasTitleWord("SPECIAL")) return "SPECIAL"

        if (isMovie(item) || hasRelWord("MOVIE")) {
            return "MOVIE"
        }

        if (catUpper == "WEB_SERIES" || catUpper == "SERIES" || typeUpper == "SERIES" ||
            titleUpper.contains("SEASON") || hasRelWord("TV") || item.episodes.size > 1
        ) {
            return "TV"
        }

        return if (isMovie(item)) "MOVIE" else "TV"
    }

    /**
     * Extracts all individual relation tags for displaying on franchise cards.
     * Preserves directional roles (SEQUEL / PREQUEL) alongside non-linear tags
     * (SIDE STORY, SPIN-OFF, etc.) and format labels without dropping any tags.
     */
    fun getFranchiseTags(candidate: MediaItem, currentItem: MediaItem): List<FranchiseTag> {
        val format = getMediaFormatLabel(candidate)
        val isCurrent = candidate.id == currentItem.id

        if (isCurrent) {
            val formatType = when {
                format == "MOVIE" -> FranchiseTagType.MOVIE
                format in listOf("OVA", "ONA", "SPECIAL", "TV SPECIAL", "EXTRA") -> FranchiseTagType.SPECIAL
                else -> FranchiseTagType.FORMAT
            }
            return listOf(
                FranchiseTag("CURRENT", FranchiseTagType.CURRENT),
                FranchiseTag(format, formatType)
            )
        }

        val explicitRelation = candidate.relationType.trim().uppercase(Locale.ROOT)
        val isExplicitSequel = explicitRelation.contains("SEQUEL")
        val isExplicitPrequel = explicitRelation.contains("PREQUEL")
        val isEncore = explicitRelation.contains("ENCORE")
        val isExtended = explicitRelation.contains("EXTENDED")
        val isDirector = explicitRelation.contains("DIRECTOR")
        val isAlternate = explicitRelation.contains("ALTERNATIVE") || explicitRelation.contains("ALTERNATE")
        val isRemaster = explicitRelation.contains("REMASTER")
        val isExtra = explicitRelation.contains("EXTRA")
        val isMainStory = explicitRelation.contains("MAIN STORY")
        val hasNonLinearTag = explicitRelation.contains("SIDE STORY") ||
                explicitRelation.contains("SPIN-OFF") ||
                explicitRelation.contains("SPINOFF") ||
                isAlternate ||
                explicitRelation.contains("PARODY") ||
                explicitRelation.contains("RECAP") ||
                isEncore ||
                isExtended ||
                isDirector ||
                isRemaster ||
                isExtra ||
                isMainStory

        val directionalRole = when {
            isExplicitSequel && isExplicitPrequel -> {
                val candidateScore = getChronologicalScore(candidate)
                val currentScore = getChronologicalScore(currentItem)
                if (candidateScore >= currentScore) "SEQUEL" else "PREQUEL"
            }
            isExplicitSequel -> {
                val candidateScore = getChronologicalScore(candidate)
                val currentScore = getChronologicalScore(currentItem)
                if (candidateScore < currentScore) "PREQUEL" else "SEQUEL"
            }
            isExplicitPrequel -> {
                val candidateScore = getChronologicalScore(candidate)
                val currentScore = getChronologicalScore(currentItem)
                if (candidateScore > currentScore) "SEQUEL" else "PREQUEL"
            }
            hasNonLinearTag -> null // Pure non-linear role/edition without explicit sequel/prequel tag
            else -> {
                val candidateScore = getChronologicalScore(candidate)
                val currentScore = getChronologicalScore(currentItem)
                when {
                    candidateScore > currentScore -> "SEQUEL"
                    candidateScore < currentScore -> "PREQUEL"
                    else -> null
                }
            }
        }

        val tags = mutableListOf<FranchiseTag>()

        if (directionalRole != null) {
            val roleType = if (directionalRole == "SEQUEL") FranchiseTagType.SEQUEL else FranchiseTagType.PREQUEL
            tags.add(FranchiseTag(directionalRole, roleType))
        }

        if (isMainStory) {
            tags.add(FranchiseTag("MAIN STORY", FranchiseTagType.MAIN_STORY))
        }
        if (isEncore) {
            tags.add(FranchiseTag("ENCORE", FranchiseTagType.ENCORE))
        }
        if (isExtended) {
            tags.add(FranchiseTag("EXTENDED CUT", FranchiseTagType.EXTENDED_CUT))
        }
        if (isDirector) {
            tags.add(FranchiseTag("DIRECTOR'S CUT", FranchiseTagType.DIRECTORS_CUT))
        }
        if (isAlternate) {
            tags.add(FranchiseTag("ALTERNATE", FranchiseTagType.ALTERNATE_VERSION))
        }
        if (isRemaster) {
            tags.add(FranchiseTag("REMASTER", FranchiseTagType.REMASTER))
        }
        if (isExtra) {
            tags.add(FranchiseTag("EXTRA", FranchiseTagType.EXTRA))
        }
        if (explicitRelation.contains("SIDE STORY")) {
            tags.add(FranchiseTag("SIDE STORY", FranchiseTagType.SIDE_STORY))
        }
        if (explicitRelation.contains("SPIN-OFF") || explicitRelation.contains("SPINOFF")) {
            tags.add(FranchiseTag("SPIN-OFF", FranchiseTagType.SPIN_OFF))
        }
        if (explicitRelation.contains("PARODY")) {
            tags.add(FranchiseTag("PARODY", FranchiseTagType.FORMAT))
        }
        if (explicitRelation.contains("RECAP")) {
            tags.add(FranchiseTag("RECAP", FranchiseTagType.FORMAT))
        }

        val formatType = when {
            format == "MOVIE" -> FranchiseTagType.MOVIE
            format in listOf("OVA", "ONA", "SPECIAL", "TV SPECIAL", "EXTRA") -> FranchiseTagType.SPECIAL
            else -> FranchiseTagType.FORMAT
        }
        tags.add(FranchiseTag(format, formatType))

        return tags.distinctBy { it.label }
    }

    /**
     * Formats a relation tag for displaying on season cards.
     * Combines all explicit relation roles with format type.
     */
    fun getFranchiseTag(candidate: MediaItem, currentItem: MediaItem): String {
        val tags = getFranchiseTags(candidate, currentItem)
        return tags.joinToString(" • ") { it.label }
    }

    /**
     * Computes the display subtitle for a franchise card.
     * Supports optional inclusion of duration (defaults to true for test suite compatibility).
     *
     * Master Authority Invariant:
     * - If !isMovie(item) && sNum > 0: Always formatted as a numbered Season (e.g. "Season 4 Pt 3 • 2023 • 2 Episodes").
     *   Story role tags like "TV Special" NEVER turn a Series into a Movie or label it "Movie 3".
     * - If isMovie(item): Formatted as a Movie (e.g. "Movie 2", "Encore Cut", "2018 • 100 min").
     * - If !isMovie(item) && sNum == 0: Formatted as a Zero-Season entry (e.g. "Extras • 2018 • 4 Episodes", "OVA • 2 Episodes").
     */
    fun getSeasonCardSubtitle(item: MediaItem, includeDuration: Boolean = true): String {
        val sNum = getEffectiveSeasonNumber(item)
        val pNum = getEffectivePartNumber(item)
        val format = getMediaFormatLabel(item)
        val relUpper = item.relationType.trim().uppercase(Locale.ROOT)
        val isMovieItem = isMovie(item)
        val parts = mutableListOf<String>()

        if (!isMovieItem && sNum > 0) {
            // Case 1: Numbered Season of a Series (e.g. S4 P3, S1, S2)
            if (pNum != null && pNum > 0) {
                parts.add("Season $sNum Pt $pNum")
            } else {
                parts.add("Season $sNum")
            }
            if (item.releaseYear.isNotBlank()) {
                parts.add(item.releaseYear)
            }
            if (item.totalEpisodes.isNotBlank()) {
                val epText = item.totalEpisodes.trim()
                parts.add(if (epText.endsWith("Eps", ignoreCase = true) || epText.endsWith("Episodes", ignoreCase = true)) epText else "$epText Episodes")
            } else if (item.episodes.isNotEmpty()) {
                parts.add("${item.episodes.size} Episodes")
            }
        } else if (isMovieItem) {
            // Case 2: Movie (Standalone, Numbered, Multi-part, or Special Edition)
            when {
                sNum > 0 && pNum != null && pNum > 0 -> parts.add("Movie $sNum Pt $pNum")
                sNum > 0 -> parts.add("Movie $sNum")
                pNum != null && pNum > 0 -> parts.add("Part $pNum")
                relUpper.contains("ENCORE") -> parts.add("Encore Cut")
                relUpper.contains("EXTENDED CUT") || relUpper.contains("EXTENDED") -> parts.add("Extended Cut")
                relUpper.contains("DIRECTOR'S CUT") || relUpper.contains("DIRECTOR") -> parts.add("Director's Cut")
                relUpper.contains("REMASTER") -> parts.add("Remaster")
            }
            if (item.releaseYear.isNotBlank()) {
                parts.add(item.releaseYear)
            }
            if (includeDuration) {
                if (item.duration.isNotBlank()) {
                    val dur = item.duration.trim()
                    parts.add(if (dur.endsWith("min", ignoreCase = true) || dur.endsWith("m", ignoreCase = true)) dur else "${dur}m")
                } else if (item.episodes.isNotEmpty()) {
                    val durMs = item.episodes.first().durationMs
                    if (durMs > 0) parts.add("${durMs / 60000}m")
                }
            }
        } else {
            // Case 3: Series Zero-Season entry (sNum == 0: Extras, OVAs, ONAs, Specials)
            val isExtra = relUpper.contains("EXTRA") || format == "EXTRA"
            when {
                isExtra && pNum != null && pNum > 1 -> parts.add("Extras Pt $pNum")
                isExtra -> parts.add("Extras")
                format in listOf("OVA", "ONA", "SPECIAL", "TV SPECIAL") && pNum != null && pNum > 1 -> parts.add("$format Pt $pNum")
                format in listOf("OVA", "ONA", "SPECIAL", "TV SPECIAL") -> parts.add(format)
                pNum != null && pNum > 1 -> parts.add("Part $pNum")
                item.releaseYear.isNotBlank() -> parts.add(item.releaseYear)
            }
            if (item.releaseYear.isNotBlank() && !parts.contains(item.releaseYear)) {
                parts.add(item.releaseYear)
            }
            if (item.totalEpisodes.isNotBlank()) {
                val epText = item.totalEpisodes.trim()
                parts.add(if (epText.endsWith("Eps", ignoreCase = true) || epText.endsWith("Episodes", ignoreCase = true)) epText else "$epText Episodes")
            } else if (item.episodes.size > 1) {
                parts.add("${item.episodes.size} Episodes")
            } else if (includeDuration) {
                if (item.duration.isNotBlank()) {
                    val dur = item.duration.trim()
                    parts.add(if (dur.endsWith("min", ignoreCase = true) || dur.endsWith("m", ignoreCase = true)) dur else "${dur}m")
                } else if (item.episodes.isNotEmpty() && item.episodes.first().durationMs > 0) {
                    parts.add("${item.episodes.first().durationMs / 60000}m")
                }
            }
        }

        return parts.joinToString(" • ")
    }

    /**
     * Single Source of Truth for top-left poster badge in Franchise Carousels,
     * Media Cards, and Dropdown selectors.
     *
     * Invariants:
     * 1. Movie Editions (ENCORE, EXTENDED, DIRECTOR'S CUT, REMASTER) take top priority.
     * 2. Non-season tags (EXTRA, OVA, ONA, TV SPECIAL, SPECIAL) reflect creator explicit selection.
     * 3. Movies:
     *    - Movie# > 0 && Part# > 0 -> "M$movie P$part"
     *    - Movie# > 0 -> "M$movie" (e.g. M1 for Demon Slayer Infinity Castle Movie 1)
     *    - Part# > 0 -> "Pt $part" (e.g. Pt 1 for Dune Part 1)
     *    - Otherwise -> "MOVIE" (Code Geass, Kaguya-sama, standalone movies)
     * 4. Series:
     *    - Season# > 0 && Part# > 0 -> "S$season P$part" (e.g. S4 P3 for AoT)
     *    - Season# > 0 -> "S$season"
     * 5. Zero-Season Series: formatLabel (EXTRA, OVA, etc.)
     * 6. franchiseOrder is SOLELY for timeline position on the shelf and NEVER touches poster badges.
     */
    fun getPosterBadge(item: MediaItem): String? {
        val relUpper = item.relationType.trim().uppercase(Locale.ROOT)
        val sNum = getEffectiveSeasonNumber(item)
        val pNum = getEffectivePartNumber(item)
        val isMovieItem = isMovie(item)
        val formatLabel = getMediaFormatLabel(item)

        return when {
            // 1. Movie Editions take top precedence
            relUpper.contains("ENCORE") -> "ENCORE"
            relUpper.contains("EXTENDED CUT") || relUpper.contains("EXTENDED") -> "EXTENDED"
            relUpper.contains("DIRECTOR'S CUT") || relUpper.contains("DIRECTOR") -> "DIRECTOR"
            relUpper.contains("REMASTER") -> "REMASTER"

            // 2. Explicit Creator Relation (EXTRA, OVA, ONA, SPECIAL) - Creator authority!
            relUpper.contains("EXTRA") || formatLabel == "EXTRA" -> "EXTRA"
            sNum == 0 && formatLabel in listOf("OVA", "ONA", "SPECIAL", "TV SPECIAL") -> formatLabel

            // 3. Movies
            isMovieItem -> when {
                sNum > 0 && pNum != null && pNum > 0 -> "M$sNum P$pNum"
                sNum > 0 -> "M$sNum"
                pNum != null && pNum > 0 -> "Pt $pNum"
                else -> "MOVIE"
            }

            // 4. Regular TV Seasons
            !isMovieItem && sNum > 0 -> when {
                pNum != null && pNum > 0 -> "S$sNum P$pNum"
                else -> "S$sNum"
            }

            // 5. Zero-Season Series fallback
            sNum == 0 -> formatLabel

            else -> null
        }
    }

    /**
     * Builds unified options for the interactive Season & Arc Dropdown in the EPISODES tab.
     * Merges internal arcs/seasons inside [currentItem] with sibling franchise entries in [catalog].
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
            val isMovie = isMovie(fItem)
            val epCount = fItem.episodes.size.takeIf { it > 0 } 
                ?: fItem.totalEpisodes.filter { it.isDigit() }.toIntOrNull() 
                ?: 0

            val relUpper = fItem.relationType.trim().uppercase(Locale.ROOT)
            val format = getMediaFormatLabel(fItem)
            val isEncore = relUpper.contains("ENCORE")
            val isExtended = relUpper.contains("EXTENDED")
            val isDirector = relUpper.contains("DIRECTOR")
            val isRemaster = relUpper.contains("REMASTER")
            val isExtra = relUpper.contains("EXTRA") || format == "EXTRA"

            val badgeStr = getPosterBadge(fItem) ?: (if (isMovie) "MOVIE" else "TV")

            val shortLabelStr = when {
                isEncore -> "Encore Cut"
                isExtended -> "Extended Cut"
                isDirector -> "Director's Cut"
                isRemaster -> "Remaster"
                isExtra -> "Extras"
                format in listOf("OVA", "ONA", "SPECIAL", "TV SPECIAL") && sNum == 0 -> format
                isMovie && sNum > 0 && pNum != null && pNum > 0 -> "Movie $sNum Pt $pNum"
                isMovie && sNum > 0 -> "Movie $sNum"
                isMovie && pNum != null && pNum > 0 -> "Part $pNum"
                isMovie -> "Movie"
                pNum != null && pNum > 0 -> "Season $sNum Pt $pNum"
                sNum > 0 -> "Season $sNum"
                else -> format
            }

            val titleStr = fItem.title

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
