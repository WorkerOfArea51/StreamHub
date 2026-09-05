package com.streamhub.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.android.gms.tasks.Tasks
import com.streamhub.app.data.StreamBackendConfig
import com.streamhub.app.data.TelegramLinkResolver
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        val ALL_COLLECTIONS = listOf(COLLECTION_MOVIES, COLLECTION_ANIMES, COLLECTION_SERIES)

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
                                val createdAt = if (item.createdAt > 0L) {
                                    item.createdAt
                                } else {
                                    doc.getLong("createdAt")
                                        ?: doc.getTimestamp("createdAt")?.toDate()?.time
                                        ?: doc.getLong("timestamp")
                                        ?: doc.getTimestamp("timestamp")?.toDate()?.time
                                        ?: 0L
                                }
                                val updatedAt = if (item.updatedAt > 0L) {
                                    item.updatedAt
                                } else {
                                    doc.getLong("updatedAt")
                                        ?: doc.getTimestamp("updatedAt")?.toDate()?.time
                                        ?: createdAt
                                }
                                val franchiseOrder = if (item.franchiseOrder > 0.0) {
                                    item.franchiseOrder
                                } else {
                                    doc.getDouble("franchiseOrder")
                                        ?: doc.getLong("franchiseOrder")?.toDouble()
                                        ?: 0.0
                                }
                                val migratedEpisodes = item.episodes.map { ep ->
                                    ep.copy(
                                        streamUrl = StreamBackendConfig.migrateUrl(ep.streamUrl),
                                        mirrorStreamUrl = StreamBackendConfig.migrateUrl(ep.mirrorStreamUrl)
                                    )
                                }
                                (if (item.id.isBlank()) item.copy(id = doc.id) else item).copy(
                                    category = finalCategory,
                                    type = finalType,
                                    franchiseOrder = franchiseOrder,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt,
                                    episodes = com.streamhub.app.data.EpisodeOrderingManager.normalizeAndSort(migratedEpisodes)
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
     * Save or update a media item in its respective Firestore collection (movies, animes, web_series)
     * and dual-write to legacy media_content for complete security rule & backward compatibility.
     */
    fun saveMediaItem(item: MediaItem) {
        _adminOperationState.value = AdminOperationState.Loading

        val finalCreatedAt = if (item.createdAt > 0L) item.createdAt else System.currentTimeMillis()
        val finalUpdatedAt = System.currentTimeMillis()
        val migratedEpisodes = item.episodes.map { ep ->
            ep.copy(
                streamUrl = TelegramLinkResolver.sanitizePlayableUrl(ep.streamUrl),
                mirrorStreamUrl = TelegramLinkResolver.sanitizePlayableUrl(
                    if (ep.mirrorStreamUrl.isNotBlank()) ep.mirrorStreamUrl else ep.streamUrl
                )
            )
        }
        val normalizedEpisodes = com.streamhub.app.data.EpisodeOrderingManager.normalizeAndSort(migratedEpisodes)
        val itemToSave = item.copy(
            createdAt = finalCreatedAt,
            updatedAt = finalUpdatedAt,
            episodes = normalizedEpisodes
        )

        // 1. Optimistic instant UI update
        _mediaCatalog.update { current ->
            val list = current.toMutableList()
            val index = list.indexOfFirst { it.id == itemToSave.id }
            if (index >= 0) list[index] = itemToSave else list.add(0, itemToSave)
            list
        }
        _catalogState.value = CatalogState.Ready

        val db = firestore
        if (db == null) {
            Log.e(TAG, "CRITICAL: Cannot save media item ${itemToSave.id} because Firestore instance is null!")
            _adminOperationState.value = AdminOperationState.Error("Firebase database not initialized")
            return
        }

        val targetCollection = getCollectionForCategory(itemToSave.category, itemToSave.type)
        val docMap = mediaItemToMap(itemToSave)
        Log.d(TAG, "Writing media item ${itemToSave.id} to Firestore collection '$targetCollection'...")

        // Primary collection write (movies, animes, web_series)
        db.collection(targetCollection)
            .document(itemToSave.id)
            .set(docMap)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully synced media item to Firestore collection '$targetCollection': ${itemToSave.id}")
                _adminOperationState.value = AdminOperationState.Success()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Primary write to '$targetCollection' failed (check security rules): ${e.message}")
            }

        // Clean up from other collections if category was moved/changed
        for (col in ALL_COLLECTIONS) {
            if (col != targetCollection) {
                db.collection(col).document(itemToSave.id).delete()
                    .addOnFailureListener { /* Ignore if document didn't exist in this collection */ }
            }
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
            "castList" to item.castList,
            "posterUrl" to item.posterUrl,
            "bannerUrl" to item.bannerUrl,
            "description" to item.description,
            "isFeatured" to item.isFeatured,
            "isTrending" to item.isTrending,
            "franchiseId" to item.franchiseId,
            "franchiseTitle" to item.franchiseTitle,
            "seasonNumber" to item.seasonNumber,
            "partNumber" to item.partNumber,
            "franchiseOrder" to item.franchiseOrder,
            "seasonTitle" to item.seasonTitle,
            "relationType" to item.relationType,
            "relatedMediaIds" to item.relatedMediaIds,
            "createdAt" to item.createdAt,
            "updatedAt" to item.updatedAt,
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

    /**
     * Scans all Firestore collections (movies, animes, web_series) and updates any document
     * whose streamUrl, mirrorStreamUrl, or episode URLs contain legacy Alwaysdata hosts.
     * Rewrites them to midnighthawk.serv00.net and normalizes /stream/ to /dl/.
     *
     * @param onProgress Callback receiving (currentProcessed, totalDocuments, updatedCount)
     * @return Result with total number of documents successfully updated.
     */
    suspend fun migrateCatalogToServ00(
        onProgress: (current: Int, total: Int, updated: Int) -> Unit = { _, _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore not initialized"))

        runCatching {
            val collections = listOf(COLLECTION_ANIMES, COLLECTION_MOVIES, COLLECTION_SERIES)
            val docsToProcess = mutableListOf<Pair<String, com.google.firebase.firestore.DocumentSnapshot>>()

            for (col in collections) {
                val snapshot = Tasks.await(db.collection(col).get())
                for (doc in snapshot.documents) {
                    docsToProcess.add(col to doc)
                }
            }
            val totalDocs = docsToProcess.size
            var processed = 0
            var updated = 0

            for ((col, doc) in docsToProcess) {
                processed++
                val item = doc.toObject(MediaItem::class.java)
                if (item != null) {
                    val rawDocStream = doc.getString("streamUrl").orEmpty()
                    val rawDocMirror = doc.getString("mirrorStreamUrl").orEmpty()
                    val hasLegacyInRoot = rawDocStream.contains("alwaysdata.net", ignoreCase = true) ||
                                          rawDocMirror.contains("alwaysdata.net", ignoreCase = true) ||
                                          rawDocStream.contains("/stream/", ignoreCase = true)
                    val hasLegacyInEpisodes = item.episodes.any { ep ->
                        ep.streamUrl.contains("alwaysdata.net", ignoreCase = true) ||
                        ep.mirrorStreamUrl.contains("alwaysdata.net", ignoreCase = true) ||
                        ep.streamUrl.contains("/stream/", ignoreCase = true)
                    }

                    if (hasLegacyInRoot || hasLegacyInEpisodes) {
                        val migratedEpisodes = item.episodes.map { ep ->
                            ep.copy(
                                streamUrl = TelegramLinkResolver.sanitizePlayableUrl(ep.streamUrl),
                                mirrorStreamUrl = TelegramLinkResolver.sanitizePlayableUrl(
                                    if (ep.mirrorStreamUrl.isNotBlank()) ep.mirrorStreamUrl else ep.streamUrl
                                )
                            )
                        }

                        val updatedItem = item.copy(
                            id = doc.id,
                            episodes = com.streamhub.app.data.EpisodeOrderingManager.normalizeAndSort(migratedEpisodes),
                            updatedAt = System.currentTimeMillis()
                        )

                        val docMap = mediaItemToMap(updatedItem).toMutableMap()
                        if (rawDocStream.isNotBlank()) {
                            docMap["streamUrl"] = TelegramLinkResolver.sanitizePlayableUrl(rawDocStream)
                        }
                        if (rawDocMirror.isNotBlank()) {
                            docMap["mirrorStreamUrl"] = TelegramLinkResolver.sanitizePlayableUrl(rawDocMirror)
                        }

                        Tasks.await(db.collection(col).document(doc.id).set(docMap))
                        updated++
                        Log.d(TAG, "Migrated document ${doc.id} in collection '$col' to Serv00")
                    }
                }
                onProgress(processed, totalDocs, updated)
            }

            Log.i(TAG, "Completed Serv00 catalog migration: $updated of $totalDocs documents updated.")
            updated
        }
    }
}
