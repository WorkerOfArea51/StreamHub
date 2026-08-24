package com.streamhub.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State of the media catalog fetch.
 */
sealed class CatalogState {
    data object Loading : CatalogState()
    data object Ready : CatalogState()
    data class Error(val message: String) : CatalogState()
}

/**
 * State of an admin write operation (save or delete).
 */
sealed class AdminOperationState {
    data object Idle : AdminOperationState()
    data object Loading : AdminOperationState()
    data class Success(val timestamp: Long = System.currentTimeMillis()) : AdminOperationState()
    data class Error(val message: String) : AdminOperationState()
}

class FirebaseRepository private constructor() {

    companion object {
        private const val TAG = "FirebaseRepository"
        const val COLLECTION_MOVIES = "movies"
        const val COLLECTION_ANIMES = "animes"
        const val COLLECTION_SERIES = "web_series"
        const val COLLECTION_LEGACY = "media_content"

        val ALL_COLLECTIONS = listOf(COLLECTION_MOVIES, COLLECTION_ANIMES, COLLECTION_SERIES, COLLECTION_LEGACY)

        fun getCollectionForCategory(category: String, type: String = ""): String {
            val cat = category.trim().lowercase()
            val typ = type.trim().lowercase()
            return when {
                cat.contains("anime") || typ.contains("anime") -> COLLECTION_ANIMES
                cat.contains("series") || cat.contains("tv") || cat.contains("show") || typ.contains("series") || typ.contains("tv") -> COLLECTION_SERIES
                else -> COLLECTION_MOVIES
            }
        }

        @Volatile
        private var INSTANCE: FirebaseRepository? = null

        fun getInstance(): FirebaseRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseRepository().also { INSTANCE = it }
            }
    }

    @Volatile
    private var firestoreCache: FirebaseFirestore? = null
    @Volatile
    private var firestoreResolved = false

    private val firestore: FirebaseFirestore?
        get() {
            if (firestoreResolved) return firestoreCache
            firestoreCache = runCatching {
                val app = com.google.firebase.FirebaseApp.getInstance()
                FirebaseFirestore.getInstance(app)
            }.recoverCatching {
                FirebaseFirestore.getInstance()
            }.onFailure { e ->
                Log.e(TAG, "Failed to get FirebaseFirestore instance", e)
            }.getOrNull()
            firestoreResolved = true
            return firestoreCache
        }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private val collectionMap = java.util.concurrent.ConcurrentHashMap<String, List<MediaItem>>()

    private val _mediaCatalog = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaCatalog: StateFlow<List<MediaItem>> = _mediaCatalog.asStateFlow()

    private val _catalogState = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val catalogState: StateFlow<CatalogState> = _catalogState.asStateFlow()

    private val _adminOperationState = MutableStateFlow<AdminOperationState>(AdminOperationState.Idle)
    val adminOperationState: StateFlow<AdminOperationState> = _adminOperationState.asStateFlow()

    init {
        loadInitialCatalog()
        attachFirestoreListener()
        scope.launch {
            kotlinx.coroutines.delay(30_000L)
            if (_catalogState.value is CatalogState.Loading) {
                _catalogState.value = CatalogState.Error("Catalog connection timeout")
            }
        }
    }

    suspend fun connect() {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            attachFirestoreListener()
        }
    }

    fun retry() {
        Log.d(TAG, "Manual retry requested")
        _catalogState.value = CatalogState.Loading
        attachFirestoreListener()
    }

    fun resetAdminOperationState() {
        _adminOperationState.value = AdminOperationState.Idle
    }

    fun cleanup() {
        removeFirestoreListeners()
    }

    private fun attachFirestoreListener() {
        val db = firestore
        if (db == null) {
            Log.e(TAG, "CRITICAL: Firestore instance is null — FirebaseApp may not be initialized")
            _catalogState.value = CatalogState.Error("Firebase database not initialized")
            return
        }

        try {
            removeFirestoreListeners()
            for (col in ALL_COLLECTIONS) {
                val reg = db.collection(col).addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore listener error on collection $col: ${error.message}")
                        return@addSnapshotListener
                    }

                    val items = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(MediaItem::class.java)?.let { item ->
                                val finalCategory = if (item.category.isNotBlank()) item.category else when (col) {
                                    COLLECTION_ANIMES -> "Anime"
                                    COLLECTION_SERIES -> "Series"
                                    else -> "Movies"
                                }
                                val finalType = if (item.type.isNotBlank()) item.type else when (col) {
                                    COLLECTION_ANIMES -> "Anime"
                                    COLLECTION_SERIES -> "Series"
                                    else -> "Movie"
                                }
                                (if (item.id.isBlank()) item.copy(id = doc.id) else item).copy(
                                    category = finalCategory,
                                    type = finalType
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse document ${doc.id} from $col", e)
                            null
                        }
                    } ?: emptyList()

                    collectionMap[col] = items

                    // Merge all collections together, deduplicating by ID
                    val merged = collectionMap.values.flatten().distinctBy { it.id }
                    _mediaCatalog.value = merged
                    _catalogState.value = CatalogState.Ready
                    Log.d(TAG, "Firestore synced collection '$col' (${items.size} items), total merged = ${merged.size}")
                }
                activeListeners.add(reg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach Firestore listeners", e)
            _catalogState.value = CatalogState.Error("Failed to attach Firestore listener: ${e.message}")
        }
    }

    private fun removeFirestoreListeners() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
    }

    /**
     * Save or update a media item in its respective Firestore collection (movies, animes, web_series).
     */
    fun saveMediaItem(item: MediaItem) {
        _adminOperationState.value = AdminOperationState.Loading
        val db = firestore
        if (db == null) {
            Log.e(TAG, "CRITICAL: Cannot save media item ${item.id} because Firestore instance is null!")
            _adminOperationState.value = AdminOperationState.Error("Firebase database not initialized")
            return
        }

        val targetCollection = getCollectionForCategory(item.category, item.type)
        val docMap = mediaItemToMap(item)
        Log.d(TAG, "Writing media item ${item.id} to Firestore collection '$targetCollection'...")
        db.collection(targetCollection)
            .document(item.id)
            .set(docMap)
            .addOnSuccessListener {
                _mediaCatalog.update { current ->
                    val list = current.toMutableList()
                    val index = list.indexOfFirst { it.id == item.id }
                    if (index >= 0) list[index] = item else list.add(0, item)
                    list
                }
                _catalogState.value = CatalogState.Ready
                _adminOperationState.value = AdminOperationState.Success()
                Log.d(TAG, "Successfully synced media item to Firestore collection '$targetCollection': ${item.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync media item to Firestore collection '$targetCollection': ${item.id}", e)
                _adminOperationState.value = AdminOperationState.Error(e.message ?: "Failed to save to Firestore")
            }
    }

    /**
     * Delete a media item from all collections.
     */
    fun deleteMediaItem(itemId: String) {
        _adminOperationState.value = AdminOperationState.Loading
        val db = firestore
        if (db == null) {
            _mediaCatalog.update { current -> current.filterNot { it.id == itemId } }
            _adminOperationState.value = AdminOperationState.Success()
            return
        }

        for (col in ALL_COLLECTIONS) {
            db.collection(col).document(itemId).delete()
        }

        _mediaCatalog.update { current -> current.filterNot { it.id == itemId } }
        _adminOperationState.value = AdminOperationState.Success()
        Log.d(TAG, "Successfully deleted media item $itemId across all collections")
    }

    private fun loadInitialCatalog() {
        _mediaCatalog.value = emptyList()
        _catalogState.value = CatalogState.Loading
    }

    private fun mediaItemToMap(item: MediaItem): Map<String, Any?> {
        return mapOf(
            "id" to item.id,
            "title" to item.title,
            "type" to item.type,
            "category" to item.category,
            "genres" to item.genres,
            "rating" to item.rating,
            "releaseYear" to item.releaseYear,
            "maturityRating" to item.maturityRating,
            "studio" to item.studio,
            "trailerId" to item.trailerId,
            "malId" to item.malId,
            "tmdbId" to item.tmdbId,
            "synonyms" to item.synonyms,
            "totalEpisodes" to item.totalEpisodes,
            "status" to item.status,
            "aired" to item.aired,
            "premiered" to item.premiered,
            "producers" to item.producers,
            "source" to item.source,
            "duration" to item.duration,
            "budgetBoxOffice" to item.budgetBoxOffice,
            "castList" to item.castList,
            "posterUrl" to item.posterUrl,
            "bannerUrl" to item.bannerUrl,
            "description" to item.description,
            "isFeatured" to item.isFeatured,
            "isTrending" to item.isTrending,
            "franchiseId" to item.franchiseId,
            "franchiseTitle" to item.franchiseTitle,
            "seasonNumber" to item.seasonNumber,
            "seasonTitle" to item.seasonTitle,
            "relationType" to item.relationType,
            "relatedMediaIds" to item.relatedMediaIds,
            "mediaInfo" to mapOf(
                "resolution" to item.mediaInfo.resolution,
                "videoCodec" to item.mediaInfo.videoCodec,
                "bitrate" to item.mediaInfo.bitrate,
                "frameRate" to item.mediaInfo.frameRate,
                "aspectRatio" to item.mediaInfo.aspectRatio,
                "fileSize" to item.mediaInfo.fileSize,
                "audioTracks" to item.mediaInfo.audioTracks,
                "subtitleTracks" to item.mediaInfo.subtitleTracks,
                "qualityBadges" to item.mediaInfo.qualityBadges
            ),
            "episodes" to item.episodes.map { ep ->
                mapOf(
                    "episodeNumber" to ep.episodeNumber,
                    "seasonNumber" to ep.seasonNumber,
                    "arcName" to ep.arcName,
                    "title" to ep.title,
                    "thumbnailUrl" to ep.thumbnailUrl,
                    "streamUrl" to ep.streamUrl,
                    "mirrorStreamUrl" to ep.mirrorStreamUrl,
                    "telegramFileId" to ep.telegramFileId,
                    "durationMs" to ep.durationMs,
                    "fileName" to ep.fileName,
                    "fileSize" to ep.fileSize
                )
            }
        )
    }
}
