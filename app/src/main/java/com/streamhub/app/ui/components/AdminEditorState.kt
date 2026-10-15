package com.streamhub.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.isActivelyTrending

/**
 * Hoisted state holder for Creator Studio (AdminEditorDialog).
 * Decouples mutable state from UI composables, preventing DEX register overflow.
 */
class AdminEditorState(
    val initialItem: MediaItem?
) {
    var activeEditItem by mutableStateOf(initialItem)
    var title by mutableStateOf(initialItem?.title ?: "")
    var type by mutableStateOf(initialItem?.type ?: "SERIES")
    var category by mutableStateOf(initialItem?.category ?: "ANIME")
    var anilistId by mutableStateOf(initialItem?.anilistId ?: "")
    var tmdbId by mutableStateOf(initialItem?.tmdbId ?: "")
    var trailerId by mutableStateOf(initialItem?.trailerId?.takeIf { !it.equals("null", ignoreCase = true) } ?: "")
    var rating by mutableStateOf(initialItem?.rating ?: "")
    var maturityRating by mutableStateOf(initialItem?.maturityRating ?: "")
    var studio by mutableStateOf(initialItem?.studio ?: "")
    var synonyms by mutableStateOf(initialItem?.synonyms ?: "")
    var totalEpisodes by mutableStateOf(initialItem?.totalEpisodes ?: "")
    var status by mutableStateOf(initialItem?.status ?: "")
    var aired by mutableStateOf(initialItem?.aired ?: "")
    var premiered by mutableStateOf(initialItem?.premiered ?: "")
    var producers by mutableStateOf(initialItem?.producers ?: "")
    var source by mutableStateOf(initialItem?.source ?: "")
    var duration by mutableStateOf(initialItem?.duration ?: "")
    var genresText by mutableStateOf(initialItem?.genres?.joinToString(", ") ?: "")
    var castText by mutableStateOf(initialItem?.castList?.joinToString(", ") ?: "")
    var posterUrl by mutableStateOf(initialItem?.posterUrl?.takeIf { !it.equals("null", ignoreCase = true) } ?: "")
    var bannerUrl by mutableStateOf(
        initialItem?.bannerUrl?.takeIf { !it.equals("null", ignoreCase = true) && it.isNotBlank() }
            ?: initialItem?.posterUrl?.takeIf { !it.equals("null", ignoreCase = true) }
            ?: ""
    )
    var description by mutableStateOf(initialItem?.description ?: "")
    var isFeatured by mutableStateOf(initialItem?.isFeatured ?: false)
    var isTrending by mutableStateOf(initialItem?.isActivelyTrending() ?: false)
    var trendingAt by mutableStateOf(if (initialItem?.isActivelyTrending() == true) initialItem.trendingAt else 0L)

    val isAnimeCat: Boolean
        get() = category.equals("Anime", ignoreCase = true) ||
                category.equals("ANIMES", ignoreCase = true) ||
                category.contains("Anime", ignoreCase = true) ||
                type.equals("ANIME", ignoreCase = true)

    val isInitMovie = initialItem?.type?.equals("MOVIE", ignoreCase = true) == true ||
                      initialItem?.category?.equals("Movie", ignoreCase = true) == true ||
                      initialItem?.category?.equals("Movies", ignoreCase = true) == true ||
                      initialItem?.relationType?.equals("Movie", ignoreCase = true) == true

    var franchiseId by mutableStateOf(initialItem?.franchiseId ?: "")
    var franchiseTitle by mutableStateOf(initialItem?.franchiseTitle ?: "")
    var seasonNumberText by mutableStateOf(
        if (initialItem != null && initialItem.seasonNumber > 0) {
            initialItem.seasonNumber.toString()
        } else if (isInitMovie) {
            ""
        } else {
            "1"
        }
    )
    var partNumberText by mutableStateOf(if ((initialItem?.partNumber ?: 0) > 0) initialItem!!.partNumber.toString() else "")
    var franchiseOrderText by mutableStateOf(
        if ((initialItem?.franchiseOrder ?: 0.0) > 0.0) {
            val ord = initialItem!!.franchiseOrder
            if (ord % 1.0 == 0.0) ord.toInt().toString() else ord.toString()
        } else ""
    )
    var seasonTitle by mutableStateOf(initialItem?.seasonTitle ?: "")
    var relationType by mutableStateOf(
        initialItem?.relationType?.takeIf { it.isNotBlank() && !it.equals("Main Story", ignoreCase = true) }
            ?: if (isInitMovie) "Movie" else "TV"
    )

    var resolution by mutableStateOf(initialItem?.mediaInfo?.resolution ?: "")
    var videoCodec by mutableStateOf(initialItem?.mediaInfo?.videoCodec ?: "")
    var bitrate by mutableStateOf(initialItem?.mediaInfo?.bitrate ?: "")
    var frameRate by mutableStateOf(initialItem?.mediaInfo?.frameRate ?: "")
    var aspectRatio by mutableStateOf(initialItem?.mediaInfo?.aspectRatio ?: "")
    var fileSize by mutableStateOf(initialItem?.mediaInfo?.fileSize ?: "")
    var audioTracksText by mutableStateOf(initialItem?.mediaInfo?.audioTracks?.joinToString(", ") ?: "")
    var subtitleTracksText by mutableStateOf(initialItem?.mediaInfo?.subtitleTracks?.joinToString(", ") ?: "")

    val isMovieFormat: Boolean
        get() = type.equals("MOVIE", ignoreCase = true) || category.equals("MOVIE", ignoreCase = true)

    val currentEpisodes = mutableStateListOf<Episode>().apply {
        addAll(initialItem?.episodes ?: emptyList())
    }

    var startBatchLink by mutableStateOf(
        if (isMovieFormat && (initialItem?.episodes?.size ?: 0) <= 1) (initialItem?.episodes?.firstOrNull()?.streamUrl ?: "") else ""
    )
    var endBatchLink by mutableStateOf("")
    var arcNameText by mutableStateOf("")
    var generatedEpisodesText by mutableStateOf("")
    var f2lBatchInput by mutableStateOf("")
    var isFetchingF2l by mutableStateOf(false)
    var isFetchingMeta by mutableStateOf(false)
    var fetchedFileName by mutableStateOf(initialItem?.episodes?.firstOrNull()?.fileName ?: "")
    var fetchedDurationMs by mutableLongStateOf(initialItem?.episodes?.firstOrNull()?.durationMs ?: 0L)
    var isTestingMovieLink by mutableStateOf(false)
    var movieLinkProbeResult by mutableStateOf<com.streamhub.app.data.api.EpisodeHealthResult?>(null)

    var isFetchingApi by mutableStateOf(false)
    var fetchError by mutableStateOf<String?>(null)
    var batchError by mutableStateOf<String?>(null)
    var batchSuccess by mutableStateOf<String?>(null)
    var validationError by mutableStateOf<String?>(null)
    var showDeleteConfirmDialog by mutableStateOf(false)
    var isCheckingHealth by mutableStateOf(false)
    var healthReport by mutableStateOf<com.streamhub.app.data.api.StreamHealthReport?>(null)
    var showBackupDialog by mutableStateOf(false)
    var showMigrationDialog by mutableStateOf(false)
    var showVoucherDialog by mutableStateOf(false)
    var showMetadataInspector by mutableStateOf(false)

    fun loadItem(itemToEdit: MediaItem) {
        activeEditItem = itemToEdit
        title = itemToEdit.title
        type = itemToEdit.type
        category = itemToEdit.category
        anilistId = itemToEdit.anilistId
        tmdbId = itemToEdit.tmdbId
        trailerId = itemToEdit.trailerId.takeIf { !it.equals("null", ignoreCase = true) } ?: ""
        rating = itemToEdit.rating
        maturityRating = itemToEdit.maturityRating
        studio = itemToEdit.studio
        synonyms = itemToEdit.synonyms
        totalEpisodes = itemToEdit.totalEpisodes
        status = itemToEdit.status
        aired = itemToEdit.aired
        premiered = itemToEdit.premiered
        producers = itemToEdit.producers
        source = itemToEdit.source
        duration = itemToEdit.duration
        genresText = itemToEdit.genres.joinToString(", ")
        castText = itemToEdit.castList.joinToString(", ")
        posterUrl = itemToEdit.posterUrl.takeIf { !it.equals("null", ignoreCase = true) } ?: ""
        bannerUrl = itemToEdit.bannerUrl.takeIf { !it.equals("null", ignoreCase = true) && it.isNotBlank() }
            ?: itemToEdit.posterUrl.takeIf { !it.equals("null", ignoreCase = true) }
            ?: ""
        description = itemToEdit.description
        isFeatured = itemToEdit.isFeatured
        isTrending = itemToEdit.isActivelyTrending()
        trendingAt = if (itemToEdit.isActivelyTrending()) itemToEdit.trendingAt else 0L

        franchiseId = itemToEdit.franchiseId
        franchiseTitle = itemToEdit.franchiseTitle
        val isItemMovie = itemToEdit.type.equals("MOVIE", ignoreCase = true) || itemToEdit.category.equals("Movie", ignoreCase = true)
        seasonNumberText = if (itemToEdit.seasonNumber > 0) {
            itemToEdit.seasonNumber.toString()
        } else if (isItemMovie) {
            ""
        } else {
            "1"
        }
        partNumberText = if (itemToEdit.partNumber > 0) itemToEdit.partNumber.toString() else ""
        franchiseOrderText = if (itemToEdit.franchiseOrder > 0.0) {
            val ord = itemToEdit.franchiseOrder
            if (ord % 1.0 == 0.0) ord.toInt().toString() else ord.toString()
        } else ""
        seasonTitle = itemToEdit.seasonTitle
        relationType = itemToEdit.relationType.takeIf { it.isNotBlank() && !it.equals("Main Story", ignoreCase = true) }
            ?: if (isItemMovie) "Movie" else "TV"

        resolution = itemToEdit.mediaInfo.resolution
        videoCodec = itemToEdit.mediaInfo.videoCodec
        bitrate = itemToEdit.mediaInfo.bitrate
        frameRate = itemToEdit.mediaInfo.frameRate
        aspectRatio = itemToEdit.mediaInfo.aspectRatio
        fileSize = itemToEdit.mediaInfo.fileSize
        audioTracksText = itemToEdit.mediaInfo.audioTracks.joinToString(", ")
        subtitleTracksText = itemToEdit.mediaInfo.subtitleTracks.joinToString(", ")

        currentEpisodes.clear()
        currentEpisodes.addAll(itemToEdit.episodes)
    }
}

@Composable
fun rememberAdminEditorState(initialItem: MediaItem?): AdminEditorState {
    return remember(initialItem) { AdminEditorState(initialItem) }
}
