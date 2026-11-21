package com.streamhub.app.ui.screens.details

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * [DetailsScreen.kt] is the Master Orchestrator for the Media Details experience in StreamHub.
 *
 * It seamlessly binds:
 * 1. [DetailsHeroBackdrop]: 16:9 trailer player & cinematic gradient backdrop.
 * 2. [DetailsTitleMetadataSection]: High-res poster, dynamic badges (AniList/IMDb, Studio, Genre chips).
 * 3. [DetailsActionButtonsRow]: Play/Resume button and My List folder organizer.
 * 4. [DetailsMainOverviewSection]: Technical MediaInfo specs chips, synopsis, and directors/writers.
 * 5. [DetailsCastSection]: Voice Cast / Ensemble cast carousel with avatars and character names.
 * 6. [DetailsTrailersSection]: Official Trailers, Teasers, and PVs carousel.
 * 7. [DetailsFranchiseSection]: Universe franchise carousel (prequels, sequels, movies, OVAs).
 * 8. 3-Tab Content System:
 *    - Tab 0: [DetailsEpisodesHeader] + [EpisodeRowItem] list with Season/Arc selectors.
 *    - Tab 1: [DetailsExtendedInfoTab] with technical broadcast & production metadata.
 *    - Tab 2: [DetailsRecommendationsTab] with AI & TMDb/AniList similar titles.
 * 9. Dialogs & Sheets:
 *    - [SeasonArcSelectorSheet]: Season & Arc picker.
 *    - [BatchDownloadSheet]: Multi-episode mass download manager.
 *    - [ArcEpisodeEditorDialog]: In-place episode & arc metadata editing.
 *    - [AdminEditorDialog]: Full Creator Studio editor.
 *    - [FolderSelectionDialog]: Custom My List bookmark folder categorization.
 * =========================================================================================
 */

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.data.repository.CatalogState
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.EpisodeOrderingManager
import com.streamhub.app.data.FranchiseManager
import com.streamhub.app.data.api.ExtendedMediaDetails
import com.streamhub.app.data.api.MetadataFetchManager
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.NetworkMonitor
import com.streamhub.app.data.SeasonArcOption
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.MediaTrailer
import com.streamhub.app.player.StreamPreloadManager
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.components.AppEmptyState
import com.streamhub.app.ui.components.AppErrorState
import com.streamhub.app.ui.components.AppLoadingState
import com.streamhub.app.ui.components.ArcEpisodeEditorDialog
import com.streamhub.app.ui.components.BatchDownloadSheet
import com.streamhub.app.ui.components.FolderSelectionDialog
import com.streamhub.app.ui.components.SeasonArcSelectorSheet
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.components.TrailerPlayerDialog
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

val LocalIsScrollInProgress = compositionLocalOf { false }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailsScreen(
    mediaId: String,
    initialEpisodeIndex: Int = -1,
    repository: CatalogRepository,
    onBackClick: () -> Unit,
    onPlayEpisode: (MediaItem, Int) -> Unit,
    onMediaClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnline by NetworkMonitor.isOnline.collectAsState()
    val catalog by repository.mediaCatalog.collectAsState()
    val downloads by DownloadManager.downloads.collectAsState()
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    var showAdminEditDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isSeasonSheetOpen by remember { mutableStateOf(false) }
    var isBatchDownloadSheetOpen by remember { mutableStateOf(false) }
    var isTrailerPlaying by remember { mutableStateOf(false) }
    var recommendations by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var extendedDetails by remember(mediaId) {
        mutableStateOf(
            MetadataFetchManager.getCachedExtendedDetails(mediaId) ?:
            ExtendedMediaDetails(
                director = "",
                writers = "",
                castMembers = emptyList(),
                trailers = emptyList()
            )
        )
    }
    var activeTrailerVideoId by remember { mutableStateOf("") }
    var activeTrailerTitle by remember { mutableStateOf("") }

    var currentMediaId by rememberSaveable(mediaId) { mutableStateOf(mediaId) }
    LaunchedEffect(mediaId) {
        currentMediaId = mediaId
    }

    val mediaItem = remember(currentMediaId, catalog, downloads) {
        catalog.firstOrNull { 
            it.id == currentMediaId ||
            (it.tmdbId.isNotBlank() && (it.tmdbId == currentMediaId || it.tmdbId == currentMediaId.removePrefix("tmdb_rec_").removePrefix("tmdb_"))) ||
            (it.anilistId.isNotBlank() && (it.anilistId == currentMediaId || it.anilistId == currentMediaId.removePrefix("anilist_rec_").removePrefix("mal_rec_").removePrefix("mal_"))) ||
            (it.title.isNotBlank() && (it.title.equals(currentMediaId, ignoreCase = true) || it.title.replace(":", "").equals(currentMediaId.replace(":", ""), ignoreCase = true)))
        } ?: run {
            // Offline fallback: construct MediaItem from completed downloads if catalog is not available offline
            val downloadedEps = downloads.filter { it.mediaId == currentMediaId && it.isCompleted }
            if (downloadedEps.isNotEmpty()) {
                val first = downloadedEps.first()
                MediaItem(
                    id = currentMediaId,
                    title = first.mediaTitle,
                    posterUrl = first.posterUrl,
                    episodes = downloadedEps.map { d ->
                        Episode(
                            episodeNumber = d.episodeIndex,
                            title = d.episodeTitle,
                            streamUrl = d.localFilePath
                        )
                    }
                )
            } else null
        }
    }

    val effectiveSeasonNumber = remember(mediaItem) {
        if (mediaItem != null) FranchiseManager.getEffectiveSeasonNumber(mediaItem) else 1
    }

    val distinctArcs = remember(mediaItem?.episodes) {
        mediaItem?.episodes?.mapNotNull { it.arcName.trim().takeIf { a -> a.isNotEmpty() } }?.distinct() ?: emptyList()
    }

    var selectedSeasonNumber by rememberSaveable(currentMediaId, effectiveSeasonNumber) {
        mutableIntStateOf(effectiveSeasonNumber)
    }
    var selectedArcName by rememberSaveable(currentMediaId) {
        mutableStateOf(distinctArcs.firstOrNull() ?: "")
    }
    var isArcSheetOpen by remember { mutableStateOf(false) }
    var selectedArcForEdit by remember { mutableStateOf<SeasonArcOption?>(null) }
    var showFolderSelectionDialog by remember { mutableStateOf(false) }

    // Derive Franchise universe items
    val franchiseItems = remember(mediaItem, catalog) {
        if (mediaItem != null) {
            FranchiseManager.getFranchiseItems(mediaItem, catalog)
        } else emptyList()
    }

    // Build Season and Arc dropdown options separately
    val seasonOptions = remember(mediaItem, catalog) {
        if (mediaItem != null) {
            FranchiseManager.buildSeasonOptions(mediaItem, catalog)
        } else emptyList()
    }

    val arcOptions = remember(mediaItem) {
        if (mediaItem != null) {
            FranchiseManager.buildArcOptions(mediaItem)
        } else emptyList()
    }

    val isMovie = mediaItem?.category?.equals("MOVIE", true) == true || 
                  mediaItem?.category?.equals("Movies", true) == true || 
                  mediaItem?.type?.equals("MOVIE", true) == true
    val isAnime = mediaItem?.category?.equals("Anime", true) == true

    val prewarmCoroutineScope = rememberCoroutineScope()
    val savedProgress = remember(currentMediaId) { WatchHistoryManager.getProgress(currentMediaId) }
    val detailsTargetPlayIndex = remember(savedProgress, mediaItem?.episodes) {
        if (savedProgress != null && mediaItem?.episodes != null) {
            val titleMatchIdx = if (savedProgress.episodeTitle.isNotBlank()) {
                mediaItem.episodes.indexOfFirst {
                    it.title.equals(savedProgress.episodeTitle, ignoreCase = true) &&
                    (it.seasonNumber == savedProgress.seasonNumber || isMovie)
                }
            } else -1
            if (titleMatchIdx >= 0) {
                titleMatchIdx
            } else if (savedProgress.episodeNumber in mediaItem.episodes.indices) {
                savedProgress.episodeNumber
            } else 0
        } else 0
    }

    LaunchedEffect(mediaItem?.id, mediaItem?.tmdbId, mediaItem?.anilistId) {
        if (mediaItem != null) {
            val isAnimeMedia = mediaItem.category.equals("ANIME", ignoreCase = true) ||
                               mediaItem.category.equals("ANIMES", ignoreCase = true) ||
                               mediaItem.type.equals("ANIME", ignoreCase = true) ||
                               mediaItem.category.contains("Anime", ignoreCase = true)
            val cached = MetadataFetchManager.getCachedExtendedDetails(mediaItem.id.ifBlank { mediaItem.title })
            if (cached != null && cached.castMembers.isNotEmpty()) {
                extendedDetails = cached
            } else if (mediaItem.castMembers.isNotEmpty() && (mediaItem.trailers.isNotEmpty() || mediaItem.director.isNotBlank())) {
                extendedDetails = ExtendedMediaDetails(
                    director = mediaItem.director,
                    writers = mediaItem.writers,
                    castMembers = mediaItem.castMembers,
                    trailers = mediaItem.trailers
                )
            }

            // Live dynamic sync: Fetch latest TMDb / AniList metadata in background
            try {
                val hasAnimeCartoonAvatars = isAnimeMedia && mediaItem.castMembers.any { it.profileUrl.contains("/character/") }
                val force = hasAnimeCartoonAvatars || mediaItem.castMembers.isEmpty() || mediaItem.castMembers.any { it.profileUrl.isBlank() } || mediaItem.trailers.isEmpty()
                val fetchedExt = MetadataFetchManager.fetchExtendedDetails(mediaItem, forceRefresh = force)
                if (fetchedExt.castMembers.isNotEmpty() || fetchedExt.trailers.isNotEmpty() || fetchedExt.director.isNotBlank()) {
                    extendedDetails = fetchedExt
                }
            } catch (e: Exception) {
                Log.w("DetailsScreen", "Failed to load dynamic extended details: ${e.message}")
            }
        }

        val isAnimeMedia = mediaItem?.category.equals("ANIME", ignoreCase = true) ||
                           mediaItem?.category.equals("ANIMES", ignoreCase = true) ||
                           mediaItem?.type.equals("ANIME", ignoreCase = true) ||
                           mediaItem?.category?.contains("Anime", ignoreCase = true) == true
        val tId = mediaItem?.tmdbId?.trim() ?: ""
        val aId = mediaItem?.anilistId?.trim() ?: ""

        if (isAnimeMedia) {
            if (aId.isNotBlank()) {
                try {
                    val fetched = MetadataFetchManager.fetchAniListRecommendations(aId)
                    if (fetched.isNotEmpty()) {
                        recommendations = fetched
                    }
                } catch (e: Exception) {
                    Log.w("DetailsScreen", "Failed to load AniList recs: ${e.message}")
                }
            }
        } else if (tId.isNotBlank()) {
            try {
                val fetched = MetadataFetchManager.fetchTMDBRecommendations(tId, isMovie)
                if (fetched.isNotEmpty()) {
                    recommendations = fetched
                }
            } catch (e: Exception) {
                Log.w("DetailsScreen", "Failed to load TMDB recs: ${e.message}")
            }
        }
    }

    val catalogState by repository.catalogState.collectAsState()

    if (mediaItem == null) {
        if (!isOnline) {
            AppEmptyState(
                title = "Offline — Title Unavailable",
                subtitle = "This title hasn't been downloaded for offline viewing. Connect to internet to view details.",
                ctaLabel = "Go Back",
                onCtaClick = onBackClick
            )
            return
        }
        when (val state = catalogState) {
            is CatalogState.Loading -> {
                AppLoadingState(message = "Loading details…")
            }
            is CatalogState.Error -> {
                AppErrorState(
                    message = state.message,
                    onRetry = { repository.refreshCatalog() },
                    title = "Failed to load content"
                )
            }
            is CatalogState.Ready -> {
                AppEmptyState(
                    title = "Content Not Found",
                    subtitle = "This title may have been moved or removed from the catalog.",
                    ctaLabel = "Go Back",
                    onCtaClick = onBackClick
                )
            }
        }
        return
    }

    val cleanTrailerId = remember(mediaItem.trailerId) {
        val raw = mediaItem.trailerId.trim()
        when {
            raw.isBlank() || raw.equals("null", ignoreCase = true) -> ""
            raw.contains("v=") -> raw.substringAfter("v=").substringBefore("&")
            raw.contains("youtu.be/") -> raw.substringAfter("youtu.be/").substringBefore("?")
            else -> raw
        }
    }

    // High-Res Cinematic Backdrop Image (Prioritize TMDB Banner -> YouTube MaxRes -> Poster)
    val backdropUrl = remember(mediaItem.bannerUrl, cleanTrailerId, mediaItem.posterUrl) {
        when {
            mediaItem.bannerUrl.isNotBlank() -> mediaItem.bannerUrl
            cleanTrailerId.isNotBlank() -> "https://img.youtube.com/vi/$cleanTrailerId/maxresdefault.jpg"
            else -> mediaItem.posterUrl
        }
    }

    // Combined list of Official Trailers, Teasers, and PVs
    val allTrailers = remember(cleanTrailerId, extendedDetails.trailers) {
        val list = mutableListOf<MediaTrailer>()
        if (cleanTrailerId.isNotBlank()) {
            list.add(MediaTrailer(id = cleanTrailerId, title = "Official Main Trailer", type = "Trailer", isOfficial = true))
        }
        extendedDetails.trailers.forEach { t ->
            if (list.none { it.id == t.id }) list.add(t)
        }
        list
    }

    // Filter Episodes based on selected Season Number or Arc Name
    val seasonFilteredEpisodes = remember(mediaItem.episodes, selectedSeasonNumber, selectedArcName, isMovie) {
        val allEps = mediaItem.episodes
        when {
            isMovie -> allEps
            selectedArcName.isNotBlank() -> allEps.filter { it.arcName.equals(selectedArcName, ignoreCase = true) }
            selectedSeasonNumber > 0 -> {
                val filtered = allEps.filter { it.seasonNumber == selectedSeasonNumber }
                if (filtered.isEmpty() && (selectedSeasonNumber == 1 || selectedSeasonNumber == effectiveSeasonNumber)) allEps else filtered
            }
            else -> allEps
        }
    }

    // Speculatively pre-warm episodes of the current season in the background
    val episodesToPrewarm = remember(detailsTargetPlayIndex, seasonFilteredEpisodes, mediaItem.episodes, selectedSeasonNumber) {
        val resumeEp = mediaItem.episodes.getOrNull(detailsTargetPlayIndex)
        val primaryEp = if (resumeEp != null && (selectedSeasonNumber <= 0 || resumeEp.seasonNumber == selectedSeasonNumber)) {
            resumeEp
        } else {
            seasonFilteredEpisodes.firstOrNull() ?: mediaItem.episodes.firstOrNull()
        }
        val targetUrl = primaryEp?.streamUrl?.ifEmpty { primaryEp.mirrorStreamUrl } ?: ""
        val list = mutableListOf<String>()
        if (targetUrl.isNotBlank() && !targetUrl.startsWith("/")) {
            list.add(targetUrl)
        }
        seasonFilteredEpisodes.forEach { ep ->
            val url = ep.streamUrl.ifEmpty { ep.mirrorStreamUrl }
            if (url.isNotBlank() && !url.startsWith("/") && url != targetUrl) {
                list.add(url)
            }
        }
        list
    }

    LaunchedEffect(currentMediaId, selectedSeasonNumber, selectedArcName, episodesToPrewarm, isOnline) {
        if (isOnline && episodesToPrewarm.isNotEmpty()) {
            StreamPreloadManager.prewarmEpisodesBatch(context, episodesToPrewarm, prewarmCoroutineScope)
        }
    }

    val episodeIndexMap = remember(mediaItem.episodes) {
        mediaItem.episodes.withIndex().associate { (i, ep) -> ep to i }
    }

    Scaffold(
        floatingActionButton = {
            if (isAdminMode) {
                FloatingActionButton(
                    onClick = { showAdminEditDialog = true },
                    containerColor = AccentOrange,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Show Specs")
                }
            }
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { innerPadding ->
        val detailsListState = rememberLazyListState()

        val currentFranchiseIndex = remember(franchiseItems, mediaItem.id) {
            franchiseItems.indexOfFirst { it.id == mediaItem.id }.coerceAtLeast(0)
        }
        val franchiseRowState = rememberSaveable(
            mediaItem.id,
            saver = LazyListState.Saver
        ) {
            LazyListState(
                firstVisibleItemIndex = currentFranchiseIndex,
                firstVisibleItemScrollOffset = 0
            )
        }

        val historyMap by WatchHistoryManager.historyFlow.collectAsState()
        val mediaProgress = historyMap[mediaItem.id] ?: remember(mediaItem.id) { WatchHistoryManager.getProgress(mediaItem.id) }

        // Auto-scroll to exact episode in list
        var lastScrolledEpisodeNumber by remember(mediaItem.id) { mutableIntStateOf(-1) }
        var lastScrolledTimestamp by remember(mediaItem.id) { mutableStateOf(0L) }
        var glowingEpisodeIndex by remember(mediaItem.id) { mutableStateOf<Int?>(null) }

        // 5-second glowing border timer in app theme
        LaunchedEffect(glowingEpisodeIndex) {
            if (glowingEpisodeIndex != null) {
                delay(5000L)
                glowingEpisodeIndex = null
            }
        }

        LaunchedEffect(
            mediaItem.id,
            mediaProgress?.episodeNumber,
            mediaProgress?.lastUpdated,
            initialEpisodeIndex,
            seasonFilteredEpisodes.size
        ) {
            val progress = mediaProgress ?: WatchHistoryManager.getProgress(mediaItem.id)
            val targetEpNumber = when {
                progress != null -> progress.episodeNumber
                initialEpisodeIndex >= 0 -> initialEpisodeIndex
                else -> null
            }
            val progressUpdated = progress?.lastUpdated ?: System.currentTimeMillis()

            if (targetEpNumber != null && mediaItem.episodes.isNotEmpty() && (!isMovie || mediaItem.episodes.size > 1)) {
                val isNewTarget = lastScrolledEpisodeNumber != targetEpNumber || progressUpdated > lastScrolledTimestamp
                if (isNewTarget) {
                    val targetEp = when {
                        progress != null && progress.episodeTitle.isNotBlank() -> {
                            mediaItem.episodes.find { it.title.equals(progress.episodeTitle, ignoreCase = true) }
                                ?: mediaItem.episodes.getOrNull(targetEpNumber)
                                ?: mediaItem.episodes.find { it.episodeNumber == targetEpNumber }
                        }
                        else -> {
                            mediaItem.episodes.getOrNull(targetEpNumber)
                                ?: mediaItem.episodes.find { it.episodeNumber == targetEpNumber }
                        }
                    }

                    if (targetEp != null) {
                        selectedTabIndex = 0
                        if (targetEp.seasonNumber > 0 && selectedSeasonNumber != targetEp.seasonNumber) {
                            selectedSeasonNumber = targetEp.seasonNumber
                        }
                        if (targetEp.arcName.isNotBlank() && !selectedArcName.equals(targetEp.arcName, ignoreCase = true)) {
                            selectedArcName = targetEp.arcName
                        }

                        // Give Compose a brief delay to apply season/arc filter updates to the list
                        delay(150)

                        val currentFiltered = when {
                            isMovie -> mediaItem.episodes
                            targetEp.arcName.isNotBlank() -> mediaItem.episodes.filter { it.arcName.equals(targetEp.arcName, ignoreCase = true) }
                            targetEp.seasonNumber > 0 -> {
                                val filtered = mediaItem.episodes.filter { it.seasonNumber == targetEp.seasonNumber }
                                if (filtered.isEmpty()) mediaItem.episodes else filtered
                            }
                            else -> mediaItem.episodes
                        }

                        val filteredIndex = currentFiltered.indexOfFirst {
                            it == targetEp || (it.episodeNumber == targetEp.episodeNumber && it.seasonNumber == targetEp.seasonNumber)
                        }
                        if (filteredIndex >= 0) {
                            lastScrolledEpisodeNumber = targetEpNumber
                            lastScrolledTimestamp = progressUpdated
                            detailsListState.animateScrollToItem(
                                index = 3 + filteredIndex,
                                scrollOffset = -40
                            )
                            val originalIdx = episodeIndexMap[targetEp] ?: mediaItem.episodes.indexOf(targetEp)
                            glowingEpisodeIndex = originalIdx
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(LocalIsScrollInProgress provides detailsListState.isScrollInProgress) {
            LazyColumn(
                state = detailsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Header Backdrop Container (Modern 16:9 Hero Trailer Player)
                item {
                    val activeVideoId = if (activeTrailerVideoId.isNotBlank()) activeTrailerVideoId else if (isTrailerPlaying) cleanTrailerId else ""
                    val activeTitle = if (activeTrailerVideoId.isNotBlank()) activeTrailerTitle else mediaItem.title

                    if (activeVideoId.isNotBlank()) {
                        TrailerPlayerDialog(
                            videoId = activeVideoId,
                            title = activeTitle,
                            onDismiss = {
                                isTrailerPlaying = false
                                activeTrailerVideoId = ""
                                activeTrailerTitle = ""
                            }
                        )
                    }

                    DetailsHeroBackdrop(
                        mediaItem = mediaItem,
                        backdropUrl = backdropUrl,
                        cleanTrailerId = cleanTrailerId,
                        onBackClick = onBackClick,
                        onWatchTrailerClick = { isTrailerPlaying = true }
                    )
                }

                // Main Info Section (Poster, Title, Badges, Action Buttons, Synopsis, Cast, Trailers, Franchise)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        DetailsTitleMetadataSection(mediaItem = mediaItem)

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons Row: Play Movie / Resume Ep + My List
                        val myListSet by MyListManager.myListFlow.collectAsState()
                        val isBookmarked = myListSet.contains(mediaItem.id)

                        val savedHistory = remember(mediaItem.id) { WatchHistoryManager.getProgress(mediaItem.id) }
                        val completedDownloads = remember(downloads, mediaItem.id) {
                            downloads.filter { it.mediaId == mediaItem.id && it.isCompleted }
                        }
                        val targetPlayIndex = if (savedHistory != null && mediaItem.episodes.isNotEmpty()) {
                            val titleMatchIdx = if (savedHistory.episodeTitle.isNotBlank()) {
                                mediaItem.episodes.indexOfFirst {
                                    it.title.equals(savedHistory.episodeTitle, ignoreCase = true) &&
                                    (it.seasonNumber == savedHistory.seasonNumber || isMovie)
                                }
                            } else -1
                            if (titleMatchIdx >= 0) {
                                titleMatchIdx
                            } else if (savedHistory.episodeNumber in mediaItem.episodes.indices) {
                                savedHistory.episodeNumber
                            } else 0
                        } else 0

                        val playButtonText = when {
                            !isOnline && completedDownloads.isEmpty() -> "Requires Internet"
                            !isOnline && completedDownloads.isNotEmpty() -> "Play Offline 📥"
                            isMovie -> if (savedHistory != null && savedHistory.positionMs > 5000L) "Resume Movie" else "Play Movie"
                            savedHistory != null && targetPlayIndex in mediaItem.episodes.indices -> {
                                val ep = mediaItem.episodes[targetPlayIndex]
                                val displayEpNum = ep.episodeNumber.takeIf { it > 0 } ?: (targetPlayIndex + 1)
                                "Resume Ep $displayEpNum"
                            }
                            else -> "Play"
                        }

                        DetailsActionButtonsRow(
                            playButtonText = playButtonText,
                            isOnline = isOnline,
                            hasCompletedDownloads = completedDownloads.isNotEmpty(),
                            isBookmarked = isBookmarked,
                            onPlayClick = {
                                StreamPreloadManager.cancelDetailsPrewarm()
                                if (!isOnline) {
                                    if (completedDownloads.isEmpty()) {
                                        ToastManager.showToast("Cannot stream while offline. Download episodes or connect to internet.", Icons.Default.WifiOff)
                                        return@DetailsActionButtonsRow
                                    }
                                    val targetDownload = completedDownloads.firstOrNull { it.episodeIndex == targetPlayIndex }
                                        ?: completedDownloads.first()
                                    val targetIndex = completedDownloads.indexOfFirst { it.episodeIndex == targetDownload.episodeIndex }.coerceAtLeast(0)
                                    val offlineMedia = MediaItem(
                                        id = "offline:${targetDownload.mediaId}:${targetDownload.episodeIndex}",
                                        title = targetDownload.mediaTitle,
                                        posterUrl = targetDownload.posterUrl,
                                        episodes = completedDownloads.map { dl ->
                                            Episode(
                                                episodeNumber = dl.episodeIndex,
                                                title = dl.episodeTitle,
                                                streamUrl = dl.localFilePath
                                            )
                                        }
                                    )
                                    onPlayEpisode(offlineMedia, targetIndex)
                                    return@DetailsActionButtonsRow
                                }
                                onPlayEpisode(mediaItem, targetPlayIndex)
                            },
                            onBookmarkToggle = {
                                val added = MyListManager.toggleBookmark(mediaItem.id)
                                val msg = if (added) "Added to Favorites • Hold to choose folder" else "Removed from My List"
                                ToastManager.showToast(msg)
                            },
                            onBookmarkLongClick = {
                                showFolderSelectionDialog = true
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Overview Section (MediaInfo Specs, Synopsis, Directors/Writers)
                        DetailsMainOverviewSection(
                            mediaItem = mediaItem,
                            extendedDetails = extendedDetails
                        )

                        // Cast Section
                        DetailsCastSection(
                            mediaItem = mediaItem,
                            extendedDetails = extendedDetails
                        )

                        // Trailers Section
                        DetailsTrailersSection(
                            allTrailers = allTrailers,
                            onTrailerClick = { trailer ->
                                activeTrailerVideoId = trailer.id
                                activeTrailerTitle = trailer.title
                            }
                        )

                        // Franchise Section
                        DetailsFranchiseSection(
                            mediaItem = mediaItem,
                            franchiseItems = franchiseItems,
                            franchiseRowState = franchiseRowState,
                            onMediaClick = onMediaClick
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val primaryColor = MaterialTheme.colorScheme.primary

                        // 3-Tab Header (EPISODES/STREAMS | MORE INFO | MORE LIKE THIS)
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = BackgroundDark,
                            contentColor = primaryColor,
                            indicator = { tabPositions ->
                                if (selectedTabIndex < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                        color = primaryColor
                                    )
                                }
                            }
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                text = { Text(if (isMovie) "STREAMS" else "EPISODES", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                text = { Text("MORE INFO", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                            Tab(
                                selected = selectedTabIndex == 2,
                                onClick = { selectedTabIndex = 2 },
                                text = { Text("MORE LIKE THIS", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Tab 0: Episodes / Movie Streams List with Smart Season & Arc Picker
                if (selectedTabIndex == 0) {
                    item {
                        if (!isMovie) {
                            DetailsEpisodesHeader(
                                mediaItem = mediaItem,
                                seasonFilteredEpisodes = seasonFilteredEpisodes,
                                seasonOptions = seasonOptions,
                                arcOptions = arcOptions,
                                selectedSeasonNumber = selectedSeasonNumber,
                                selectedArcName = selectedArcName,
                                onOpenSeasonSheet = { isSeasonSheetOpen = true },
                                onOpenArcSheet = { isArcSheetOpen = true },
                                onOpenBatchDownloadSheet = { isBatchDownloadSheetOpen = true }
                            )
                        }
                    }

                    if (seasonFilteredEpisodes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDark)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val emptyLabel = if (selectedArcName.isNotBlank()) selectedArcName else "Season $selectedSeasonNumber"
                                    Text(
                                        text = if (isMovie) "Movie Stream Indexing in Progress" else "$emptyLabel Episodes Coming Soon",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isMovie) "Stream links will appear here once attached via Creator Studio." else "Episodes will appear once indexed.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(seasonFilteredEpisodes, key = { _, episode -> "${episode.seasonNumber}_${episode.episodeNumber}_${episode.title}" }) { index, episode ->
                            val originalIndex = episodeIndexMap[episode] ?: index
                            val downloadItem = downloads.firstOrNull { it.mediaId == mediaItem.id && it.episodeIndex == originalIndex }
                            val isDownloaded = downloadItem?.isCompleted == true
                            val isQueued = downloadItem?.isQueued == true
                            val isDownloading = downloadItem != null && !downloadItem.isCompleted && !downloadItem.isPaused && !downloadItem.isCanceled && !downloadItem.isQueued
                            val downloadProgress = downloadItem?.progressPercent ?: 0
                            val isEpisodeGlowing = glowingEpisodeIndex != null && glowingEpisodeIndex == originalIndex
                            EpisodeRowItem(
                                episode = episode,
                                index = originalIndex,
                                mediaItem = mediaItem,
                                isDownloaded = isDownloaded,
                                isDownloading = isDownloading,
                                isQueued = isQueued,
                                downloadProgress = downloadProgress,
                                isOnline = isOnline,
                                isGlowing = isEpisodeGlowing,
                                onPlay = {
                                    StreamPreloadManager.cancelDetailsPrewarm()
                                    if (downloadItem != null && downloadItem.isCompleted) {
                                        val completedForShow = downloads.filter { it.mediaId == mediaItem.id && it.isCompleted }.sortedBy { it.episodeIndex }
                                        val playlist = if (completedForShow.isNotEmpty()) completedForShow else listOf(downloadItem)
                                        val targetIndex = playlist.indexOfFirst { it.episodeIndex == downloadItem.episodeIndex }.coerceAtLeast(0)
                                        val offlineMedia = MediaItem(
                                            id = "offline:${downloadItem.mediaId}:${downloadItem.episodeIndex}",
                                            title = downloadItem.mediaTitle,
                                            posterUrl = downloadItem.posterUrl,
                                            episodes = playlist.map { dl ->
                                                Episode(
                                                    episodeNumber = dl.episodeIndex,
                                                    title = dl.episodeTitle,
                                                    streamUrl = dl.localFilePath
                                                )
                                            }
                                        )
                                        onPlayEpisode(offlineMedia, targetIndex)
                                    } else if (!isOnline) {
                                        ToastManager.showToast("Cannot stream while offline. Connect to internet first.", Icons.Default.WifiOff)
                                    } else {
                                        onPlayEpisode(mediaItem, originalIndex)
                                    }
                                },
                                onDownload = { 
                                    if (!isOnline) {
                                        ToastManager.showToast("Cannot download while offline. Connect to internet first.", Icons.Default.WifiOff)
                                    } else if (isDownloaded) {
                                        ToastManager.showToast("Episode is already downloaded for offline playback", Icons.Default.PlayArrow)
                                    } else {
                                        ToastManager.showToast("Starting download...", Icons.Default.Download)
                                        DownloadManager.startDownload(context, mediaItem, originalIndex) 
                                    }
                                },
                                onCancelDownload = {
                                    if (downloadItem != null) {
                                        DownloadManager.deleteDownload(downloadItem)
                                        ToastManager.showToast("Download cancelled", Icons.Default.Close)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Tab 1: More Info Specs
                if (selectedTabIndex == 1) {
                    item {
                        DetailsExtendedInfoTab(
                            mediaItem = mediaItem,
                            extendedDetails = extendedDetails,
                            allTrailers = allTrailers
                        )
                    }
                }

                // Tab 2: More Like This (Recommendations)
                if (selectedTabIndex == 2) {
                    item {
                        DetailsRecommendationsTab(
                            isMovie = isMovie,
                            isAnime = isAnime,
                            recommendations = recommendations,
                            catalog = catalog,
                            onMediaClick = onMediaClick
                        )
                    }
                }
            }
        }
    }

    if (isSeasonSheetOpen) {
        SeasonArcSelectorSheet(
            title = "Select Season & Media",
            universeTitle = mediaItem.franchiseTitle.ifBlank { mediaItem.title },
            options = seasonOptions,
            selectedSeasonNumber = selectedSeasonNumber,
            selectedArcName = "",
            currentMedia = mediaItem,
            onDismiss = { isSeasonSheetOpen = false },
            onSelectOption = { opt ->
                isSeasonSheetOpen = false
                if (opt.isExternalMedia && opt.targetMediaItem != null) {
                    onMediaClick(opt.targetMediaItem)
                }
            }
        )
    }

    if (isArcSheetOpen) {
        SeasonArcSelectorSheet(
            title = "Select Story Arc",
            universeTitle = mediaItem.title,
            options = arcOptions,
            selectedSeasonNumber = selectedSeasonNumber,
            selectedArcName = selectedArcName,
            currentMedia = mediaItem,
            onDismiss = { isArcSheetOpen = false },
            onSelectOption = { opt ->
                isArcSheetOpen = false
                selectedArcName = opt.internalArcName
            },
            onEditArc = { opt ->
                isArcSheetOpen = false
                selectedArcForEdit = opt
            }
        )
    }

    if (isBatchDownloadSheetOpen) {
        val currentOpt = seasonOptions.firstOrNull { it.isCurrent }
        val seasonLabel = currentOpt?.shortLabel ?: "Season $selectedSeasonNumber"
        BatchDownloadSheet(
            mediaItem = mediaItem,
            episodes = seasonFilteredEpisodes,
            episodeIndexMap = episodeIndexMap,
            seasonLabel = seasonLabel,
            onDismiss = { isBatchDownloadSheetOpen = false },
            onConfirmDownload = { selectedIndices ->
                ToastManager.showToast("Queued ${selectedIndices.size} episodes for download", Icons.Default.Download)
                DownloadManager.enqueueBatch(context, mediaItem, selectedIndices)
            }
        )
    }

    selectedArcForEdit?.let { arcOpt ->
        ArcEpisodeEditorDialog(
            mediaItem = mediaItem,
            arcOption = arcOpt,
            onDismiss = { selectedArcForEdit = null },
            onSaveArcEpisodes = { updatedArcEpisodes ->
                val targetArc = arcOpt.internalArcName
                val targetSeason = arcOpt.internalSeasonNumber.coerceAtLeast(1)

                val otherEpisodes = mediaItem.episodes.filterNot { ep ->
                    if (targetArc.isNotBlank()) {
                        ep.arcName.equals(targetArc, ignoreCase = true)
                    } else {
                        ep.seasonNumber == targetSeason
                    }
                }

                val mergedEpisodes = EpisodeOrderingManager.normalizeAndSort(otherEpisodes + updatedArcEpisodes)

                val updatedMediaItem = mediaItem.copy(episodes = mergedEpisodes)
                repository.saveMediaItem(updatedMediaItem)
                val newArcName = updatedArcEpisodes.firstOrNull()?.arcName ?: ""
                if (selectedArcName.equals(targetArc, ignoreCase = true) && newArcName.isNotBlank()) {
                    selectedArcName = newArcName
                }
                selectedArcForEdit = null
            }
        )
    }

    if (showAdminEditDialog) {
        AdminEditorDialog(
            initialItem = mediaItem,
            onDismiss = { showAdminEditDialog = false },
            onSave = { updatedItem ->
                StreamPreloadManager.cancelDetailsPrewarm()
                StreamPreloadManager.cancelBingePrecache(resetCompleted = true)
                repository.saveMediaItem(updatedItem)
                showAdminEditDialog = false
            },
            onDelete = { itemId ->
                repository.deleteMediaItem(itemId)
                showAdminEditDialog = false
                onBackClick()
            }
        )
    }

    if (showFolderSelectionDialog) {
        FolderSelectionDialog(
            mediaItem = mediaItem,
            onDismiss = { showFolderSelectionDialog = false }
        )
    }
}
