package com.streamhub.app.ui.screens.downloads

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.R
import com.streamhub.app.ui.theme.bouncyClickable

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the Figma-exact empty state for the Downloads Screen:
 * [DownloadsEmptyState]
 *
 * When the user has not downloaded any movies or anime episodes yet, this composable displays:
 * 1. A clean vector illustration ([R.drawable.ic_figma_empty_downloads_base]).
 * 2. A 14-degree rotated Play Badge styled with the active app theme's primary color.
 * 3. An orbital dot aligned to the illustration.
 * 4. Friendly, non-technical instructions explaining how to save media for offline viewing.
 * 5. An asymmetric CTA button ("Find something to download") that takes the user back to browse.
 * =========================================================================================
 */

@Composable
fun DownloadsEmptyState(
    onFindContent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Dynamic 240x188 Illustration with centered play icon & app theme accent badge
        Box(
            modifier = Modifier.size(width = 240.dp, height = 188.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_figma_empty_downloads_base),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            // 14-Degree Rotated Play Badge (Dynamically adapted to App Theme Crimson Red / Primary)
            Box(
                modifier = Modifier
                    .offset(x = 171.dp, y = 15.dp)
                    .size(42.dp)
                    .graphicsLayer {
                        rotationZ = 14f
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Orbital Circle Dot (Dynamically adapted to App Theme Primary)
            Box(
                modifier = Modifier
                    .offset(x = 24.dp, y = 142.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "No offline downloads yet",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the download icon beside any episode while online to save it for offline playback anywhere.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Exact Asymmetric CTA Button (264x52dp, rx: 22-22-22-10dp, Dynamically adapted to App Theme)
        Box(
            modifier = Modifier
                .size(width = 264.dp, height = 52.dp)
                .bouncyClickable(role = Role.Button) { onFindContent() }
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 10.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_figma_arrow_forward),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Find something to download",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
