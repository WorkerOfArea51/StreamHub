package com.streamhub.app.ui.screens.downloads

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.DownloadedItem
import com.streamhub.app.ui.components.ToastManager
import java.util.Locale

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the safety confirmation dialogs for offline downloads in StreamHub:
 *
 * 1. [DeleteEpisodeDialog] - Asks for confirmation before removing a single downloaded episode.
 * 2. [DeleteSeriesDialog] - Asks for confirmation before wiping all downloaded episodes of a series.
 * 3. [DeleteBatchEpisodesDialog] - Asks for confirmation before wiping multiple user-selected episodes.
 *
 * Each dialog displays the exact file name / series title, the amount of device storage that
 * will be freed up in MB, and provides red destructive confirmation buttons with toast feedback.
 * =========================================================================================
 */

/**
 * Single Episode Delete Confirmation Dialog
 */
@Composable
fun DeleteEpisodeDialog(
    item: DownloadedItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Download?",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"${item.episodeTitle.ifBlank { item.mediaTitle }}\"? This file will be removed from your device storage.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    DownloadManager.deleteDownload(item)
                    ToastManager.showToast("Deleted episode download", Icons.Default.Delete)
                }
            ) {
                Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
    )
}

/**
 * Batch Series Delete Confirmation Dialog
 */
@Composable
fun DeleteSeriesDialog(
    group: DownloadedSeriesGroup,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Entire Series?",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete all ${group.episodes.size} downloaded episodes of \"${group.mediaTitle}\"? This will immediately free up ${String.format(Locale.US, "%.1f", group.totalFileSizeMb)} MB of device storage.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetId = group.mediaId
                    val count = group.episodes.size
                    onDismiss()
                    DownloadManager.deleteDownloadsForMedia(targetId)
                    ToastManager.showToast("Deleted $count episodes of ${group.mediaTitle}", Icons.Default.Delete)
                }
            ) {
                Text("Delete All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
    )
}

/**
 * Multi-Select Batch Episodes Delete Confirmation Dialog
 */
@Composable
fun DeleteBatchEpisodesDialog(
    batch: List<DownloadedItem>,
    onDismiss: () -> Unit
) {
    val totalMb = batch.sumOf { it.fileSizeMb.toDouble() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete ${batch.size} Episodes?",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete ${batch.size} selected downloaded episodes? This will immediately free up ${String.format(Locale.US, "%.1f", totalMb)} MB of device storage.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val count = batch.size
                    onDismiss()
                    batch.forEach { ep ->
                        DownloadManager.deleteDownload(ep)
                    }
                    ToastManager.showToast("Deleted $count episodes (${String.format(Locale.US, "%.1f", totalMb)} MB freed)", Icons.Default.Delete)
                }
            ) {
                Text("Delete (${batch.size})", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
    )
}
