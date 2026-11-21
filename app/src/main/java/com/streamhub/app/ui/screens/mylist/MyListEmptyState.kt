package com.streamhub.app.ui.screens.mylist

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * [MyListEmptyState.kt] renders the Figma-exact whimsical empty state illustration
 * when the user has no saved shows in a selected category:
 *
 * 1. [MyListEmptyIllustration]:
 *    - Vector artwork base (outer orbit oval, soft lavender pebble, bookmark ribbon with +).
 *    - 14-Degree rotated squircle badge with plus (+) inside in primary accent.
 *    - Orbital blinking & bobbing coral dot with live radial bloom ("going down and coming up").
 *
 * 2. [MyListEmptyState]:
 *    - Empty state title & descriptive subtitle matching the selected category.
 *    - "Explore your next watch" primary CTA button navigating back to catalog.
 * =========================================================================================
 */

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.R

@Composable
fun MyListEmptyIllustration(
    primaryColor: Color
) {
    Box(
        modifier = Modifier.size(width = 240.dp, height = 188.dp),
        contentAlignment = Alignment.TopStart
    ) {
        // 1. Exact Vector Artwork Base
        Image(
            painter = painterResource(id = R.drawable.ic_figma_empty_watchlist_base),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        // 2. 14-Degree Rotated Squircle Badge with Plus (+) inside in Primary Accent
        Box(
            modifier = Modifier
                .offset(x = 171.dp, y = 15.dp)
                .size(42.dp)
                .graphicsLayer {
                    rotationZ = 14f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .clip(RoundedCornerShape(14.dp))
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // 3. Orbital Blinking & Bobbing Coral Dot with Live Glow Animation
        val infiniteTransition = rememberInfiniteTransition(label = "EmptyWatchlistDot")
        val bobOffset by infiniteTransition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DotBob"
        )
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DotPulse"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DotGlow"
        )

        // Dot Box positioned along orbit at cx=30, cy=148
        Box(
            modifier = Modifier
                .offset(x = 18.dp, y = (136f + bobOffset).dp)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer Glowing Radial Bloom
            Box(
                modifier = Modifier
                    .size(24.dp * pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = glowAlpha * 0.75f),
                                primaryColor.copy(alpha = glowAlpha * 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
            // Solid Core Dot
            Box(
                modifier = Modifier
                    .size(11.dp * pulseScale)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
        }
    }
}

@Composable
fun MyListEmptyState(
    selectedStatus: MyListStatusCategory,
    onExploreClick: (() -> Unit)?,
    onResetFilters: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val emptyTitle = when (selectedStatus) {
        MyListStatusCategory.ALL -> "Your watchlist is empty"
        MyListStatusCategory.IN_PROGRESS -> "No Shows In Progress"
        MyListStatusCategory.WATCH_LATER -> "No Watch Later Titles"
        MyListStatusCategory.FAVORITES -> "No Favorites Saved Yet"
        MyListStatusCategory.COMPLETED -> "No Completed Titles Yet"
        MyListStatusCategory.COLLECTIONS -> "No Items in this Folder"
    }
    val emptySubtitle = when (selectedStatus) {
        MyListStatusCategory.ALL -> "Tap \"+ My List\" on any show to bookmark it\nhere for quick access anytime."
        MyListStatusCategory.IN_PROGRESS -> "Start watching any anime or movie to track your resume points here!"
        MyListStatusCategory.WATCH_LATER -> "Saved shows you haven't started yet will appear here."
        MyListStatusCategory.FAVORITES -> "Tap the heart on any title card to add it to your loved favorites list."
        MyListStatusCategory.COMPLETED -> "Finished anime and movies will be organized here."
        MyListStatusCategory.COLLECTIONS -> "Move titles into custom folders to organize your collection."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MyListEmptyIllustration(primaryColor = primaryColor)

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = emptyTitle,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = emptySubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // CTA Button: Explore your next watch
        Button(
            onClick = {
                onResetFilters()
                onExploreClick?.invoke()
            },
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier
                .width(264.dp)
                .height(52.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Explore your next watch",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
