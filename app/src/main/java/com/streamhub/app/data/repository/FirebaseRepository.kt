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
        private const val COLLECTION_NAME = "media_content"

        @Volatile
        private var INSTANCE: FirebaseRepository? = null

        fun getInstance(): FirebaseRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseRepository().also { INSTANCE = it }
            }
    }

    private val firestore by lazy {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var listenerRegistration: ListenerRegistration? = null

    private val _mediaCatalog = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaCatalog: StateFlow<List<MediaItem>> = _mediaCatalog.asStateFlow()

    private val _catalogState = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val catalogState: StateFlow<CatalogState> = _catalogState.asStateFlow()

    private val _adminOperationState = MutableStateFlow<AdminOperationState>(AdminOperationState.Idle)
    val adminOperationState: StateFlow<AdminOperationState> = _adminOperationState.asStateFlow()

    init {
        loadInitialCatalog()
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
        removeFirestoreListener()
    }

    private fun attachFirestoreListener() {
        val db = firestore
        if (db == null) {
            Log.w(TAG, "Firestore instance is null — using empty catalog")
            _catalogState.value = CatalogState.Ready
            return
        }

        try {
            listenerRegistration?.remove()
            listenerRegistration = db.collection(COLLECTION_NAME)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Firestore snapshot listener error", error)
                        _catalogState.value = CatalogState.Error(error.message ?: "Firestore error")
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        _catalogState.value = CatalogState.Ready
                        return@addSnapshotListener
                    }

                    scope.launch(Dispatchers.IO) {
                        val remoteItems = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(MediaItem::class.java)?.let { item ->
                                    if (item.id.isBlank()) item.copy(id = doc.id) else item
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse document ${doc.id}", e)
                                null
                            }
                        }

                        if (remoteItems.isNotEmpty()) {
                            _mediaCatalog.update { current -> (current + remoteItems).distinctBy { it.id } }
                        }
                        _catalogState.value = CatalogState.Ready
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach Firestore listener", e)
            _catalogState.value = CatalogState.Error("Failed to attach Firestore listener: ${e.message}")
        }
    }

    private fun removeFirestoreListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    /**
     * Save or update a media item in catalog.
     */
    fun saveMediaItem(item: MediaItem) {
        _adminOperationState.value = AdminOperationState.Loading
        val db = firestore
        if (db == null) {
            _mediaCatalog.update { current ->
                val list = current.toMutableList()
                val index = list.indexOfFirst { it.id == item.id }
                if (index >= 0) list[index] = item else list.add(0, item)
                list
            }
            _catalogState.value = CatalogState.Ready
            _adminOperationState.value = AdminOperationState.Success()
            return
        }

        val docMap = mediaItemToMap(item)
        db.collection(COLLECTION_NAME)
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
                Log.d(TAG, "Successfully synced media item to Firestore: ${item.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync media item to Firestore: ${item.id}", e)
                _adminOperationState.value = AdminOperationState.Error(e.message ?: "Failed to save to Firestore")
            }
    }

    /**
     * Delete a media item from catalog.
     */
    fun deleteMediaItem(itemId: String) {
        _adminOperationState.value = AdminOperationState.Loading
        val db = firestore
        if (db == null) {
            _mediaCatalog.update { current -> current.filterNot { it.id == itemId } }
            _adminOperationState.value = AdminOperationState.Success()
            return
        }
        db.collection(COLLECTION_NAME)
            .document(itemId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Successfully deleted media item from Firestore: $itemId")
                _mediaCatalog.update { current -> current.filterNot { it.id == itemId } }
                _adminOperationState.value = AdminOperationState.Success()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete media item from Firestore: $itemId", e)
                _adminOperationState.value = AdminOperationState.Error(e.message ?: "Delete failed")
            }
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
                    "title" to ep.title,
                    "thumbnailUrl" to ep.thumbnailUrl,
                    "streamUrl" to ep.streamUrl,
                    "mirrorStreamUrl" to ep.mirrorStreamUrl,
                    "telegramFileId" to ep.telegramFileId,
                    "durationMs" to ep.durationMs
                )
            }
        )
    }
}
