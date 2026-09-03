package com.streamhub.app.data

import com.streamhub.app.data.models.Episode
import java.util.Locale

/**
 * Robust episode normalization and chronological ordering engine.
 *
 * Solves:
 * 1. False arc-scoped / batch episode numbers (e.g. EP 356 assigned episodeNumber = 14 because it was the 14th episode of Lost Agent Arc).
 * 2. Multi-arc episode interleaving (e.g. Arc 1 Ep 14 and Arc 16 Ep 14 grouped side-by-side by naive season/episode sorts).
 * 3. Preserving strict storyline continuity across all story arcs.
 */
object EpisodeOrderingManager {

    private val EXPLICIT_EP_REGEX = Regex(
        """(?i)\b(?:ep|episode|e)\s*[-_.:#]?\s*0*(\d+)\b"""
    )
    private val BRACKETED_NUM_REGEX = Regex(
        """\[\s*0*(\d{1,4})\s*\]"""
    )

    private val EXCLUDED_NUMBERS = setOf(
        240, 360, 480, 540, 576, 720, 1080, 1440, 2160, 4320, // Resolutions
        264, 265, // Codecs (x264, x265)
        8, 10, // 8-bit, 10-bit
        24, 30, 60, 120 // Common frame rates
    )

    /**
     * Attempts to extract the true absolute series episode number from the title or filename.
     * e.g. "EP 356 Foe or Friend! Ginjō's Unseen Heart!" -> 356
     * "EP - 14 - Back to Back, a Fight to the Death!" -> 14
     * "EP - 14 - EP 356 Foe or Friend!" -> 356 (prefers the absolute series number)
     */
    fun extractAbsoluteEpisodeNumber(title: String, fileName: String = ""): Int? {
        val candidates = mutableListOf<Int>()

        // 1. Check title for explicit markers (EP, Episode, E)
        EXPLICIT_EP_REGEX.findAll(title).forEach { match ->
            match.groupValues[1].toIntOrNull()?.let { num ->
                if (num in 1..9999 && num !in EXCLUDED_NUMBERS) {
                    candidates.add(num)
                }
            }
        }

        // 2. Check title for bracketed numbers like [356]
        BRACKETED_NUM_REGEX.findAll(title).forEach { match ->
            match.groupValues[1].toIntOrNull()?.let { num ->
                if (num in 1..9999 && num !in EXCLUDED_NUMBERS) {
                    candidates.add(num)
                }
            }
        }

        // 3. Check fileName if title gave no explicit matches
        if (candidates.isEmpty() && fileName.isNotBlank()) {
            EXPLICIT_EP_REGEX.findAll(fileName).forEach { match ->
                match.groupValues[1].toIntOrNull()?.let { num ->
                    if (num in 1..9999 && num !in EXCLUDED_NUMBERS) {
                        candidates.add(num)
                    }
                }
            }
            BRACKETED_NUM_REGEX.findAll(fileName).forEach { match ->
                match.groupValues[1].toIntOrNull()?.let { num ->
                    if (num in 1..9999 && num !in EXCLUDED_NUMBERS) {
                        candidates.add(num)
                    }
                }
            }
        }

        if (candidates.isEmpty()) return null

        // If multiple numbers were matched (e.g. "EP - 14 - EP 356..."):
        // The higher number is almost always the absolute series episode number (356 vs 14).
        return candidates.maxOrNull()
    }

    /**
     * Resolves the true effective episode number for an [Episode].
     * If [ep.title] or [ep.fileName] contains an explicit series number greater than [ep.episodeNumber],
     * we promote it to the absolute series number.
     */
    fun resolveEffectiveEpisode(ep: Episode): Episode {
        val explicitNumber = extractAbsoluteEpisodeNumber(ep.title, ep.fileName)
        return if (explicitNumber != null && (explicitNumber > ep.episodeNumber || ep.episodeNumber <= 0)) {
            ep.copy(episodeNumber = explicitNumber)
        } else {
            ep
        }
    }

    /**
     * Normalizes all episode numbers and sorts the list in strict storyline / chronological order:
     * 1. Resolves absolute episode numbers from titles/filenames.
     * 2. Preserves arc timeline order so multi-arc series never interleave episodes from different arcs.
     * 3. Sorts by seasonNumber -> arcOrder -> episodeNumber.
     */
    fun normalizeAndSort(
        episodes: List<Episode>,
        explicitArcOrder: List<String> = emptyList()
    ): List<Episode> {
        if (episodes.isEmpty()) return emptyList()

        // 1. Resolve effective episode numbers
        val resolvedList = episodes.map { resolveEffectiveEpisode(it) }

        // 2. Discover distinct story arcs in chronological order
        val arcOrder = if (explicitArcOrder.isNotEmpty()) {
            explicitArcOrder
        } else {
            resolvedList.mapNotNull { it.arcName.trim().takeIf { a -> a.isNotEmpty() } }.distinct()
        }

        val arcOrderIndexMap = arcOrder.withIndex().associate { (idx, name) ->
            name.lowercase(Locale.ROOT) to idx
        }

        // 3. Check if all episodes have strictly unique absolute episode numbers within each season
        val hasOverlappingEpisodeNumbers = resolvedList.groupBy { it.seasonNumber }
            .any { (_, seasonEps) ->
                val epNums = seasonEps.map { it.episodeNumber }
                epNums.size != epNums.distinct().size
            }

        return if (!hasOverlappingEpisodeNumbers) {
            // All episode numbers are distinct absolute numbers (e.g. 1..366)
            resolvedList.sortedWith(
                compareBy<Episode> { it.seasonNumber }
                    .thenBy { it.episodeNumber }
            )
        } else {
            // There are overlapping episode numbers across different arcs (e.g. Arc 1 Ep 1..20, Arc 2 Ep 1..21)
            // Group and sort by seasonNumber -> arc order -> episodeNumber to preserve storyline blocks!
            resolvedList.sortedWith(
                compareBy<Episode> { it.seasonNumber }
                    .thenBy { ep ->
                        val cleanArc = ep.arcName.trim().lowercase(Locale.ROOT)
                        arcOrderIndexMap[cleanArc] ?: 9999
                    }
                    .thenBy { it.episodeNumber }
            )
        }
    }
}
