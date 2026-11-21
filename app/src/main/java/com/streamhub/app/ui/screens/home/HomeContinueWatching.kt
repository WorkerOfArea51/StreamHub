package com.streamhub.app.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.ui.components.MinTouchTarget
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * ⏯️ WHAT IS THIS FILE?
 * This file handles the interactive "Continue Watching" showcase on the Home Screen.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * 1. Shows a sleek showcase card of movies or episodes you recently started watching.
 * 2. Displays exact remaining time (e.g. "24m left") and a red/colored progress bar.
 * 3. Supports smooth horizontal card-swipe physics:
 *    - Swiping transitions between active cards and compact "pill" previews.
 * 4. Lets you tap to instantly resume playback right where you left off.
 * 5. Includes an 'X' button to remove a show from your continue watching list.
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Reads data from: `WatchHistoryManager.kt`.
 * - Triggered by: `HomeScreen.kt` when the user has incomplete watch sessions.
 */

@Composable
fun ContinueWatchingSection(
    continueWatchingList: List<Pair<MediaItem, PlaybackProgress>>,
    onPlayEpisode: (MediaItem, Int) -> Unit,
    onNavigateToHistory: () -> Unit,
    onClearClick: () -> Unit,
    onRemove: (MediaItem, PlaybackProgress) -> Unit,
    onLongClick: (Pair<MediaItem, PlaybackProgress>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (continueWatchingList.isEmpty()) return

    val n = continueWatchingList.size
    val primaryColor = MaterialTheme.colorScheme.primary
    val coroutineScope = rememberCoroutineScope()
    val currentScroll = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Clamp scroll bounds when items are removed
    LaunchedEffect(n) {
        if (currentScroll.value > (n - 1).toFloat()) {
            currentScroll.snapTo((n - 1).toFloat().coerceAtLeast(0f))
        }
    }

    val scrollVal = currentScroll.value
    val activeIndex = scrollVal.roundToInt().coerceIn(0, (n - 1).coerceAtLeast(0))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 8.dp)
    ) {
        // ── 1. TOP HEADER (Icon + "Continue Watching" title + History & Clear buttons) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play Squircle with theme gradient
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.78f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.22f, size.height * 0.15f)
                            lineTo(size.width * 0.22f, size.height * 0.85f)
                            lineTo(size.width * 0.90f, size.height * 0.50f)
                            close()
                        }
                        drawPath(path, color = Color.White)
                    }
                }

                Text(
                    text = "Continue Watching",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick actions on right
            Row(verticalAlignment = Alignment.CenterVertically) {
                MinTouchTarget(
                    onClick = onNavigateToHistory,
                    contentDescription = "Navigate to watch history"
                ) {
                    Text(
                        text = "History ↗",
                        color = primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                MinTouchTarget(
                    onClick = onClearClick,
                    contentDescription = "Clear continue watching history"
                ) {
                    Text(
                        text = "Clear",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── 2. FLUID SHOWCASE STAGE (Interactive horizontal swipe between cards) ──
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .padding(horizontal = 16.dp)
        ) {
            val stageWidth = maxWidth
            val density = LocalDensity.current

            val pillWidth = 52.dp
            val gap = 10.dp
            val heroWidth = if (n > 1) (stageWidth - pillWidth - gap).coerceAtLeast(180.dp) else stageWidth
            val stepDistancePx = with(density) { (heroWidth + gap).toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(n) {
                        if (n <= 1) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                coroutineScope.launch {
                                    val snapTarget = currentScroll.value.roundToInt().coerceIn(0, n - 1)
                                    currentScroll.animateTo(
                                        targetValue = snapTarget.toFloat(),
                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f)
                                    )
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                coroutineScope.launch {
                                    val snapTarget = currentScroll.value.roundToInt().coerceIn(0, n - 1)
                                    currentScroll.animateTo(
                                        targetValue = snapTarget.toFloat(),
                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f)
                                    )
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val deltaScroll = dragAmount / stepDistancePx
                                    val newScroll = (currentScroll.value - deltaScroll).coerceIn(-0.15f, (n - 1).toFloat() + 0.15f)
                                    currentScroll.snapTo(newScroll)
                                }
                            }
                        )
                    }
            ) {
                if (n == 1) {
                    // Single item: Fill whole showcase width
                    val (media, progress) = continueWatchingList[0]
                    val targetEpIndex = resolveTargetEpIndex(media, progress)
                    ContinueWatchingShowcaseCard(
                        media = media,
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        onPlay = { onPlayEpisode(media, targetEpIndex) },
                        onRemove = { onRemove(media, progress) },
                        onLongClick = { onLongClick(media to progress) }
                    )
                } else {
                    // Multiple items: Animate sliding hero cards & secondary pill cards
                    val s = scrollVal.coerceIn(0f, (n - 1).toFloat())
                    val k = s.toInt().coerceIn(0, (n - 2).coerceAtLeast(0))
                    val p = (s - k).coerceIn(0f, 1f)

                    continueWatchingList.forEachIndexed { idx, (media, progress) ->
                        val targetEpIndex = resolveTargetEpIndex(media, progress)
                        val isTransitionToLast = k == n - 2

                        val (itemLeft, itemWidth, fullAlpha, pillAlpha, isVisible) = when {
                            isTransitionToLast -> {
                                when (idx) {
                                    k -> {
                                        val w = lerp(heroWidth, pillWidth, p)
                                        val fA = (1f - p * 1.8f).coerceIn(0f, 1f)
                                        val pA = (p * 1.8f - 0.15f).coerceIn(0f, 1f)
                                        Tuple5(0.dp, w, fA, pA, true)
                                    }
                                    k + 1 -> {
                                        val l = lerp(heroWidth + gap, pillWidth + gap, p)
                                        val w = lerp(pillWidth, heroWidth, p)
                                        val fA = (p * 1.6f - 0.2f).coerceIn(0f, 1f)
                                        val pA = (1f - p * 1.6f).coerceIn(0f, 1f)
                                        Tuple5(l, w, fA, pA, true)
                                    }
                                    k - 1 -> Tuple5(-(heroWidth + gap) - 30.dp, heroWidth, 0f, 0f, false)
                                    else -> Tuple5(stageWidth + 40.dp, pillWidth, 0f, 0f, false)
                                }
                            }
                            else -> {
                                when (idx) {
                                    k -> {
                                        val l = -(heroWidth + gap) * p
                                        val fA = (1f - p * 1.5f).coerceIn(0f, 1f)
                                        Tuple5(l, heroWidth, fA, 0f, true)
                                    }
                                    k + 1 -> {
                                        val l = lerp(heroWidth + gap, 0.dp, p)
                                        val w = lerp(pillWidth, heroWidth, p)
                                        val fA = (p * 1.6f - 0.2f).coerceIn(0f, 1f)
                                        val pA = (1f - p * 1.6f).coerceIn(0f, 1f)
                                        Tuple5(l, w, fA, pA, true)
                                    }
                                    k + 2 -> {
                                        val l = lerp(stageWidth + 20.dp, heroWidth + gap, p)
                                        val pA = (p * 1.5f).coerceIn(0f, 1f)
                                        Tuple5(l, pillWidth, 0f, pA, true)
                                    }
                                    else -> Tuple5(-(heroWidth + gap) - 30.dp, heroWidth, 0f, 0f, false)
                                }
                            }
                        }

                        if (isVisible) {
                            Box(
                                modifier = Modifier
                                    .offset(x = itemLeft)
                                    .width(itemWidth)
                                    .height(146.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(SurfaceDark)
                            ) {
                                if (fullAlpha > 0.01f) {
                                    ContinueWatchingShowcaseCard(
                                        media = media,
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(alpha = fullAlpha),
                                        onPlay = { onPlayEpisode(media, targetEpIndex) },
                                        onRemove = { onRemove(media, progress) },
                                        onLongClick = { onLongClick(media to progress) }
                                    )
                                }

                                if (pillAlpha > 0.01f) {
                                    ContinueWatchingPillCard(
                                        media = media,
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(alpha = pillAlpha)
                                            .clickable {
                                                coroutineScope.launch {
                                                    currentScroll.animateTo(
                                                        idx.toFloat(),
                                                        spring(dampingRatio = 0.85f, stiffness = 380f)
                                                    )
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3. BOTTOM PROGRESS INDICATOR (Item count on left, dots on right) ──
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${activeIndex + 1} of $n in progress",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )

            if (n > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(n) { i ->
                        val isCurrent = i == activeIndex
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isCurrent) 20.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) primaryColor else Color.White.copy(alpha = 0.2f))
                                .clickable {
                                    coroutineScope.launch {
                                        currentScroll.animateTo(
                                            i.toFloat(),
                                            spring(dampingRatio = 0.85f, stiffness = 380f)
                                        )
                                    }
                                }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Full expanded card showing thumbnail, title, remaining time, and resume button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueWatchingShowcaseCard(
    media: MediaItem,
    progress: PlaybackProgress,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val progressFraction = if (progress.durationMs > 0) {
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val remainingMs = (progress.durationMs - progress.positionMs).coerceAtLeast(0L)
    val remainingMinutes = remainingMs / 60_000L

    val isMovie = media.category.equals("Movie", ignoreCase = true) ||
            media.category.equals("Movies", ignoreCase = true) ||
            progress.mediaType.equals("Movie", ignoreCase = true) ||
            progress.mediaType.equals("Movies", ignoreCase = true)

    val currentEp = if (progress.episodeTitle.isNotBlank()) {
        media.episodes.firstOrNull { it.title.equals(progress.episodeTitle, ignoreCase = true) }
            ?: media.episodes.getOrNull(progress.episodeNumber)
    } else {
        media.episodes.getOrNull(progress.episodeNumber)
    }

    val subtitleText = if (isMovie) {
        if (media.releaseYear.isNotBlank()) "Movie • ${media.releaseYear}" else "Movie"
    } else {
        val seasonNum = if (progress.seasonNumber > 0) progress.seasonNumber else (currentEp?.seasonNumber ?: 1)
        val epNum = currentEp?.episodeNumber?.takeIf { it > 0 } ?: (progress.episodeNumber + 1)
        if (currentEp != null && currentEp.title.isNotBlank() && !currentEp.title.equals(media.title, ignoreCase = true)) {
            "S$seasonNum:E$epNum • ${currentEp.title}"
        } else {
            "S$seasonNum:E$epNum"
        }
    }

    val thumbModel = currentEp?.thumbnailUrl?.ifEmpty { media.bannerUrl.ifEmpty { media.posterUrl } } ?: media.posterUrl

    Row(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        // Thumbnail with bottom progress bar
        Box(
            modifier = Modifier
                .width(133.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                .background(SurfaceDark)
        ) {
            AsyncImage(
                model = thumbModel,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Vignette gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x33000000), Color.Transparent, Color(0x66000000))
                        )
                    )
            )

            // 4dp Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF22222E))
            ) {
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction.coerceIn(0.04f, 1f))
                            .background(primaryColor)
                    )
                }
            }
        }

        // Details column: Time left, close button, title, and resume icon
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Row 1: Time Left + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (remainingMinutes > 0) "${remainingMinutes}m left" else "In progress",
                    color = primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClick = onRemove,
                            onClickLabel = "Remove ${media.title} from history"
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove from history",
                        tint = Color(0xFF8E8E98),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Row 2: Title + Subtitle
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = media.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitleText,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 3: Resume button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FigmaPlayOutlineIcon(
                    tint = primaryColor,
                    modifier = Modifier.size(15.dp)
                )

                Text(
                    text = "Resume ${if (isMovie) "movie" else "episode"}",
                    color = primaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

/**
 * Compact pill-shaped preview card shown when swiping between items.
 */
@Composable
private fun ContinueWatchingPillCard(
    media: MediaItem,
    progress: PlaybackProgress,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val currentEp = if (progress.episodeTitle.isNotBlank()) {
        media.episodes.firstOrNull { it.title.equals(progress.episodeTitle, ignoreCase = true) }
            ?: media.episodes.getOrNull(progress.episodeNumber)
    } else {
        media.episodes.getOrNull(progress.episodeNumber)
    }
    val thumbModel = currentEp?.thumbnailUrl?.ifEmpty { media.bannerUrl.ifEmpty { media.posterUrl } } ?: media.posterUrl

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = thumbModel,
            contentDescription = media.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.55f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.18f)
                    lineTo(size.width * 0.25f, size.height * 0.82f)
                    lineTo(size.width * 0.88f, size.height * 0.50f)
                    close()
                }
                drawPath(path, color = Color.White)
            }
        }
    }
}

/**
 * Custom vector play outline icon.
 */
@Composable
private fun FigmaPlayOutlineIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sx = size.width / 16f
        val sy = size.height / 16f

        val path = Path().apply {
            moveTo(3.125f * sx, 3.625f * sy)
            lineTo(12.504f * sx, 6.919f * sy)
            cubicTo(
                12.96f * sx, 7.376f * sy,
                13.126f * sx, 7.80f * sy,
                13.126f * sx, 8.00f * sy
            )
            cubicTo(
                13.126f * sx, 8.22f * sy,
                12.96f * sx, 8.622f * sy,
                12.506f * sx, 9.080f * sy
            )
            lineTo(5.005f * sx, 13.455f * sy)
            cubicTo(
                4.379f * sx, 13.625f * sy,
                3.752f * sx, 13.459f * sy,
                3.293f * sx, 13.001f * sy
            )
            cubicTo(
                3.124f * sx, 12.811f * sy,
                3.124f * sx, 12.595f * sy,
                3.124f * sx, 12.375f * sy
            )
            close()
        }

        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.6.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

private fun resolveTargetEpIndex(media: MediaItem, progress: PlaybackProgress): Int {
    return if (progress.episodeTitle.isNotBlank()) {
        val idx = media.episodes.indexOfFirst { it.title.equals(progress.episodeTitle, ignoreCase = true) }
        if (idx >= 0) idx else progress.episodeNumber
    } else {
        progress.episodeNumber
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)
