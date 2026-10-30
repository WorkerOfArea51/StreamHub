package com.streamhub.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.R
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.theme.bouncyTouch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Material 3 Expressive (M3E) Hero Carousel
 *
 * Full-width, generous 310dp height stage eliminating dark nested borders to give
 * maximum horizontal breathing room and crystal-clear, unclouded artwork visibility.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    featuredItems: List<MediaItem>,
    onPlayClick: (MediaItem) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (featuredItems.isEmpty()) return

    val n = featuredItems.size
    val coroutineScope = rememberCoroutineScope()
    val myListSet by MyListManager.myListFlow.collectAsState()

    val currentScroll = remember { Animatable(0f) }
    var targetIndex by remember { mutableIntStateOf(0) }
    val isDragging = remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }
    var selectedMediaForFolder by remember { mutableStateOf<MediaItem?>(null) }

    // Rhythmic Blinking Beacon Dot for "IN THE SPOTLIGHT"
    val beaconTransition = rememberInfiniteTransition(label = "spotlightBeacon")
    val beaconAlpha by beaconTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 625, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )
    val beaconScale by beaconTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 625, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconScale"
    )

    // Smooth Auto-Advance every 5 seconds (pauses 8s on user interaction)
    LaunchedEffect(n) {
        while (isActive) {
            delay(5000L)
            val timeSinceInteraction = System.currentTimeMillis() - lastInteractionTime
            if (!isDragging.value && timeSinceInteraction >= 8000L && n > 1) {
                val nextIndex = targetIndex + 1
                targetIndex = nextIndex
                currentScroll.animateTo(
                    targetValue = nextIndex.toFloat(),
                    animationSpec = tween(
                        durationMillis = 720,
                        easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
                    )
                )
            }
        }
    }

    val scrollVal = currentScroll.value
    val activeIndex = ((scrollVal.roundToInt() % n) + n) % n
    val activeMedia = featuredItems[activeIndex]
    val isBookmarked = myListSet.contains(activeMedia.id)

    // Clean, borderless container maximizing horizontal width across screen
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // ── TOP PORTION: Full-Breathing 350dp M3E Dynamic Shape-Morphing Stage ──
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            val stageWidth = maxWidth
            val density = LocalDensity.current

            val gap = 10.dp
            val pillWidth = 56.dp
            val heroWidth = if (n > 1) (stageWidth - pillWidth - gap).coerceAtLeast(200.dp) else stageWidth
            val stepDistance = heroWidth + gap
            val stepDistancePx = with(density) { stepDistance.toPx() }

            val heroRadius = 24.dp
            val pillRadius = 28.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(n) {
                        if (n <= 1) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDragging.value = true
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onDragEnd = {
                                isDragging.value = false
                                coroutineScope.launch {
                                    val snapTarget = currentScroll.value.roundToInt()
                                    targetIndex = snapTarget
                                    currentScroll.animateTo(
                                        targetValue = snapTarget.toFloat(),
                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f)
                                    )
                                }
                            },
                            onDragCancel = {
                                isDragging.value = false
                                coroutineScope.launch {
                                    val snapTarget = currentScroll.value.roundToInt()
                                    targetIndex = snapTarget
                                    currentScroll.animateTo(
                                        targetValue = snapTarget.toFloat(),
                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f)
                                    )
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                lastInteractionTime = System.currentTimeMillis()
                                coroutineScope.launch {
                                    val deltaScroll = dragAmount / stepDistancePx
                                    currentScroll.snapTo(currentScroll.value - deltaScroll)
                                }
                            }
                        )
                    }
            ) {
                if (n == 1) {
                    val item = featuredItems[0]
                    val imageModel = item.posterUrl.ifEmpty { item.bannerUrl }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(heroRadius))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { onMediaClick(item) }
                    ) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Floating Spotlight Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B0B12))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .graphicsLayer(scaleX = beaconScale, scaleY = beaconScale, alpha = beaconAlpha)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "IN THE SPOTLIGHT",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                } else {
                    val normalizedScroll = ((scrollVal % n) + n) % n
                    featuredItems.forEachIndexed { idx, item ->
                        var delta = (idx - normalizedScroll).toFloat()
                        while (delta > n / 2f) delta -= n
                        while (delta < -n / 2f) delta += n

                        if (delta <= -1.4f || delta >= 2.4f) {
                            return@forEachIndexed
                        }

                        val (itemWidth, itemLeft, itemRadius, itemAlpha, parallaxOffset, badgeAlpha) = when {
                            delta < 0f -> {
                                val p = (-delta).coerceIn(0f, 1f)
                                val w = lerp(heroWidth, pillWidth, p)
                                val l = -(pillWidth + gap) * p
                                val r = lerp(heroRadius, pillRadius, p)
                                val a = (1f - p * 0.7f).coerceIn(0f, 1f)
                                val plx = 12.dp * p
                                val bAlpha = (1f - p * 2.5f).coerceIn(0f, 1f)
                                Tuple6(w, l, r, a, plx, bAlpha)
                            }
                            delta < 1f -> {
                                val p = delta.coerceIn(0f, 1f)
                                val w = lerp(heroWidth, pillWidth, p)
                                val l = (heroWidth + gap) * p
                                val r = lerp(heroRadius, pillRadius, p)
                                val a = 1f
                                val plx = 12.dp * (1f - p)
                                val bAlpha = (1f - p * 2.5f).coerceIn(0f, 1f)
                                Tuple6(w, l, r, a, plx, bAlpha)
                            }
                            else -> {
                                val p = (delta - 1f).coerceIn(0f, 1f)
                                val w = pillWidth
                                val l = (heroWidth + gap) + (stageWidth - heroWidth) * p
                                val r = pillRadius
                                val a = (1f - p * 0.8f).coerceIn(0f, 1f)
                                val plx = -10.dp * p
                                Tuple6(w, l, r, a, plx, 0f)
                            }
                        }

                        // Rich poster image for both hero stage and upcoming capsule pill
                        val imageModel = item.posterUrl.ifEmpty { item.bannerUrl }

                        Box(
                            modifier = Modifier
                                .offset(x = itemLeft)
                                .width(itemWidth)
                                .fillMaxHeight()
                                .graphicsLayer(alpha = itemAlpha)
                                .clip(RoundedCornerShape(itemRadius))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable {
                                    if (delta.absoluteValue < 0.35f) {
                                        onMediaClick(item)
                                    } else if (delta > 0.4f) {
                                        coroutineScope.launch {
                                            lastInteractionTime = System.currentTimeMillis()
                                            val nextTarget = (scrollVal + delta).roundToInt()
                                            currentScroll.animateTo(
                                                targetValue = nextTarget.toFloat(),
                                                animationSpec = tween(
                                                    durationMillis = 720,
                                                    easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
                                                )
                                            )
                                        }
                                    }
                                }
                        ) {
                            // Crystal-clear artwork cropped natively to capsule with zero murky overlays
                            AsyncImage(
                                model = imageModel,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Spotlight Badge on Hero with Blinking Beacon
                            if (badgeAlpha > 0.02f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp)
                                        .graphicsLayer(alpha = badgeAlpha)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0B0B12))
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .graphicsLayer(scaleX = beaconScale, scaleY = beaconScale, alpha = beaconAlpha)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            text = "IN THE SPOTLIGHT",
                                            color = Color.White,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── BOTTOM CONTENT AREA: METADATA, TITLE, SYNOPSIS & EXACT FIGMA BUTTONS ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            // Metadata & Liquid Indicator Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = activeMedia.category.uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    if (activeMedia.releaseYear.isNotBlank()) {
                        Text("•", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                        Text(
                            text = activeMedia.releaseYear,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (activeMedia.rating.isNotBlank()) {
                        Text("•", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFF9900),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = activeMedia.rating,
                                color = Color(0xFFFF9900),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Dynamic Liquid Indicator Dots
                if (n > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(n.coerceAtMost(6)) { dotIndex ->
                            val isActiveDot = dotIndex == activeIndex
                            val dotWidth by animateDpAsState(
                                targetValue = if (isActiveDot) 16.dp else 4.dp,
                                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                                label = "dotWidth"
                            )
                            val dotColor by animateColorAsState(
                                targetValue = if (isActiveDot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                animationSpec = tween(200),
                                label = "dotColor"
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = dotWidth, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Headline Title (Comfortable 2-line wrapping matching Figma)
            Text(
                text = activeMedia.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 26.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Synopsis
            if (activeMedia.description.isNotBlank()) {
                Text(
                    text = activeMedia.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Asymmetric M3E Squircle Action Buttons (22dp 22dp 22dp 10dp, height 52dp)
            val buttonShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 10.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Watch now Button (StreamHub Red + White Outlined Play Icon & White Text)
                Surface(
                    onClick = {
                        lastInteractionTime = System.currentTimeMillis()
                        onPlayClick(activeMedia)
                    },
                    shape = buttonShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .bouncyTouch()
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_figma_watch_play),
                            contentDescription = "Watch now",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Watch now",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                // My list Button (Black Matte SurfaceContainer + Matching 22-22-22-10 Asymmetric Shape)
                Surface(
                    shape = buttonShape,
                    color = if (isBookmarked) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        1.dp,
                        if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .bouncyTouch()
                        .clip(buttonShape)
                        .combinedClickable(
                            onClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                val added = MyListManager.toggleBookmark(activeMedia.id)
                                val msg = if (added) "Added to Favorites • Hold to choose folder" else "Removed from My List"
                                ToastManager.showToast(msg)
                            },
                            onLongClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                selectedMediaForFolder = activeMedia
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = "My list",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBookmarked) "In My list" else "My list",
                            color = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }

    // Long-Press Folder Selection Sheet Dialog
    selectedMediaForFolder?.let { targetMedia ->
        FolderSelectionDialog(
            mediaItem = targetMedia,
            onDismiss = { selectedMediaForFolder = null }
        )
    }
}

/**
 * Lightweight helper to bundle carousel interpolation parameters
 */
private data class Tuple6<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)
