package com.streamhub.app.ui.screens.storage

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the safety confirmation dialogs for disk and cache operations:
 *
 * 1. [ClearAllCacheDialog] - Confirms wiping video buffers, image posters, and JSON metadata.
 * 2. [DeleteShowDownloadsDialog] - Confirms removing all offline files for a specific series.
 * 3. [DeleteAllDownloadsDialog] - Confirms deleting the entire offline library.
 * =========================================================================================
 */

@Composable
fun ClearAllCacheDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear All Cache?", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "This will delete all temporary video streaming chunks, poster images, and temporary cache. Your watch history, favorites, and offline downloads will remain safe.",
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                modifier = Modifier.bouncyTouch(),
                onClick = onConfirm
            ) {
                Text("Clear All", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                shape = CircleShape,
                modifier = Modifier.bouncyTouch(),
                onClick = onDismiss
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun DeleteShowDownloadsDialog(
    showTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Downloads?", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Are you sure you want to delete all downloaded files for \"$showTitle\"? This cannot be undone.",
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                modifier = Modifier.bouncyTouch(),
                onClick = onConfirm
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                shape = CircleShape,
                modifier = Modifier.bouncyTouch(),
                onClick = onDismiss
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun DeleteAllDownloadsDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete All Offline Downloads?", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "This will permanently remove all downloaded videos and episodes from your device storage.",
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                modifier = Modifier.bouncyTouch(),
                onClick = onConfirm
            ) {
                Text("Delete All", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                shape = CircleShape,
                modifier = Modifier.bouncyTouch(),
                onClick = onDismiss
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
    )
}
