package com.streamhub.app.ui.screens.downloads

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * [DownloadedSeriesGroup.kt] defines the data model and expiration badge utilities
 * for grouping multi-episode offline shows in StreamHub:
 *
 * 1. [DownloadedSeriesGroup]:
 *    - Groups all downloaded episodes of a show under a single mediaId.
 *    - Prevents card duplication in the downloads list.
 *    - Tracks total MBs, completed count, downloading count, and standalone flag.
 *
 * 2. [formatDownloadExpiryBadge]:
 *    - Computes user-friendly expiration countdown text (e.g. "Expires in 30d", "Expires tomorrow")
 *      based on the user's configured Retention TTL.
 * =========================================================================================
 */

import com.streamhub.app.data.DownloadedItem
import java.io.File

/**
 * Clean data holder for a show with its downloaded episodes.
 */
data class DownloadedSeriesGroup(
    val mediaId: String,
    val mediaTitle: String,
    val posterUrl: String,
    val episodes: List<DownloadedItem>,
    val totalFileSizeMb: Double,
    val completedCount: Int,
    val downloadingCount: Int,
    val isSingleStandaloneItem: Boolean
)

/**
 * Helper to compute human-friendly expiration countdown text for offline media files.
 */
fun formatDownloadExpiryBadge(completedAt: Long, localFilePath: String, ttlDays: Int): String {
    if (ttlDays <= 0) return "Never expires"
    val now = System.currentTimeMillis()
    val file = File(localFilePath)
    val effectiveTime = if (completedAt > 0L) completedAt else if (file.exists()) file.lastModified() else now
    val age = now - effectiveTime
    val ttlMillis = ttlDays * 24L * 3600_000L
    val daysLeft = ((ttlMillis - age) / (24L * 3600_000L)).toInt()
    return when {
        daysLeft > 1 -> "Expires in ${daysLeft}d"
        daysLeft == 1 -> "Expires tomorrow"
        daysLeft == 0 -> "Expires today"
        else -> "Expiring soon"
    }
}
