package com.streamhub.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State of the media catalog fetch.
 * - Loading: initial fetch has not completed yet
 * - Ready: at least one successful Firestore snapshot has been received
 * - Error: Firestore returned an error AND no cached data is available
 *
 * Note: as of M2, Error is never emitted (the catch blocks still swallow errors
 * silently). M3 will wire actual error emission. The state exists now so
 * SplashScreen can wait for Ready instead of a fixed delay.
 */
sealed class CatalogState {
    data object Loading : CatalogState()
    data object Ready : CatalogState()
    data class Error(val message: String) : CatalogState()
}

class FirebaseRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val COLLECTION_NAME = "media_content"

    private val _mediaCatalog = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaCatalog: StateFlow<List<MediaItem>> = _mediaCatalog.asStateFlow()

    private val _catalogState = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val catalogState: StateFlow<CatalogState> = _catalogState.asStateFlow()

    init {
        loadInitialCatalog()
        attachFirestoreListener()
    }

    private fun attachFirestoreListener() {
        try {
            firestore.collection(COLLECTION_NAME)
                .orderBy("title", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) {
                        // M3 will emit Error state here. For now, mark Ready so
                        // SplashScreen proceeds — empty catalog is better than
                        // an infinite splash screen.
                        if (_catalogState.value is CatalogState.Loading) {
                            _catalogState.value = CatalogState.Ready
                        }
                        return@addSnapshotListener
                    }

                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(MediaItem::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (items.isNotEmpty()) {
                        _mediaCatalog.value = items
                    }
                    _catalogState.value = CatalogState.Ready
                }
        } catch (e: Exception) {
            // Offline or uninitialized fallback — still mark Ready so SplashScreen
            // does not hang forever. M3 will emit proper Error state.
            _catalogState.value = CatalogState.Ready
        }
    }

    fun saveMediaItem(item: MediaItem) {
        // Local state update
        val current = _mediaCatalog.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        _mediaCatalog.value = current

        // Remote Firestore sync
        try {
            firestore.collection(COLLECTION_NAME)
                .document(item.id)
                .set(item)
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    fun deleteMediaItem(itemId: String) {
        val current = _mediaCatalog.value.toMutableList()
        current.removeAll { it.id == itemId }
        _mediaCatalog.value = current

        try {
            firestore.collection(COLLECTION_NAME)
                .document(itemId)
                .delete()
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    private fun loadInitialCatalog() {
        val sampleItems = listOf(
            MediaItem(
                id = "anime_demon_slayer",
                title = "Demon Slayer: Kimetsu no Yaiba",
                type = "SERIES",
                category = "ANIME",
                genres = listOf("Action", "Fantasy", "Supernatural"),
                rating = "4.9",
                releaseYear = "2024",
                maturityRating = "16+",
                posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80",
                bannerUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1200&auto=format&fit=crop&q=80",
                description = "Tanjiro Kamado sets out to become a demon slayer after his family is slaughtered and his sister Nezuko is turned into a demon.",
                isFeatured = true,
                isTrending = true,
                mediaInfo = MediaInfo(
                    resolution = "1080p FHD",
                    videoCodec = "HEVC / x265",
                    bitrate = "3450 kb/s",
                    frameRate = "23.976 FPS",
                    fileSize = "1.4 GB",
                    audioTracks = listOf("Japanese (AAC 5.1)", "Hindi (Stereo)", "English (AAC)"),
                    subtitleTracks = listOf("English (ESub)", "Hindi (Sub)"),
                    qualityBadges = listOf("1080p", "x265", "Multi-Audio", "ESub")
                ),
                episodes = listOf(
                    Episode(
                        episodeNumber = 1,
                        title = "EP 1: Cruelty & Destiny",
                        thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=400",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        mirrorStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        durationMs = 596000L
                    ),
                    Episode(
                        episodeNumber = 2,
                        title = "EP 2: Trainer Sakonji Urokodaki",
                        thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=400",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        durationMs = 653000L
                    )
                )
            ),
            MediaItem(
                id = "movie_cadaver",
                title = "Cadaver 2022",
                type = "MOVIE",
                category = "MOVIE",
                genres = listOf("Thriller", "Crime", "Mystery"),
                rating = "4.7",
                releaseYear = "2022",
                maturityRating = "18+",
                posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80",
                bannerUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1200&auto=format&fit=crop&q=80",
                description = "A high-stakes forensic pathologist gets entangled in a mysterious police investigation involving a criminal mastermind.",
                isFeatured = false,
                isTrending = true,
                mediaInfo = MediaInfo(
                    resolution = "1080p WEB-DL",
                    videoCodec = "AVC / x264",
                    bitrate = "2299 kb/s",
                    frameRate = "24.00 FPS",
                    aspectRatio = "2.35:1",
                    fileSize = "2.3 GB",
                    audioTracks = listOf("Hindi (AAC 5.1)", "Tamil (AAC 5.1)"),
                    subtitleTracks = listOf("English (UTF-8)"),
                    qualityBadges = listOf("1080p", "x264", "Dual Audio", "ESub")
                ),
                episodes = listOf(
                    Episode(
                        episodeNumber = 1,
                        title = "Full Movie - Cadaver (2022)",
                        thumbnailUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        mirrorStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        durationMs = 7140000L
                    )
                )
            )
        )
        _mediaCatalog.value = sampleItems
    }
}
