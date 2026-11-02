package com.streamhub.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.BroadcastManager
import com.streamhub.app.data.api.BroadcastPayload
import com.streamhub.app.ui.components.InAppBroadcastBanner
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * Modern Admin Broadcast Announcement Dialog with Category Selector,
 * 1-Tap Action Deep Linking, and Live WYSIWYG Banner Preview.
 */
@Composable
fun GlobalBroadcastAnnouncementDialog(
    onDismiss: () -> Unit
) {
    var broadcastTitle by remember { mutableStateOf("StreamHub Announcement 📢") }
    var broadcastMessage by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(BroadcastPayload.CATEGORY_INFO) }
    var selectedActionType by remember { mutableStateOf(BroadcastPayload.ACTION_NONE) }
    var actionTarget by remember { mutableStateOf("") }
    var actionLabel by remember { mutableStateOf("") }
    var expiryHours by remember { mutableIntStateOf(24) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Broadcast Announcement",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(scrollState)
            ) {
                Text(
                    text = "Broadcast a live heads-up notification and in-app banner to all users via Cloudflare Edge KV.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                // Category selector pills
                Column(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = "Announcement Category:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Triple(BroadcastPayload.CATEGORY_CONTENT, "Content", Color(0xFF00E676)),
                            Triple(BroadcastPayload.CATEGORY_INFO, "Info", Color(0xFF38BDF8)),
                            Triple(BroadcastPayload.CATEGORY_ALERT, "Alert", Color(0xFFFFA726))
                        ).forEach { (cat, label, col) ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) col.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) col else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = broadcastTitle,
                    onValueChange = { broadcastTitle = it },
                    label = { Text("Broadcast Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Message
                OutlinedTextField(
                    value = broadcastMessage,
                    onValueChange = { broadcastMessage = it },
                    label = { Text("Broadcast Message") },
                    placeholder = { Text("e.g. Minions & Monsters is now streaming in full 1080p!") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Action Deep Link Selector
                Column(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = "Interactive 1-Tap Action:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            BroadcastPayload.ACTION_NONE to "None",
                            BroadcastPayload.ACTION_WATCH to "Watch Show",
                            BroadcastPayload.ACTION_LINK to "Open Link"
                        ).forEach { (type, label) ->
                            val isSelected = selectedActionType == type
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .clickable { selectedActionType = type }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF38BDF8) else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Target input (if action enabled)
                if (selectedActionType != BroadcastPayload.ACTION_NONE) {
                    val isWatch = selectedActionType == BroadcastPayload.ACTION_WATCH
                    OutlinedTextField(
                        value = actionTarget,
                        onValueChange = { actionTarget = it },
                        label = { Text(if (isWatch) "Show Media ID" else "External URL") },
                        placeholder = { Text(if (isWatch) "e.g. minions_monsters_2026" else "e.g. https://t.me/...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = actionLabel,
                        onValueChange = { actionLabel = it },
                        label = { Text("Button Text (Optional)") },
                        placeholder = { Text(if (isWatch) "Watch Now" else "Open Link") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Expiration duration selector
                Column(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = "Notification Lifespan (Auto-Expires After):",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(6 to "6h", 12 to "12h", 24 to "24h", 48 to "48h").forEach { (hrs, label) ->
                            val isSelected = expiryHours == hrs
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color(0xFF00E676).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .clickable { expiryHours = hrs }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF00E676) else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Live WYSIWYG Preview Box
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        text = "Live In-App Banner Preview:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val previewPayload = BroadcastPayload(
                        id = "preview",
                        title = broadcastTitle.ifBlank { "StreamHub Announcement" },
                        message = broadcastMessage.ifBlank { "Preview announcement text will appear here." },
                        category = selectedCategory,
                        actionType = selectedActionType,
                        actionTarget = actionTarget,
                        actionLabel = actionLabel
                    )
                    InAppBroadcastBanner(
                        broadcast = previewPayload,
                        onDismiss = {},
                        onActionClick = { _, _ -> },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (broadcastMessage.isNotBlank()) {
                        BroadcastManager.sendGlobalBroadcast(
                            title = broadcastTitle.trim(),
                            message = broadcastMessage.trim(),
                            expiryHours = expiryHours,
                            category = selectedCategory,
                            actionType = selectedActionType,
                            actionTarget = actionTarget.trim(),
                            actionLabel = actionLabel.trim()
                        )
                        ToastManager.showToast("Broadcast announcement published to all users!")
                        onDismiss()
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                modifier = Modifier.bouncyTouch()
            ) {
                Text("Broadcast to All", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        BroadcastManager.clearActiveBroadcast { ok ->
                            ToastManager.showToast(if (ok) "Active broadcast cleared" else "Failed to clear broadcast")
                        }
                        onDismiss()
                    },
                    shape = CircleShape,
                    modifier = Modifier.bouncyTouch()
                ) {
                    Text("Clear Active", color = PrimaryRed, fontSize = 12.sp)
                }
                TextButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier.bouncyTouch()
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    )
}
