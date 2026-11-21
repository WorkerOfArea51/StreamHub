package com.streamhub.app.ui.screens.broadcast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.api.BroadcastPayload

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the active broadcast status card:
 * [BroadcastActiveCard]
 *
 * Displays:
 * 1. A live pulsing emerald green indicator when an announcement is active on Cloudflare Edge.
 * 2. Active status badge: "1 ACTIVE EDGE BROADCAST" vs "NO ACTIVE BROADCAST".
 * 3. Dynamic countdown: "Expires in Xh".
 * 4. Title & message snippet of the active announcement.
 * 5. Quick action buttons: "Edit In Studio" (pre-populates editor) and "Retire from Devices".
 * =========================================================================================
 */

@Composable
fun BroadcastActiveCard(
    activeBroadcast: BroadcastPayload?,
    pulseScale: Float,
    onEditInStudio: (BroadcastPayload) -> Unit,
    onRetireClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activeBroadcast != null) {
                Color(0xFF1B1428)
            } else {
                BroadcastSurfaceDark
            }
        ),
        border = BorderStroke(
            1.dp,
            if (activeBroadcast != null) BroadcastPrimaryPurple.copy(alpha = 0.4f) else BroadcastSurfaceBorder
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(if (activeBroadcast != null) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                if (activeBroadcast != null) BroadcastEmeraldAccent else Color.Gray.copy(alpha = 0.5f)
                            )
                    )
                    Text(
                        text = if (activeBroadcast != null) "1 ACTIVE EDGE BROADCAST" else "NO ACTIVE BROADCAST",
                        color = if (activeBroadcast != null) BroadcastEmeraldAccent else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }

                if (activeBroadcast != null) {
                    val remainingHours = activeBroadcast.expiresAt.let { exp ->
                        val diff = exp - System.currentTimeMillis()
                        if (diff > 0) (diff / 3600_000L).coerceAtLeast(1) else 0
                    }
                    Text(
                        text = "Expires in ${remainingHours}h",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (activeBroadcast != null) {
                Text(
                    text = activeBroadcast.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = activeBroadcast.message,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onEditInStudio(activeBroadcast) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit in studio",
                            tint = BroadcastPrimaryPurple,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit In Studio",
                            color = BroadcastPrimaryPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = onRetireClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Retire from Devices",
                            color = BroadcastRoseAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Ready to broadcast. Publish an announcement below to display on all client Home screens.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
