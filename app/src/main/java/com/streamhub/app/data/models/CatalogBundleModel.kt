package com.streamhub.app.data.models

/**
 * Represents a single bundle document inside the `catalog_bundles` Firestore collection.
 * Holds full MediaItem objects (including all episodes and streaming URLs) up to a strict
 * 970 KB threshold.
 */
data class CatalogBundle(
    val bundleId: String = "",           // e.g. "movies_bundle_part_1", "animes_bundle_part_1"
    val category: String = "",           // "Anime", "Movies", "Series"
    val partIndex: Int = 1,              // 1, 2, 3...
    val totalParts: Int = 1,             // Total parts for this category
    val showsCount: Int = 0,             // Number of shows in this bundle
    val episodesCount: Int = 0,          // Total episodes across all shows in this bundle
    val sizeBytes: Long = 0L,            // Approximate byte size
    val updatedAt: Long = 0L,            // Timestamp of update
    val items: List<MediaItem> = emptyList() // The complete MediaItem objects including episodes
)
