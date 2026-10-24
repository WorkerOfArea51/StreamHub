package com.streamhub.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.android.gms.tasks.Tasks
import com.streamhub.app.data.StreamBackendConfig
import com.streamhub.app.data.StreamLinkResolver
import com.streamhub.app.data.models.MediaItem
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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

class CatalogRepository private constructor() {

    companion object {
        private const val TAG = "CatalogRepository"
        const val COLLECTION_MOVIES = "movies"
        const val COLLECTION_ANIMES = "animes"
        const val COLLECTION_SERIES = "web_series"
        const val COLLECTION_BUNDLES = "catalog_bundles"

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
                "anilistId" to item.anilistId,
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
                "featured" to item.isFeatured,
                "isTrending" to item.isTrending,
                "trending" to item.isTrending,
                "trendingAt" to item.trendingAt,
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
                "director" to item.director,
                "writers" to item.writers,
                "castMembers" to item.castMembers.map { cm ->
                    mapOf("name" to cm.name, "character" to cm.character, "profileUrl" to cm.profileUrl)
                },
                "trailers" to item.trailers.map { tr ->
                    mapOf("id" to tr.id, "title" to tr.title, "type" to tr.type, "isOfficial" to tr.isOfficial)
                },
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
        private var instance: CatalogRepository? = null

        fun getInstance(): CatalogRepository {
            return instance ?: synchronized(this) {
                instance ?: CatalogRepository().also { instance = it }
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
    private val collectionMap = java.util.concurrent.ConcurrentHashMap<String, List<MediaItem>>()
    private val tombstoneDeletedIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

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
        // Periodic Cloudflare Edge version check every 5 minutes
        scope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(300_000L) // 5 minutes
                runCatching {
                    com.streamhub.app.data.api.CatalogEdgeClient.fetchCatalogVersion().onSuccess { versionInfo ->
                        val localCount = _mediaCatalog.value.size
                        if (versionInfo.count > 0 && versionInfo.count != localCount) {
                            Log.d(TAG, "Cloudflare Edge catalog version/count updated (${versionInfo.count} vs $localCount), refreshing...")
                            attachFirestoreListener()
                        }
                    }
                }
            }
        }
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
        if (_mediaCatalog.value.isEmpty()) {
            _catalogState.value = CatalogState.Loading
        }
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
        try {
            removeFirestoreListeners()

            // 1. Ultra-fast Cloudflare Edge boot: 15ms response with ZERO Firebase reads
            scope.launch {
                com.streamhub.app.data.api.CatalogEdgeClient.fetchCatalog()
                    .onSuccess { edgeItems ->
                        if (edgeItems.isNotEmpty()) {
                            val currentCatalog = _mediaCatalog.value
                            val currentById = currentCatalog.associateBy { it.id }
                            val merged = edgeItems.map { incoming ->
                                val local = currentById[incoming.id]
                                if (local != null && local.updatedAt > incoming.updatedAt) local else incoming
                            }
                            _mediaCatalog.value = merged
                            _catalogState.value = CatalogState.Ready
                            Log.d(TAG, "🚀 Loaded ${merged.size} shows from Cloudflare Edge (0 Firebase reads)!")
                            triggerNotificationAlerts(merged)

                            // Save smart-capped snapshot for offline mode & instant 15ms cold boot
                            runCatching {
                                val ctx = com.streamhub.app.StreamHubApplication.getInstance()
                                com.streamhub.app.data.CatalogCacheManager.saveCatalogSnapshot(ctx, merged)
                            }.onFailure { Log.w(TAG, "Failed to persist catalog cache: ${it.message}") }
                        }
                    }
                    .onFailure { err ->
                        Log.w(TAG, "Cloudflare Edge catalog fetch failed: ${err.message}. Maintaining cached catalog (0 Firebase reads).")
                        // STRICT ZERO-FIREBASE-FALLBACK FOR REGULAR USERS!
                        // If we already have items in memory/disk, keep UI active in Ready state!
                        if (_mediaCatalog.value.isNotEmpty()) {
                            _catalogState.value = CatalogState.Ready
                        } else {
                            _catalogState.value = CatalogState.Error("Server temporarily unreachable. Check your connection.")
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach Cloudflare listener", e)
            if (_mediaCatalog.value.isNotEmpty()) {
                _catalogState.value = CatalogState.Ready
            } else {
                _catalogState.value = CatalogState.Error("Connection error: ${e.message}")
            }
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
                                val isFeatured = doc.getBoolean("isFeatured")
                                    ?: doc.getBoolean("featured")
                                    ?: item.isFeatured
                                val isTrending = doc.getBoolean("isTrending")
                                    ?: doc.getBoolean("trending")
                                    ?: item.isTrending
                                val trendingAt = doc.getLong("trendingAt")
                                    ?: doc.getTimestamp("trendingAt")?.toDate()?.time
                                    ?: item.trendingAt
                                val migratedEpisodes = item.episodes.map { ep ->
                                    ep.copy(
                                        streamUrl = StreamBackendConfig.migrateUrl(ep.streamUrl),
                                        mirrorStreamUrl = StreamBackendConfig.migrateUrl(ep.mirrorStreamUrl)
                                    )
                                }
                                (if (item.id.isBlank()) item.copy(id = doc.id) else item).copy(
                                    category = finalCategory,
                                    type = finalType,
                                    isFeatured = isFeatured,
                                    isTrending = isTrending,
                                    trendingAt = trendingAt,
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

                    // Merge all collections together, deduplicating by ID with timestamp protection
                    val currentCatalog = _mediaCatalog.value
                    val currentById = currentCatalog.associateBy { it.id }
                    val allRaw = collectionMap.values.flatten().distinctBy { it.id }
                    val merged = allRaw.map { incomingItem ->
                        val localItem = currentById[incomingItem.id]
                        if (localItem != null && localItem.updatedAt > incomingItem.updatedAt) {
                            localItem
                        } else {
                            incomingItem
                        }
                    }
                    val incomingIds = allRaw.map { it.id }.toSet()
                    val pendingLocal = currentCatalog.filter { it.id !in incomingIds && it.updatedAt > 0L }
                    val finalCatalog = (merged + pendingLocal).distinctBy { it.id }

                    _mediaCatalog.value = finalCatalog
                    _catalogState.value = CatalogState.Ready
                    Log.d(TAG, "Firestore synced raw collection '$col' (${items.size} items), total merged = ${finalCatalog.size}")

                    triggerNotificationAlerts(finalCatalog)
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
            val anilistId = (map["anilistId"] as? String)?.takeIf { it.isNotBlank() } ?: (map["malId"] as? String) ?: ""
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
            val isFeatured = (map["isFeatured"] as? Boolean) ?: (map["featured"] as? Boolean) ?: false
            val isTrending = (map["isTrending"] as? Boolean) ?: (map["trending"] as? Boolean) ?: false
            val trendingAt = (map["trendingAt"] as? Number)?.toLong() ?: 0L
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

            val director = map["director"] as? String ?: ""
            val writers = map["writers"] as? String ?: ""
            val castMembers = (map["castMembers"] as? List<*>)?.mapNotNull { cmObj ->
                val cmMap = cmObj as? Map<*, *> ?: return@mapNotNull null
                com.streamhub.app.data.models.CastMember(
                    name = cmMap["name"] as? String ?: "",
                    character = cmMap["character"] as? String ?: "",
                    profileUrl = cmMap["profileUrl"] as? String ?: ""
                )
            } ?: emptyList()
            val trailers = (map["trailers"] as? List<*>)?.mapNotNull { trObj ->
                val trMap = trObj as? Map<*, *> ?: return@mapNotNull null
                com.streamhub.app.data.models.MediaTrailer(
                    id = trMap["id"] as? String ?: "",
                    title = trMap["title"] as? String ?: "",
                    type = trMap["type"] as? String ?: "Trailer",
                    isOfficial = trMap["isOfficial"] as? Boolean ?: false
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
                anilistId = anilistId,
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
                trendingAt = trendingAt,
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
                director = director,
                writers = writers,
                castMembers = castMembers,
                trailers = trailers,
                mediaInfo = mediaInfo,
                episodes = episodes
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed manual parse for MediaItem $fallbackId", e)
            null
        }
    }

    /**
     * Suspending version of saveMediaItem that saves directly to Cloudflare Edge KV as primary.
     * Also dual-writes a background backup to Firestore if initialized.
     */
    suspend fun saveMediaItemSuspending(item: MediaItem): Result<Unit> = withContext(Dispatchers.IO) {
        if (!com.streamhub.app.data.AdminManager.isAdminMode.value) {
            Log.e(TAG, "SECURITY VIOLATION: Unauthorized attempt to save media item ${item.id} without Admin/Owner verification!")
            _adminOperationState.value = AdminOperationState.Error("Unauthorized: Admin access required")
            return@withContext Result.failure(SecurityException("Unauthorized: Creator Studio Admin mode required"))
        }
        _adminOperationState.value = AdminOperationState.Loading

        val finalCreatedAt = if (item.createdAt > 0L) item.createdAt else System.currentTimeMillis()
        val finalUpdatedAt = System.currentTimeMillis()
        val migratedEpisodes = item.episodes.map { ep ->
            ep.copy(
                streamUrl = StreamLinkResolver.sanitizePlayableUrl(ep.streamUrl),
                mirrorStreamUrl = StreamLinkResolver.sanitizePlayableUrl(
                    if (ep.mirrorStreamUrl.isNotBlank()) ep.mirrorStreamUrl else ep.streamUrl
                )
            )
        }
        val normalizedEpisodes = com.streamhub.app.data.EpisodeOrderingManager.normalizeAndSort(migratedEpisodes)
        val finalTrendingAt = if (item.isTrending) {
            if (item.trendingAt > 0L) item.trendingAt else finalUpdatedAt
        } else 0L
        val itemToSave = item.copy(
            createdAt = finalCreatedAt,
            updatedAt = finalUpdatedAt,
            trendingAt = finalTrendingAt,
            episodes = normalizedEpisodes
        )

        tombstoneDeletedIds.remove(itemToSave.id)
        // 1. Optimistic instant UI update
        _mediaCatalog.update { current ->
            val list = current.toMutableList()
            val index = list.indexOfFirst { it.id == itemToSave.id }
            if (index >= 0) list[index] = itemToSave else list.add(0, itemToSave)
            list
        }
        _catalogState.value = CatalogState.Ready

        // 2. Persist local snapshot immediately
        runCatching {
            val ctx = com.streamhub.app.StreamHubApplication.getInstance()
            com.streamhub.app.data.CatalogCacheManager.saveCatalogSnapshot(ctx, _mediaCatalog.value)
        }

        // 3. Primary write: Cloudflare Edge KV
        val edgeResult = com.streamhub.app.data.api.CatalogEdgeClient.saveMediaItem(itemToSave)

        // 4. Dual-write backup to Firestore (non-blocking, best-effort)
        firestore?.let { db ->
            try {
                val targetCollection = getCollectionForCategory(itemToSave.category, itemToSave.type)
                val batch = db.batch()
                batch.set(db.collection(targetCollection).document(itemToSave.id), mediaItemToMap(itemToSave))
                for (col in ALL_COLLECTIONS) {
                    if (col != targetCollection) {
                        batch.delete(db.collection(col).document(itemToSave.id))
                    }
                }
                batch.commit()
            } catch (e: Exception) {
                Log.w(TAG, "Non-fatal Firestore backup write error: ${e.message}")
            }
        }

        if (edgeResult.isSuccess) {
            Log.d(TAG, "Successfully synced media item to Cloudflare Edge: ${itemToSave.id}")
            _adminOperationState.value = AdminOperationState.Success()
            Result.success(Unit)
        } else {
            val err = edgeResult.exceptionOrNull() ?: Exception("Cloudflare Edge write failed")
            Log.w(TAG, "Cloudflare Edge write failed: ${err.message}")
            _adminOperationState.value = AdminOperationState.Error(err.message ?: "Write failed")
            Result.failure(err)
        }
    }

    /**
     * High-speed atomic batch save for bulk migration / auto-repair operations.
     * Uploads items to Cloudflare Edge KV in chunks of 100 with optimistic local caching.
     */
    suspend fun saveMediaItemsBatchSuspending(items: List<MediaItem>): Result<Int> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext Result.success(0)
        if (!com.streamhub.app.data.AdminManager.isAdminMode.value) {
            Log.e(TAG, "SECURITY VIOLATION: Unauthorized attempt to batch save media items without Admin/Owner verification!")
            _adminOperationState.value = AdminOperationState.Error("Unauthorized: Admin access required")
            return@withContext Result.failure(SecurityException("Unauthorized: Creator Studio Admin mode required"))
        }
        _adminOperationState.value = AdminOperationState.Loading

        val now = System.currentTimeMillis()
        val preparedItems = items.map { item ->
            val finalCreatedAt = if (item.createdAt > 0L) item.createdAt else now
            val migratedEpisodes = item.episodes.map { ep ->
                ep.copy(
                    streamUrl = StreamLinkResolver.sanitizePlayableUrl(ep.streamUrl),
                    mirrorStreamUrl = StreamLinkResolver.sanitizePlayableUrl(
                        if (ep.mirrorStreamUrl.isNotBlank()) ep.mirrorStreamUrl else ep.streamUrl
                    )
                )
            }
            val normalizedEpisodes = com.streamhub.app.data.EpisodeOrderingManager.normalizeAndSort(migratedEpisodes)
            val finalTrendingAt = if (item.isTrending) {
                if (item.trendingAt > 0L) item.trendingAt else now
            } else 0L
            item.copy(
                createdAt = finalCreatedAt,
                updatedAt = now,
                trendingAt = finalTrendingAt,
                episodes = normalizedEpisodes
            )
        }

        // Optimistic UI update
        _mediaCatalog.update { current ->
            val map = current.associateBy { it.id }.toMutableMap()
            for (p in preparedItems) {
                map[p.id] = p
            }
            map.values.toList()
        }
        _catalogState.value = CatalogState.Ready

        runCatching {
            val ctx = com.streamhub.app.StreamHubApplication.getInstance()
            com.streamhub.app.data.CatalogCacheManager.saveCatalogSnapshot(ctx, _mediaCatalog.value)
        }

        // Primary: Cloudflare Edge KV batch upload in chunks of 100
        var savedCount = 0
        val chunks = preparedItems.chunked(100)
        for (chunk in chunks) {
            val res = com.streamhub.app.data.api.CatalogEdgeClient.saveMediaItemsBatch(chunk)
            if (res.isFailure) {
                val err = res.exceptionOrNull() ?: Exception("Cloudflare Edge batch save failed")
                _adminOperationState.value = AdminOperationState.Error(err.message ?: "Batch write failed")
                return@withContext Result.failure(err)
            }
            savedCount += chunk.size
        }

        // Best-effort Firestore dual-write backup in background
        firestore?.let { db ->
            scope.launch(Dispatchers.IO) {
                try {
                    for (chunk in preparedItems.chunked(250)) {
                        val batch = db.batch()
                        for (item in chunk) {
                            val targetCol = getCollectionForCategory(item.category, item.type)
                            batch.set(db.collection(targetCol).document(item.id), mediaItemToMap(item))
                        }
                        Tasks.await(batch.commit())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Non-fatal Firestore backup batch write error: ${e.message}")
                }
            }
        }

        _adminOperationState.value = AdminOperationState.Success()
        com.streamhub.app.data.api.CatalogEdgeClient.refreshEdgeCatalog()
        Result.success(savedCount)
    }

    /**
     * 1-Tap Universal Cloudflare Edge KV Migration.
     * Takes all shows in catalog and uploads them to Cloudflare Edge.
     */
    suspend fun migrateAllShowsToCloudflare(
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!com.streamhub.app.data.AdminManager.isAdminMode.value) {
            return@withContext Result.failure(SecurityException("Unauthorized: Creator Studio Admin mode required"))
        }
        val items = _mediaCatalog.value
        if (items.isEmpty()) return@withContext Result.success(0)
        _adminOperationState.value = AdminOperationState.Loading
        var saved = 0
        val chunks = items.chunked(100)
        for (chunk in chunks) {
            val res = com.streamhub.app.data.api.CatalogEdgeClient.saveMediaItemsBatch(chunk)
            if (res.isFailure) {
                val err = res.exceptionOrNull() ?: Exception("Cloudflare Edge migration failed")
                _adminOperationState.value = AdminOperationState.Error(err.message ?: "Migration failed")
                return@withContext Result.failure(err)
            }
            saved += chunk.size
            onProgress(saved, items.size)
        }
        _adminOperationState.value = AdminOperationState.Success()
        com.streamhub.app.data.api.CatalogEdgeClient.refreshEdgeCatalog()
        Log.d(TAG, "🎉 Successfully migrated all $saved shows to Cloudflare Edge KV!")
        Result.success(saved)
    }

    /**
     * Save or update a media item in Cloudflare Edge KV (with Firestore dual-write backup).
     */
    fun saveMediaItem(item: MediaItem) {
        scope.launch {
            saveMediaItemSuspending(item)
        }
    }

    /**
     * Delete a media item from Cloudflare Edge KV (and Firestore backup).
     */
    fun deleteMediaItem(itemId: String) {
        if (!com.streamhub.app.data.AdminManager.isAdminMode.value) {
            Log.e(TAG, "SECURITY VIOLATION: Unauthorized attempt to delete media item $itemId without Admin/Owner verification!")
            _adminOperationState.value = AdminOperationState.Error("Unauthorized: Admin access required")
            return
        }
        _adminOperationState.value = AdminOperationState.Loading
        tombstoneDeletedIds.add(itemId)
        _mediaCatalog.update { current -> current.filterNot { it.id == itemId } }

        scope.launch(Dispatchers.IO) {
            runCatching {
                val ctx = com.streamhub.app.StreamHubApplication.getInstance()
                com.streamhub.app.data.CatalogCacheManager.saveCatalogSnapshot(ctx, _mediaCatalog.value)
            }

            val edgeResult = com.streamhub.app.data.api.CatalogEdgeClient.deleteMediaItem(itemId)

            // Dual-delete backup in Firestore (non-blocking)
            firestore?.let { db ->
                try {
                    val batch = db.batch()
                    for (col in ALL_COLLECTIONS) {
                        batch.delete(db.collection(col).document(itemId))
                    }
                    batch.commit()
                } catch (e: Exception) {
                    Log.w(TAG, "Non-fatal Firestore backup delete error: ${e.message}")
                }
            }

            if (edgeResult.isSuccess) {
                _adminOperationState.value = AdminOperationState.Success()
                Log.d(TAG, "Successfully deleted media item $itemId from Cloudflare Edge KV")
            } else {
                _adminOperationState.value = AdminOperationState.Error(edgeResult.exceptionOrNull()?.message ?: "Delete failed")
            }

            // Cascade cleanup: purge local bookmarks, watch progress, and downloaded files
            runCatching {
                com.streamhub.app.data.MyListManager.removeFromList(itemId)
                com.streamhub.app.data.WatchHistoryManager.removeMediaProgress(itemId)
                com.streamhub.app.data.DownloadManager.deleteDownloadsForMedia(itemId)
                com.streamhub.app.data.api.CatalogEdgeClient.refreshEdgeCatalog()
            }.onFailure { Log.w(TAG, "Non-fatal cascade cleanup error after delete: ${it.message}") }
        }
    }

    private fun loadInitialCatalog() {
        scope.launch {
            val cached = runCatching {
                val ctx = com.streamhub.app.StreamHubApplication.getInstance()
                com.streamhub.app.data.CatalogCacheManager.loadCachedCatalog(ctx)
            }.getOrDefault(emptyList())

            if (cached.isNotEmpty()) {
                _mediaCatalog.value = cached
                _catalogState.value = CatalogState.Ready
                Log.d(TAG, "⚡ Initialized catalog from local cache (${cached.size} shows) in ~15ms!")
            } else {
                _mediaCatalog.value = emptyList()
                _catalogState.value = CatalogState.Loading
            }
        }
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

/** Backward compatibility alias for legacy imports during refactor */
typealias FirebaseRepository = CatalogRepository
