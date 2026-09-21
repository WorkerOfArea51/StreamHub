package com.streamhub.app.data.importer

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.streamhub.app.data.models.CatalogBundle
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Smart Catalog Bundler Engine with 970 KB Auto-Split Partitioning.
 *
 * Encapsulates full MediaItem objects (all metadata + all episodes & streaming links)
 * into category bundles stored under the `catalog_bundles` Firestore collection.
 *
 * Invariant: Never splits or truncates a MediaItem. If adding an item to the current part
 * would cause the bundle to exceed 970 KB, the current part is sealed and the entire show
 * is placed into the next part.
 */
object CatalogBundleManager {

    private const val TAG = "CatalogBundleManager"
    const val COLLECTION_BUNDLES = "catalog_bundles"

    // Strict 970 KB auto-split limit (970 * 1024 = 993,280 bytes)
    const val BUNDLE_MAX_BYTES = 970 * 1024L

    // Overhead for bundle envelope fields (bundleId, category, partIndex, counts, timestamps)
    private const val BASE_ENVELOPE_BYTES = 600L

    private val gson: Gson by lazy { GsonBuilder().create() }

    /**
     * Accurately calculates the UTF-8 byte size of a MediaItem when serialized.
     */
    fun estimateItemBytes(item: MediaItem): Long {
        return try {
            gson.toJson(item).toByteArray(Charsets.UTF_8).size.toLong()
        } catch (e: Exception) {
            Log.w(TAG, "Error estimating item bytes for ${item.id}", e)
            4096L
        }
    }

    /**
     * Packs a list of MediaItems into one or more CatalogBundle parts, each staying strictly under 970 KB.
     * Guaranteed: No MediaItem is ever cut in half.
     */
    fun packCategory(
        categoryName: String,
        prefix: String,
        items: List<MediaItem>
    ): List<CatalogBundle> {
        if (items.isEmpty()) return emptyList()

        val bundles = mutableListOf<CatalogBundle>()
        var currentPart = 1
        val currentItems = mutableListOf<MediaItem>()
        var currentBytes = 0L

        for (item in items) {
            val itemBytes = estimateItemBytes(item)

            // If current part already has items and adding this item would exceed 970 KB:
            if (currentItems.isNotEmpty() && (BASE_ENVELOPE_BYTES + currentBytes + itemBytes > BUNDLE_MAX_BYTES)) {
                // Seal current part
                val totalEps = currentItems.sumOf { it.episodes.size }
                bundles.add(
                    CatalogBundle(
                        bundleId = "${prefix}${currentPart}",
                        category = categoryName,
                        partIndex = currentPart,
                        totalParts = 1, // updated below
                        showsCount = currentItems.size,
                        episodesCount = totalEps,
                        sizeBytes = BASE_ENVELOPE_BYTES + currentBytes,
                        updatedAt = System.currentTimeMillis(),
                        items = currentItems.toList()
                    )
                )

                // Start next part with this entire item
                currentPart++
                currentItems.clear()
                currentItems.add(item)
                currentBytes = itemBytes
            } else {
                currentItems.add(item)
                currentBytes += itemBytes
            }
        }

        // Add trailing part
        if (currentItems.isNotEmpty()) {
            val totalEps = currentItems.sumOf { it.episodes.size }
            bundles.add(
                CatalogBundle(
                    bundleId = "${prefix}${currentPart}",
                    category = categoryName,
                    partIndex = currentPart,
                    totalParts = currentPart,
                    showsCount = currentItems.size,
                    episodesCount = totalEps,
                    sizeBytes = BASE_ENVELOPE_BYTES + currentBytes,
                    updatedAt = System.currentTimeMillis(),
                    items = currentItems.toList()
                )
            )
        }

        val totalParts = bundles.size
        return bundles.map { it.copy(totalParts = totalParts) }
    }

    /**
     * Packs the entire catalog across Anime, Movies, and Series into optimized CatalogBundles.
     */
    fun packEntireCatalog(catalog: List<MediaItem>): List<CatalogBundle> {
        val animes = catalog.filter { it.category.equals("ANIME", ignoreCase = true) }
        val movies = catalog.filter {
            !it.category.equals("ANIME", ignoreCase = true) &&
                (it.category.equals("MOVIE", ignoreCase = true) ||
                    it.category.equals("MOVIES", ignoreCase = true) ||
                    it.type.equals("MOVIE", ignoreCase = true))
        }
        val series = catalog.filter {
            !it.category.equals("ANIME", ignoreCase = true) &&
                !it.category.equals("MOVIE", ignoreCase = true) &&
                !it.category.equals("MOVIES", ignoreCase = true) &&
                !it.type.equals("MOVIE", ignoreCase = true)
        }

        val movieBundles = packCategory("Movies", "movies_bundle_part_", movies)
        val seriesBundles = packCategory("Series", "series_bundle_part_", series)
        val animeBundles = packCategory("Anime", "animes_bundle_part_", animes)

        return movieBundles + seriesBundles + animeBundles
    }

    /**
     * Converts a CatalogBundle into a Firestore-compatible Map with full MediaItem details.
     */
    fun bundleToMap(bundle: CatalogBundle): Map<String, Any?> {
        return mapOf(
            "bundleId" to bundle.bundleId,
            "category" to bundle.category,
            "partIndex" to bundle.partIndex,
            "totalParts" to bundle.totalParts,
            "showsCount" to bundle.showsCount,
            "episodesCount" to bundle.episodesCount,
            "sizeBytes" to bundle.sizeBytes,
            "updatedAt" to bundle.updatedAt,
            "items" to bundle.items.map { FirebaseRepository.mediaItemToMap(it) }
        )
    }

    /**
     * Uploads the given bundles to Firestore collection `catalog_bundles`.
     * Also removes any obsolete higher part numbers that are no longer needed.
     */
    suspend fun uploadBundlesToFirestore(
        db: FirebaseFirestore,
        bundles: List<CatalogBundle>,
        onProgress: (current: Int, total: Int, bundleName: String) -> Unit = { _, _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val collectionRef = db.collection(COLLECTION_BUNDLES)
            var uploaded = 0

            for ((index, bundle) in bundles.withIndex()) {
                onProgress(index + 1, bundles.size, bundle.bundleId)
                val docMap = bundleToMap(bundle)
                Tasks.await(collectionRef.document(bundle.bundleId).set(docMap))
                uploaded++
                Log.d(TAG, "Uploaded bundle ${bundle.bundleId} (${bundle.showsCount} shows, ${bundle.sizeBytes / 1024} KB)")
            }

            // Clean up obsolete parts per prefix
            val prefixes = bundles.map { it.bundleId.substringBeforeLast('_') + "_" }.distinct()
            for (prefix in prefixes) {
                cleanupStaleParts(db, prefix, bundles.map { it.bundleId }.toSet())
            }

            Result.success(uploaded)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload bundles to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes any Firestore documents starting with `prefix` that are no longer part of `activePartIds`.
     */
    private suspend fun cleanupStaleParts(
        db: FirebaseFirestore,
        prefix: String,
        activePartIds: Set<String>
    ) {
        try {
            val snapshot = Tasks.await(db.collection(COLLECTION_BUNDLES).get())
            for (doc in snapshot.documents) {
                if (doc.id.startsWith(prefix) && !activePartIds.contains(doc.id)) {
                    Log.d(TAG, "Cleaning up obsolete bundle part: ${doc.id}")
                    Tasks.await(db.collection(COLLECTION_BUNDLES).document(doc.id).delete())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal error cleaning up stale bundle parts for prefix $prefix: ${e.message}")
        }
    }

    /**
     * Incremental background sync: Re-packs only the category of the modified/deleted show
     * and updates its corresponding bundles in Firestore without touching other categories.
     */
    suspend fun syncCategoryBundles(
        db: FirebaseFirestore,
        category: String,
        type: String,
        fullCatalog: List<MediaItem>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isAnime = category.equals("ANIME", ignoreCase = true)
            val isMovie = !isAnime && (category.equals("MOVIE", ignoreCase = true) || category.equals("MOVIES", ignoreCase = true) || type.equals("MOVIE", ignoreCase = true))

            val (categoryName, prefix, items) = when {
                isAnime -> Triple(
                    "Anime",
                    "animes_bundle_part_",
                    fullCatalog.filter { it.category.equals("ANIME", ignoreCase = true) }
                )
                isMovie -> Triple(
                    "Movies",
                    "movies_bundle_part_",
                    fullCatalog.filter {
                        !it.category.equals("ANIME", ignoreCase = true) &&
                            (it.category.equals("MOVIE", ignoreCase = true) ||
                                it.category.equals("MOVIES", ignoreCase = true) ||
                                it.type.equals("MOVIE", ignoreCase = true))
                    }
                )
                else -> Triple(
                    "Series",
                    "series_bundle_part_",
                    fullCatalog.filter {
                        !it.category.equals("ANIME", ignoreCase = true) &&
                            !it.category.equals("MOVIE", ignoreCase = true) &&
                            !it.category.equals("MOVIES", ignoreCase = true) &&
                            !it.type.equals("MOVIE", ignoreCase = true)
                    }
                )
            }

            val categoryBundles = packCategory(categoryName, prefix, items)
            uploadBundlesToFirestore(db, categoryBundles)
            Log.d(TAG, "Incremental sync complete for $categoryName (${categoryBundles.size} parts)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed incremental bundle sync for category $category", e)
            Result.failure(e)
        }
    }
}
