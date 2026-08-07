package com.streamhub.app.data.models

/**
 * Domain model representing a single media entry (movie, anime, or web series).
 *
 * All defaults are EMPTY — never fabricated data. If a field is missing from
 * the Firestore document, the UI should display empty (or hide the row), not
 * invent data. This prevents the "every show looks like Solo Leveling" bug
 * that the previous defaults caused.
 *
 * Required fields (no sensible empty default): id, title, type, category.
 * All other fields are optional and may be empty.
 */
data class MediaItem(
    val id: String = "",
    val title: String = "",
    val type: String = "MOVIE",          // "MOVIE" or "SERIES"
    val category: String = "MOVIE",      // "ANIME", "MOVIE", "WEB_SERIES"
    val genres: List<String> = emptyList(),
    val rating: String = "",             // e.g. "8.14" — empty if unknown
    val releaseYear: String = "",        // e.g. "2024" — empty if unknown
    val maturityRating: String = "",     // e.g. "16+" — empty if unknown
    val studio: String = "",             // e.g. "A-1 Pictures" — empty if unknown
    val trailerId: String = "",          // YouTube video ID — empty if unknown
    val malId: String = "",
    val tmdbId: String = "",
    val synonyms: String = "",           // alternative titles — empty if unknown
    val totalEpisodes: String = "",      // e.g. "12 Episodes" — empty if unknown
    val status: String = "",             // e.g. "Finished Airing" — empty if unknown
    val aired: String = "",              // e.g. "Jan 7, 2024 to Mar 31, 2024" — empty if unknown
    val premiered: String = "",          // e.g. "Winter 2024" — empty if unknown
    val producers: String = "",          // e.g. "Aniplex, Crunchyroll" — empty if unknown
    val source: String = "",             // e.g. "Web manga" — empty if unknown
    val duration: String = "",           // e.g. "23 min. per ep" — empty if unknown
    val budgetBoxOffice: String = "",    // e.g. "$25M Budget / $85M Box Office" — empty if unknown
    val castList: List<String> = emptyList(),
    val posterUrl: String = "",
    val bannerUrl: String = "",
    val description: String = "",
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val mediaInfo: MediaInfo = MediaInfo(),
    val episodes: List<Episode> = emptyList()
)

/**
 * Technical specifications for a media item.
 *
 * All defaults are EMPTY — UI hides badges/rows when fields are empty.
 */
data class MediaInfo(
    val resolution: String = "",         // e.g. "1080p FHD" — empty if unknown
    val videoCodec: String = "",         // e.g. "HEVC / x265" — empty if unknown
    val bitrate: String = "",            // e.g. "3450 kb/s" — empty if unknown
    val frameRate: String = "",          // e.g. "23.976 FPS" — empty if unknown
    val aspectRatio: String = "",        // e.g. "2.35:1" — empty if unknown
    val fileSize: String = "",           // e.g. "2.3 GB" — empty if unknown
    val audioTracks: List<String> = emptyList(),
    val subtitleTracks: List<String> = emptyList(),
    val qualityBadges: List<String> = emptyList()
)

/**
 * Single episode of a series (or the single "episode" representing a movie).
 *
 * REMOVED in M3: isDownloaded, downloadProgress — these were dead state never
 * updated by any code. Real download tracking will be added in M6 via a
 * separate DownloadedItem model (not bolted onto Episode).
 */
data class Episode(
    val episodeNumber: Int = 1,
    val seasonNumber: Int = 1,
    val title: String = "",
    val thumbnailUrl: String = "",
    val streamUrl: String = "",
    val mirrorStreamUrl: String = "",
    val telegramFileId: String = "",
    val durationMs: Long = 0L
)

/**
 * Persisted playback position for resume-watching.
 *
 * Stored in SharedPreferences by WatchHistoryManager, keyed by mediaId.
 */
data class PlaybackProgress(
    val mediaId: String,
    val episodeNumber: Int,
    val positionMs: Long,
    val durationMs: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)
