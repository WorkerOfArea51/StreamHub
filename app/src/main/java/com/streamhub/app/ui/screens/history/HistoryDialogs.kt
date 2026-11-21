package com.streamhub.app.ui.screens.history

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the safety confirmation dialog for clearing Watch History:
 * [ClearHistoryDialog]
 *
 * Confirms before wiping the user's chronological watch progress records.
 * =========================================================================================
 */

@Composable
fun ClearHistoryDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(28.dp)
            )
        },
        title = { Text("Clear All History?", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text("This will permanently remove all watch progress and continue watching history.", color = TextSecondary) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                modifier = Modifier.bouncyTouch()
            ) {
                Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape,
                modifier = Modifier.bouncyTouch()
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
    )
}
