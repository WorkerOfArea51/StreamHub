package com.streamhub.app.data.models

data class MediaItem(
    val id: String = "",
    val title: String = "",
    val type: String = "MOVIE", // "MOVIE" or "SERIES"
    val category: String = "ANIME", // "ANIME", "MOVIE", "WEB_SERIES"
    val genres: List<String> = emptyList(),
    val rating: String = "8.14",
    val releaseYear: String = "2024",
    val maturityRating: String = "16+",
    val studio: String = "A-1 Pictures",
    val trailerId: String = "1kCwjK4rgYg", // YouTube Video ID
    val malId: String = "",
    val tmdbId: String = "",
    val synonyms: String = "Na Honjaman Level Up, I Level Up Alone",
    val totalEpisodes: String = "12 Episodes",
    val status: String = "Finished Airing",
    val aired: String = "Jan 7, 2024 to Mar 31, 2024",
    val premiered: String = "Winter 2024",
    val producers: String = "Aniplex, Crunchyroll, Netmarble",
    val source: String = "Web manga",
    val duration: String = "23 min. per ep",
    val budgetBoxOffice: String = "$25M Budget / $85M Box Office",
    val castList: List<String> = emptyList(),
    val posterUrl: String = "",
    val bannerUrl: String = "",
    val description: String = "",
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val mediaInfo: MediaInfo = MediaInfo(),
    val episodes: List<Episode> = emptyList()
)

data class MediaInfo(
    val resolution: String = "1080p",
    val videoCodec: String = "AVC / x264",
    val bitrate: String = "2299 kb/s",
    val frameRate: String = "24.00 FPS",
    val aspectRatio: String = "2.35:1",
    val fileSize: String = "2.3 GB",
    val audioTracks: List<String> = listOf("Hindi (AAC 5.1)", "Tamil (AAC 5.1)"),
    val subtitleTracks: List<String> = listOf("English (UTF-8)"),
    val qualityBadges: List<String> = listOf("1080p", "x264", "Dual Audio", "ESub")
)

data class Episode(
    val episodeNumber: Int = 1,
    val seasonNumber: Int = 1,
    val title: String = "Episode 1",
    val thumbnailUrl: String = "",
    val streamUrl: String = "",
    val mirrorStreamUrl: String = "",
    val telegramFileId: String = "",
    val durationMs: Long = 0L,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f
)

data class PlaybackProgress(
    val mediaId: String,
    val episodeNumber: Int,
    val positionMs: Long,
    val durationMs: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)
