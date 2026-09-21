package com.streamhub.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.android.gms.tasks.Tasks
import com.streamhub.app.data.StreamBackendConfig
import com.streamhub.app.data.TelegramLinkResolver
import com.streamhub.app.data.models.MediaItem
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
        const val COLLECTION_BUNDLES = com.streamhub.app.data.importer.CatalogBundleManager.COLLECTION_BUNDLES

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

        fun mediaItemToMap(item: MediaItem): Map<String, Any?> {
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

        @Volatile
        private var instance: FirebaseRepository? = null

        fun getInstance(): FirebaseRepository {
            return instance ?: synchronized(this) {
                instance ?: FirebaseRepository().also { instance = it }
            }
        }
    }

    @Volatile
    private var firestoreCache: FirebaseFirestore? = null
    @Volatile
    private var firestoreResolved = false

    val firestore: FirebaseFirestore?
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
    private val rawCollectionListeners = mutableListOf<ListenerRegistration>()
    private var bundleListener: ListenerRegistration? = null
    private val collectionMap = java.util.concurrent.ConcurrentHashMap<String, List<MediaItem>>()

    private val _isUsingBundles = MutableStateFlow(false)
    val isUsingBundles: StateFlow<Boolean> = _isUsingBundles.asStateFlow()

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

    fun refreshCatalog() = retry()

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

            // 1. Primary path: Listen to catalog_bundles for ultra-fast, minimal-read sync (~6 reads)
            bundleListener?.remove()
            bundleListener = db.collection(COLLECTION_BUNDLES).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "catalog_bundles listener error: ${error.message}. Falling back to raw collections.")
                    if (!_isUsingBundles.value && rawCollectionListeners.isEmpty()) {
                        attachRawCollectionListeners(db)
                    }
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d(TAG, "No bundles found in '$COLLECTION_BUNDLES'. Falling back to raw collections.")
                    if (!_isUsingBundles.value && rawCollectionListeners.isEmpty()) {
                        attachRawCollectionListeners(db)
                    }
                    return@addSnapshotListener
                }

                try {
                    val allBundleItems = mutableListOf<MediaItem>()
                    for (doc in snapshot.documents) {
                        val bundle = doc.toObject(com.streamhub.app.data.models.CatalogBundle::class.java)
                        if (bundle != null && bundle.items.isNotEmpty()) {
                            allBundleItems.addAll(bundle.items)
                        } else {
                            // Fallback manual parsing if reflection misses dynamic nested fields
                            val rawItems = doc.get("items") as? List<Map<String, Any?>>
                            rawItems?.mapNotNull { parseMediaItemMap(it, doc.id) }?.let {
                                allBundleItems.addAll(it)
                            }
                        }
                    }

                    if (allBundleItems.isNotEmpty()) {
                        _isUsingBundles.value = true
                        removeRawCollectionListeners()

                        val normalized = allBundleItems.map { item ->
                            val migratedEpisodes = item.episodes.map { ep ->
                                ep.copy(
                                    streamUrl = StreamBackendConfig.migrateUrl(ep.streamUrl),
                                    mirrorStreamUrl = StreamBackendConfig.migrateUrl(ep.mirrorStreamUrl)
                                )
                            }
                            item.copy(
                                episodes = com.streamhub.app.data.EpisodeOrderingManager.normalizeAndSort(migratedEpisodes)
                            )
                        }.distinctBy { it.id }

                        _mediaCatalog.value = normalized
                        _catalogState.value = CatalogState.Ready
                        Log.d(TAG, "✅ Synced catalog via Bundles! ${snapshot.size()} bundle docs read, total shows = ${normalized.size}")

                        triggerNotificationAlerts(normalized)
                    } else {
                        if (!_isUsingBundles.value && rawCollectionListeners.isEmpty()) {
                            attachRawCollectionListeners(db)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing catalog bundles", e)
                    if (!_isUsingBundles.value && rawCollectionListeners.isEmpty()) {
                        attachRawCollectionListeners(db)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach Firestore listeners", e)
            _catalogState.value = CatalogState.Error("Failed to attach Firestore listener: ${e.message}")
        }
    }

    private fun attachRawCollectionListeners(db: FirebaseFirestore) {
        if (rawCollectionListeners.isNotEmpty()) return
        Log.d(TAG, "Attaching raw collection listeners for ${ALL_COLLECTIONS.joinToString()}")
        try {
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
                    Log.d(TAG, "Firestore synced raw collection '$col' (${items.size} items), total merged = ${merged.size}")

                    triggerNotificationAlerts(merged)
                }
                rawCollectionListeners.add(reg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach raw collection listeners", e)
        }
    }

    private fun removeRawCollectionListeners() {
        rawCollectionListeners.forEach { it.remove() }
        rawCollectionListeners.clear()
        collectionMap.clear()
    }

    private fun removeFirestoreListeners() {
        bundleListener?.remove()
        bundleListener = null
        removeRawCollectionListeners()
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
    }

    private fun triggerNotificationAlerts(items: List<MediaItem>) {
        if (items.isEmpty()) return
        scope.launch {
            runCatching {
                val context = com.streamhub.app.StreamHubApplication.getInstance()
                val myListIds = com.streamhub.app.data.MyListManager.myListFlow.value
                if (myListIds.isNotEmpty()) {
                    com.streamhub.app.data.NotificationAlertManager.checkAndNotifyUpdates(context, items, myListIds)
                }
            }
        }
    }

    private fun parseMediaItemMap(map: Map<String, Any?>, fallbackId: String): MediaItem? {
        return try {
            val id = (map["id"] as? String)?.takeIf { it.isNotBlank() } ?: fallbackId
            val title = map["title"] as? String ?: ""
            val type = map["type"] as? String ?: "MOVIE"
            val category = map["category"] as? String ?: "MOVIE"
            val genres = (map["genres"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val rating = map["rating"] as? String ?: ""
            val releaseYear = map["releaseYear"] as? String ?: ""
            val maturityRating = map["maturityRating"] as? String ?: ""
            val studio = map["studio"] as? String ?: ""
            val trailerId = map["trailerId"] as? String ?: ""
            val malId = map["malId"] as? String ?: ""
            val tmdbId = map["tmdbId"] as? String ?: ""
            val synonyms = map["synonyms"] as? String ?: ""
            val totalEpisodes = map["totalEpisodes"] as? String ?: ""
            val status = map["status"] as? String ?: ""
            val aired = map["aired"] as? String ?: ""
            val premiered = map["premiered"] as? String ?: ""
            val producers = map["producers"] as? String ?: ""
            val source = map["source"] as? String ?: ""
            val duration = map["duration"] as? String ?: ""
            val castList = (map["castList"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val posterUrl = map["posterUrl"] as? String ?: ""
            val bannerUrl = map["bannerUrl"] as? String ?: ""
            val description = map["description"] as? String ?: ""
            val isFeatured = map["isFeatured"] as? Boolean ?: false
            val isTrending = map["isTrending"] as? Boolean ?: false
            val franchiseId = map["franchiseId"] as? String ?: ""
            val franchiseTitle = map["franchiseTitle"] as? String ?: ""
            val seasonNumber = (map["seasonNumber"] as? Number)?.toInt() ?: 1
            val partNumber = (map["partNumber"] as? Number)?.toInt() ?: 0
            val franchiseOrder = (map["franchiseOrder"] as? Number)?.toDouble() ?: 0.0
            val seasonTitle = map["seasonTitle"] as? String ?: ""
            val relationType = map["relationType"] as? String ?: ""
            val relatedMediaIds = (map["relatedMediaIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
            val updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L

            val rawInfo = map["mediaInfo"] as? Map<String, Any?>
            val mediaInfo = if (rawInfo != null) {
                com.streamhub.app.data.models.MediaInfo(
                    resolution = rawInfo["resolution"] as? String ?: "",
                    videoCodec = rawInfo["videoCodec"] as? String ?: "",
                    bitrate = rawInfo["bitrate"] as? String ?: "",
                    frameRate = rawInfo["frameRate"] as? String ?: "",
                    aspectRatio = rawInfo["aspectRatio"] as? String ?: "",
                    fileSize = rawInfo["fileSize"] as? String ?: "",
                    audioTracks = (rawInfo["audioTracks"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    subtitleTracks = (rawInfo["subtitleTracks"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    qualityBadges = (rawInfo["qualityBadges"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                )
            } else com.streamhub.app.data.models.MediaInfo()

            val rawEpisodes = map["episodes"] as? List<Map<String, Any?>>
            val episodes = rawEpisodes?.mapIndexed { index, epMap ->
                com.streamhub.app.data.models.Episode(
                    episodeNumber = (epMap["episodeNumber"] as? Number)?.toInt() ?: (index + 1),
                    seasonNumber = (epMap["seasonNumber"] as? Number)?.toInt() ?: 1,
                    arcName = epMap["arcName"] as? String ?: "",
                    title = epMap["title"] as? String ?: "Episode ${index + 1}",
                    thumbnailUrl = epMap["thumbnailUrl"] as? String ?: "",
                    streamUrl = epMap["streamUrl"] as? String ?: "",
                    mirrorStreamUrl = epMap["mirrorStreamUrl"] as? String ?: "",
                    telegramFileId = epMap["telegramFileId"] as? String ?: "",
                    durationMs = (epMap["durationMs"] as? Number)?.toLong() ?: 0L,
                    fileName = epMap["fileName"] as? String ?: "",
                    fileSize = epMap["fileSize"] as? String ?: ""
                )
            } ?: emptyList()

            MediaItem(
                id = id,
                title = title,
                type = type,
                category = category,
                genres = genres,
                rating = rating,
                releaseYear = releaseYear,
                maturityRating = maturityRating,
                studio = studio,
                trailerId = trailerId,
                malId = malId,
                tmdbId = tmdbId,
                synonyms = synonyms,
                totalEpisodes = totalEpisodes,
                status = status,
                aired = aired,
                premiered = premiered,
                producers = producers,
                source = source,
                duration = duration,
                castList = castList,
                posterUrl = posterUrl,
                bannerUrl = bannerUrl,
                description = description,
                isFeatured = isFeatured,
                isTrending = isTrending,
                franchiseId = franchiseId,
                franchiseTitle = franchiseTitle,
                seasonNumber = seasonNumber,
                partNumber = partNumber,
                franchiseOrder = franchiseOrder,
                seasonTitle = seasonTitle,
                relationType = relationType,
                relatedMediaIds = relatedMediaIds,
                createdAt = createdAt,
                updatedAt = updatedAt,
                mediaInfo = mediaInfo,
                episodes = episodes
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed manual parse for MediaItem $fallbackId", e)
            null
        }
    }

    /**
     * Suspending version of saveMediaItem that suspends until the Firestore atomic batch write completes or fails.
     * Returns Result.success(Unit) or Result.failure(Exception).
     */
    suspend fun saveMediaItemSuspending(item: MediaItem): Result<Unit> = suspendCancellableCoroutine { cont ->
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
            if (cont.isActive) cont.resume(Result.failure(IllegalStateException("Firebase database not initialized")))
            return@suspendCancellableCoroutine
        }

        val targetCollection = getCollectionForCategory(itemToSave.category, itemToSave.type)
        val docMap = mediaItemToMap(itemToSave)
        Log.d(TAG, "Writing media item ${itemToSave.id} to Firestore collection '$targetCollection'...")

        val batch = db.batch()
        val targetRef = db.collection(targetCollection).document(itemToSave.id)
        batch.set(targetRef, docMap)

        // Clean up from other collections if category was moved/changed
        for (col in ALL_COLLECTIONS) {
            if (col != targetCollection) {
                batch.delete(db.collection(col).document(itemToSave.id))
            }
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Successfully synced media item to Firestore collection '$targetCollection': ${itemToSave.id}")
                _adminOperationState.value = AdminOperationState.Success()
                if (cont.isActive) cont.resume(Result.success(Unit))

                // Background sync: update the affected category bundle in catalog_bundles
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        com.streamhub.app.data.importer.CatalogBundleManager.syncCategoryBundles(
                            db = db,
                            category = itemToSave.category,
                            type = itemToSave.type,
                            fullCatalog = _mediaCatalog.value
                        )
                    }.onFailure { Log.w(TAG, "Non-fatal bundle sync error after save: ${it.message}") }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Batch write to '$targetCollection' failed: ${e.message}")
                _adminOperationState.value = AdminOperationState.Error(e.message ?: "Write failed")
                if (cont.isActive) cont.resume(Result.failure(e))
            }
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

        val batch = db.batch()
        val targetRef = db.collection(targetCollection).document(itemToSave.id)
        batch.set(targetRef, docMap)

        // Clean up from other collections if category was moved/changed
        for (col in ALL_COLLECTIONS) {
            if (col != targetCollection) {
                batch.delete(db.collection(col).document(itemToSave.id))
            }
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Successfully synced media item to Firestore collection '$targetCollection': ${itemToSave.id}")
                _adminOperationState.value = AdminOperationState.Success()

                // Background sync: update the affected category bundle in catalog_bundles
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        com.streamhub.app.data.importer.CatalogBundleManager.syncCategoryBundles(
                            db = db,
                            category = itemToSave.category,
                            type = itemToSave.type,
                            fullCatalog = _mediaCatalog.value
                        )
                    }.onFailure { Log.w(TAG, "Non-fatal bundle sync error after save: ${it.message}") }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Batch write to '$targetCollection' failed: ${e.message}")
                _adminOperationState.value = AdminOperationState.Error(e.message ?: "Write failed")
            }
    }

    /**
     * Delete a media item from all collections using an atomic batch.
     */
    fun deleteMediaItem(itemId: String) {
        _adminOperationState.value = AdminOperationState.Loading
        val targetItem = _mediaCatalog.value.firstOrNull { it.id == itemId }
        val category = targetItem?.category ?: ""
        val type = targetItem?.type ?: ""

        val db = firestore
        if (db == null) {
            _mediaCatalog.update { current -> current.filterNot { it.id == itemId } }
            _adminOperationState.value = AdminOperationState.Success()
            return
        }

        val batch = db.batch()
        for (col in ALL_COLLECTIONS) {
            batch.delete(db.collection(col).document(itemId))
        }

        batch.commit()
            .addOnSuccessListener {
                _mediaCatalog.update { current -> current.filterNot { it.id == itemId } }
                _adminOperationState.value = AdminOperationState.Success()
                Log.d(TAG, "Successfully deleted media item $itemId across all collections")

                // Background sync: update the affected category bundle in catalog_bundles
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        com.streamhub.app.data.importer.CatalogBundleManager.syncCategoryBundles(
                            db = db,
                            category = category,
                            type = type,
                            fullCatalog = _mediaCatalog.value
                        )
                    }.onFailure { Log.w(TAG, "Non-fatal bundle sync error after delete: ${it.message}") }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Batch delete for $itemId failed: ${e.message}")
                _adminOperationState.value = AdminOperationState.Error(e.message ?: "Delete failed")
            }
    }

    private fun loadInitialCatalog() {
        _mediaCatalog.value = emptyList()
        _catalogState.value = CatalogState.Loading
    }

    /**
     * Scans all Firestore collections (movies, animes, web_series) and updates any document
     * whose streamUrl, mirrorStreamUrl, or episode URLs contain legacy Alwaysdata hosts.
     * Rewrites them to midnighthawk.serv00.net and normalizes /stream/ to /dl/.
     *
     * @param onProgress Callback receiving (currentProcessed, totalDocuments, updatedCount)
     * @return Result with total number of documents successfully updated.
     */
    /**
     * Scans all Firestore documents across animes, movies, and web_series.
     * Rewrites URLs matching sourceHost (and optionally legacy Alwaysdata) to targetHost,
     * and normalizes /stream/ to /dl/.
     *
     * @param sourceHost The old VPS host domain (e.g., midnighthawk.serv00.net)
     * @param targetHost The new VPS host domain (e.g., stream.myvps.com)
     * @param includeAlwaysdata Also rewrite any legacy Alwaysdata URLs found
     * @param onProgress Callback receiving (currentProcessed, totalDocuments, updatedCount)
     * @return Result with total number of documents successfully updated.
     */
    suspend fun migrateCatalogServer(
        sourceHost: String,
        targetHost: String,
        includeAlwaysdata: Boolean = true,
        onProgress: (current: Int, total: Int, updated: Int) -> Unit = { _, _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore not initialized"))

        val cleanSource = sourceHost.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val cleanTarget = targetHost.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')

        if (cleanTarget.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Target host cannot be empty"))
        }

        try {
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

            fun needsMigration(url: String): Boolean {
                if (url.isBlank()) return false
                val matchesAlwaysdata = includeAlwaysdata && url.contains("alwaysdata.net", ignoreCase = true)
                val matchesSource = cleanSource.isNotBlank() && url.contains(cleanSource, ignoreCase = true) && !cleanSource.equals(cleanTarget, ignoreCase = true)
                val hasStreamRoute = url.contains("/stream/", ignoreCase = true)
                return matchesAlwaysdata || matchesSource || hasStreamRoute
            }

            fun rewrite(url: String): String {
                return com.streamhub.app.data.StreamBackendConfig.rewriteServerUrl(
                    url = url,
                    sourceHost = cleanSource,
                    targetHost = cleanTarget,
                    includeAlwaysdata = includeAlwaysdata,
                    normalizeRoute = true
                )
            }

            for ((col, doc) in docsToProcess) {
                processed++
                val item = doc.toObject(MediaItem::class.java)
                if (item != null) {
                    val rawDocStream = doc.getString("streamUrl").orEmpty()
                    val rawDocMirror = doc.getString("mirrorStreamUrl").orEmpty()

                    val hasLegacyInRoot = needsMigration(rawDocStream) || needsMigration(rawDocMirror)
                    val hasLegacyInEpisodes = item.episodes.any { ep ->
                        needsMigration(ep.streamUrl) || needsMigration(ep.mirrorStreamUrl)
                    }

                    if (hasLegacyInRoot || hasLegacyInEpisodes) {
                        val migratedEpisodes = item.episodes.map { ep ->
                            ep.copy(
                                streamUrl = rewrite(ep.streamUrl),
                                mirrorStreamUrl = rewrite(if (ep.mirrorStreamUrl.isNotBlank()) ep.mirrorStreamUrl else ep.streamUrl)
                            )
                        }

                        val updatedItem = item.copy(
                            id = doc.id,
                            episodes = com.streamhub.app.data.EpisodeOrderingManager.normalizeAndSort(migratedEpisodes),
                            updatedAt = System.currentTimeMillis()
                        )

                        val docMap = mediaItemToMap(updatedItem).toMutableMap()
                        if (rawDocStream.isNotBlank()) {
                            docMap["streamUrl"] = rewrite(rawDocStream)
                        }
                        if (rawDocMirror.isNotBlank()) {
                            docMap["mirrorStreamUrl"] = rewrite(rawDocMirror)
                        }

                        Tasks.await(db.collection(col).document(doc.id).set(docMap))
                        updated++
                        Log.d(TAG, "Migrated document ${doc.id} in collection '$col' to $cleanTarget")
                    }
                }
                onProgress(processed, totalDocs, updated)
            }

            Log.i(TAG, "Completed catalog migration to $cleanTarget: $updated of $totalDocs documents updated.")
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Catalog migration failed", e)
            Result.failure(e)
        }
    }

    /**
     * Legacy helper retained for backward compatibility.
     */
    suspend fun migrateCatalogToServ00(
        onProgress: (current: Int, total: Int, updated: Int) -> Unit = { _, _, _ -> }
    ): Result<Int> = migrateCatalogServer(
        sourceHost = com.streamhub.app.data.StreamBackendConfig.LEGACY_STREAMING_HOST,
        targetHost = com.streamhub.app.data.StreamBackendConfig.DEFAULT_STREAMING_HOST,
        includeAlwaysdata = true,
        onProgress = onProgress
    )
}
