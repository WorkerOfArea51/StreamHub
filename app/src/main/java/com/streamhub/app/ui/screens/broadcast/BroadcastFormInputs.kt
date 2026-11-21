package com.streamhub.app.ui.screens.broadcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.api.BroadcastPayload

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the complete form input cards for composing announcements:
 *
 * 1. [BroadcastToneSelector] - Tone cards: "Content" (Emerald), "Notice" (Sky), "Urgent" (Amber).
 * 2. [BroadcastTextFieldsCard] - Title and message fields with character count indicators.
 * 3. [BroadcastActionCard] - CTA selector (None / Watch Show / Web Link) and target link inputs.
 * 4. [BroadcastLifespanSelector] - Auto-expiration time buttons (1h, 6h, 12h, 24h, 3d, 7d).
 * =========================================================================================
 */

@Composable
fun BroadcastToneSelector(
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "ANNOUNCEMENT TONE",
            color = Color.Gray,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryToneCard(
                title = "Content",
                subtitle = "Drops & Movies",
                icon = Icons.Default.AutoAwesome,
                accentColor = BroadcastEmeraldAccent,
                isSelected = selectedCategory == BroadcastPayload.CATEGORY_CONTENT,
                modifier = Modifier.weight(1f),
                onClick = { onSelectCategory(BroadcastPayload.CATEGORY_CONTENT) }
            )
            CategoryToneCard(
                title = "Notice",
                subtitle = "Updates & Tips",
                icon = Icons.Default.Info,
                accentColor = BroadcastSkyAccent,
                isSelected = selectedCategory == BroadcastPayload.CATEGORY_INFO,
                modifier = Modifier.weight(1f),
                onClick = { onSelectCategory(BroadcastPayload.CATEGORY_INFO) }
            )
            CategoryToneCard(
                title = "Urgent",
                subtitle = "Alerts & Maint.",
                icon = Icons.Default.WarningAmber,
                accentColor = BroadcastAmberAccent,
                isSelected = selectedCategory == BroadcastPayload.CATEGORY_ALERT,
                modifier = Modifier.weight(1f),
                onClick = { onSelectCategory(BroadcastPayload.CATEGORY_ALERT) }
            )
        }
    }
}

@Composable
fun CategoryToneCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else BroadcastSurfaceDark,
        border = BorderStroke(1.dp, if (isSelected) accentColor else BroadcastSurfaceBorder),
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    color = if (isSelected) accentColor else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun BroadcastTextFieldsCard(
    title: String,
    onTitleChange: (String) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BroadcastSurfaceDark),
        border = BorderStroke(1.dp, BroadcastSurfaceBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ANNOUNCEMENT TITLE",
                        color = Color.Gray,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${title.length}/60",
                        color = if (title.length > 55) BroadcastAmberAccent else Color.Gray,
                        fontSize = 10.sp
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) onTitleChange(it) },
                    placeholder = { Text("e.g. Minions & Monsters Now Streaming", color = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BroadcastPrimaryPurple,
                        unfocusedBorderColor = BroadcastSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F0F1A),
                        unfocusedContainerColor = Color(0xFF0F0F1A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Message Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ANNOUNCEMENT MESSAGE",
                        color = Color.Gray,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${message.length}/240",
                        color = if (message.length > 220) BroadcastAmberAccent else Color.Gray,
                        fontSize = 10.sp
                    )
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= 240) onMessageChange(it) },
                    placeholder = { Text("Write announcement message for client devices...", color = Color.Gray) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BroadcastPrimaryPurple,
                        unfocusedBorderColor = BroadcastSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F0F1A),
                        unfocusedContainerColor = Color(0xFF0F0F1A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun BroadcastActionCard(
    actionType: String,
    onSelectActionType: (String) -> Unit,
    actionTarget: String,
    onActionTargetChange: (String) -> Unit,
    actionLabel: String,
    onActionLabelChange: (String) -> Unit,
    onOpenCatalogPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BroadcastSurfaceDark),
        border = BorderStroke(1.dp, BroadcastSurfaceBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IN-BANNER CALL TO ACTION",
                    color = Color.Gray,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )

                if (actionType == BroadcastPayload.ACTION_WATCH) {
                    TextButton(
                        onClick = onOpenCatalogPicker,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Pick Show",
                            tint = BroadcastEmeraldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pick Show from Catalog ↗",
                            color = BroadcastEmeraldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Action Type Segmented Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F1A), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionTypePill(
                    label = "None",
                    isSelected = actionType == BroadcastPayload.ACTION_NONE,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectActionType(BroadcastPayload.ACTION_NONE) }
                )
                ActionTypePill(
                    label = "🎬 Watch Show",
                    isSelected = actionType == BroadcastPayload.ACTION_WATCH,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectActionType(BroadcastPayload.ACTION_WATCH) }
                )
                ActionTypePill(
                    label = "🔗 Web Link",
                    isSelected = actionType == BroadcastPayload.ACTION_LINK,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectActionType(BroadcastPayload.ACTION_LINK) }
                )
            }

            // Dynamic Input Fields
            AnimatedVisibility(visible = actionType != BroadcastPayload.ACTION_NONE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = actionTarget,
                        onValueChange = onActionTargetChange,
                        label = {
                            Text(
                                if (actionType == BroadcastPayload.ACTION_WATCH) {
                                    "Show Title or Media ID"
                                } else {
                                    "External Web Link / URL"
                                }
                            )
                        },
                        placeholder = {
                            Text(
                                if (actionType == BroadcastPayload.ACTION_WATCH) {
                                    "e.g. Minions & Monsters (or tap Pick Show above)"
                                } else {
                                    "e.g. https://t.me/streamhub_channel"
                                },
                                color = Color.Gray
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BroadcastPrimaryPurple,
                            unfocusedBorderColor = BroadcastSurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F0F1A),
                            unfocusedContainerColor = Color(0xFF0F0F1A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = actionLabel,
                        onValueChange = onActionLabelChange,
                        label = { Text("Button Label") },
                        placeholder = { Text("e.g. Watch Now", color = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BroadcastPrimaryPurple,
                            unfocusedBorderColor = BroadcastSurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F0F1A),
                            unfocusedContainerColor = Color(0xFF0F0F1A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ActionTypePill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) BroadcastPrimaryPurple else Color.Transparent,
        modifier = modifier
            .height(30.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun BroadcastLifespanSelector(
    expiryHours: Int,
    onSelectExpiryHours: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "AUTO-EXPIRATION LIFESPAN",
                color = Color.Gray,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = if (expiryHours == 168) "7 Days (Auto-decay)" else "$expiryHours Hours (Auto-decay)",
                color = BroadcastPrimaryPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1 to "1h", 6 to "6h", 12 to "12h", 24 to "24h", 72 to "3d", 168 to "7d").forEach { (h, lbl) ->
                val isSel = expiryHours == h
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) BroadcastPrimaryPurple else BroadcastSurfaceDark,
                    border = BorderStroke(1.dp, if (isSel) BroadcastPrimaryPurple else BroadcastSurfaceBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable { onSelectExpiryHours(h) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = lbl,
                            color = if (isSel) Color.White else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
