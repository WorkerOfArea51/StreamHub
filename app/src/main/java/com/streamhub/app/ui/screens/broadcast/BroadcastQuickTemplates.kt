package com.streamhub.app.ui.screens.broadcast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the 1-tap quick template presets for the Broadcast Studio:
 * [BroadcastQuickTemplatesRow] & [TemplateChip]
 *
 * Pre-configures announcement content in one tap:
 * 1. "New Release" - Sets Content tone, Watch Now CTA, and 24-hour lifespan.
 * 2. "App Update" - Sets Notice tone, Changelog external link CTA, and 72-hour lifespan.
 * 3. "Maintenance" - Sets Urgent tone, None CTA, and 6-hour lifespan.
 * =========================================================================================
 */

@Composable
fun BroadcastQuickTemplatesRow(
    onApplyNewRelease: () -> Unit,
    onApplyAppUpdate: () -> Unit,
    onApplyMaintenance: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "⚡ 1-TAP QUICK TEMPLATES",
            color = Color.Gray,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TemplateChip(
                title = "New Release",
                subtitle = "Movie / Drop",
                emoji = "🎬",
                accentColor = BroadcastEmeraldAccent,
                modifier = Modifier.weight(1f),
                onClick = onApplyNewRelease
            )

            TemplateChip(
                title = "App Update",
                subtitle = "Changelog",
                emoji = "🚀",
                accentColor = BroadcastSkyAccent,
                modifier = Modifier.weight(1f),
                onClick = onApplyAppUpdate
            )

            TemplateChip(
                title = "Maintenance",
                subtitle = "Server Notice",
                emoji = "⚠️",
                accentColor = BroadcastAmberAccent,
                modifier = Modifier.weight(1f),
                onClick = onApplyMaintenance
            )
        }
    }
}

@Composable
fun TemplateChip(
    title: String,
    subtitle: String,
    emoji: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BroadcastSurfaceDark,
        border = BorderStroke(1.dp, BroadcastSurfaceBorder),
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = emoji, fontSize = 12.sp)
                Text(text = title, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = subtitle, color = Color.Gray, fontSize = 9.5.sp)
        }
    }
}
