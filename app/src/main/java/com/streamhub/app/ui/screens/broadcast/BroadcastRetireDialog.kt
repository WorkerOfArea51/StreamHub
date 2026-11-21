package com.streamhub.app.ui.screens.broadcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the safety confirmation dialog for retiring an active announcement:
 * [BroadcastRetireDialog]
 *
 * Prompts the administrator before wiping the active broadcast from Cloudflare Edge KV
 * storage, immediately purging the announcement banner across all client devices.
 * =========================================================================================
 */

@Composable
fun BroadcastRetireDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BroadcastSurfaceCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = BroadcastRoseAccent)
                Text(text = "Retire Broadcast?", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                text = "This will immediately remove the active announcement from all user devices and clear it from Cloudflare Edge.",
                color = Color.LightGray,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = BroadcastRoseAccent)
            ) {
                Text(text = "Retire Now", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = Color.Gray)
            }
        }
    )
}
