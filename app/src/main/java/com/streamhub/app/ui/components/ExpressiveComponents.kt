package com.streamhub.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.navigation.Screen
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyClickable
import com.streamhub.app.ui.theme.bouncyTouch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive UI Suite:
 * 1. PlayStoreDownloadButton: Animated circular progress with centered cancel button (✕) and checkmark morph.
 * 2. ExpressiveLoadingIndicator: Organic multi-petal shape-morphing rotating spinner.
 * 3. ExpressiveFloatingNavBar: Floating capsule dock bottom navigation with glassmorphism and spring physics.
 * 4. ExpressiveFabMenu: Expandable staggered action pill menu with rotation morph from + to ✕.
 * 5. ExpressiveSplitButton: Connected tonal pill buttons with tactile feedback.
 */

// =========================================================================
// 1. Google Play Store-Style Circular Download Progress Button
// =========================================================================

/**
 * Google Play Store-style circular download button:
 * - Idle: Circular down-arrow icon with subtle border.
 * - Queued: Pulsing/spinning ring with centered ✕ to cancel.
 * - In Progress: Animated progress ring sweep with centered ✕ to cancel on tap.
 * - Completed: Emerald filled badge with checkmark (✓).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayStoreDownloadButton(
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isQueued: Boolean,
    progressPercent: Int,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    onHold: (() -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val haptics = LocalHapticFeedback.current

    val handleHold: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (onHold != null) {
            onHold()
        } else {
            val label = when {
                isDownloaded -> "Downloaded & Ready Offline 📥"
                isDownloading -> "Downloading ($progressPercent%) • Tap ✕ to cancel ⚡"
                isQueued -> "Queued in download engine • Tap ✕ to cancel ⏳"
                else -> "Download episode for offline playback 💾"
            }
            ToastManager.showToast(label)
        }
    }

    // Animated progress angle
    val animatedProgress by animateFloatAsState(
        targetValue = (progressPercent.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "download_progress_sweep"
    )

    // Indeterminate rotation for queued or zero-progress downloads
    val infiniteTransition = rememberInfiniteTransition(label = "queue_rotation")
    val queueRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "queue_spinner"
    )

    Box(
        modifier = modifier
            .size(size)
            .bouncyTouch(pressedScale = 0.90f),
        contentAlignment = Alignment.Center
    ) {
        when {
            // State: Completed Download
            isDownloaded -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color(0x2610B981))
                        .border(1.5.dp, Color(0xFF10B981), CircleShape)
                        .combinedClickable(
                            onClick = { onDownload() },
                            onLongClick = handleHold
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Downloaded & Ready Offline",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            // State: Actively Downloading with Play Store Progress Ring + Centered [✕]
            isDownloading -> {
                val strokeWidthDp = 3.2.dp
                val isPending = progressPercent <= 0
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = { onCancel() },
                            onLongClick = handleHold
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Circular Progress Canvas
                    Canvas(modifier = Modifier.fillMaxSize().padding(2.5.dp)) {
                        val strokeWidthPx = strokeWidthDp.toPx()
                        val canvasWidth = this@Canvas.size.width
                        val canvasHeight = this@Canvas.size.height
                        val arcSize = Size(canvasWidth - strokeWidthPx, canvasHeight - strokeWidthPx)
                        val topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

                        // 1. Inactive Track (Google Play style subtle grey ring)
                        drawArc(
                            color = Color(0x33FFFFFF),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidthPx)
                        )

                        // 2. Active Progress Sweep
                        if (isPending) {
                            // Rotating indeterminate arc when pending / starting
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(primaryColor, AccentOrange, primaryColor)
                                ),
                                startAngle = queueRotation,
                                sweepAngle = 120f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )
                        } else {
                            val sweep = (animatedProgress * 360f).coerceIn(8f, 360f)
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(primaryColor, AccentOrange, primaryColor)
                                ),
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Centered Cancel [✕] Icon inside tinted circle
                    Box(
                        modifier = Modifier
                            .size(size * 0.52f)
                            .clip(CircleShape)
                            .background(Color(0x33FF1744)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Download",
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.36f)
                        )
                    }
                }
            }

            // State: Queued in Sequential Download Engine with Rotating Ring + Centered [✕]
            isQueued -> {
                val strokeWidthDp = 3.0.dp
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = { onCancel() },
                            onLongClick = handleHold
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.5.dp)
                            .graphicsLayer { rotationZ = queueRotation }
                    ) {
                        val strokeWidthPx = strokeWidthDp.toPx()
                        val canvasWidth = this@Canvas.size.width
                        val canvasHeight = this@Canvas.size.height
                        val arcSize = Size(canvasWidth - strokeWidthPx, canvasHeight - strokeWidthPx)
                        val topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

                        // Segmented rotating arc
                        drawArc(
                            color = Color(0xFFB388FF),
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(size * 0.52f)
                            .clip(CircleShape)
                            .background(Color(0x33B388FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove from Download Queue",
                            tint = Color(0xFFB388FF),
                            modifier = Modifier.size(size * 0.36f)
                        )
                    }
                }
            }

            // State: Idle (Not Downloaded)
            else -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .combinedClickable(
                            onClick = { onDownload() },
                            onLongClick = handleHold
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Episode",
                        tint = TextPrimary,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 2. Material 3 Expressive Morphing Loading Indicator
// =========================================================================

/**
 * Google Material 3 Expressive organic multi-petal shape-morphing rotating spinner.
 * Replaces boring indeterminate circles with a fluid living shape that pulses and rotates.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = AccentOrange,
    size: Dp = 36.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_expressive_loader")

    // Continuous 360 rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Morph oscillation (cycles shape lobes from flower to star)
    val morphPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morphPhase"
    )

    // Breathing pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = pulseScale
                scaleY = pulseScale
            }
    ) {
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f
        val baseRadius = (this.size.minDimension / 2f) * 0.72f
        val lobeCount = 6 // 6-petal organic morphing flower

        val path = Path()
        val steps = 90
        for (i in 0 until steps) {
            val angle = (i.toFloat() / steps) * (2 * PI).toFloat()
            // Harmonic wave for petal morphology: r(theta) = R * (1 + A * sin(k*theta + phase))
            val wave = 0.24f * sin(lobeCount * angle + morphPhase)
            val r = baseRadius * (1f + wave)
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        // Outer morphing gradient ribbon
        drawPath(
            path = path,
            brush = Brush.sweepGradient(
                listOf(color, accentColor, color)
            ),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Center pulsating organic core
        drawCircle(
            color = color.copy(alpha = 0.35f),
            radius = baseRadius * 0.35f
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = baseRadius * 0.16f
        )
    }
}

// =========================================================================
// 3. Material 3 Expressive Floating Capsule Dock Navigation Bar
// =========================================================================

/**
 * Material 3 Expressive Floating Navigation Dock:
 * Clean, icon-only capsule dock floating above content.
 * When any button is held (long-pressed), an M3 Expressive popup pill springs in
 * above the dock revealing the screen name with haptic feedback.
 */
@Composable
fun ExpressiveFloatingNavBar(
    screens: List<Screen>,
    currentRoute: String?,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var activeHoldLabel by remember { mutableStateOf<String?>(null) }
    var activeHoldIcon by remember { mutableStateOf<ImageVector?>(null) }
    var dismissJob by remember { mutableStateOf<Job?>(null) }

    fun triggerHold(screen: Screen) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        activeHoldLabel = screen.title
        activeHoldIcon = screen.icon
        dismissJob?.cancel()
        dismissJob = coroutineScope.launch {
            delay(1800)
            activeHoldLabel = null
            activeHoldIcon = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // M3 Expressive Hold Popup Pill
            AnimatedVisibility(
                visible = activeHoldLabel != null,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { it } +
                        scaleIn(initialScale = 0.82f),
                exit = fadeOut(tween(140)) +
                        slideOutVertically { it / 2 } +
                        scaleOut(targetScale = 0.88f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 8.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        activeHoldIcon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                        }
                        Text(
                            text = activeHoldLabel.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Floating Capsule Dock Container (Icon-Only, M3 Expressive Split Button Segmented Design)
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 14.dp,
                tonalElevation = 6.dp,
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    screens.forEachIndexed { index, screen ->
                        val isSelected = currentRoute == screen.route
                        val itemShape = when {
                            screens.size <= 1 -> CircleShape
                            index == 0 -> RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                            index == screens.lastIndex -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 22.dp, bottomEnd = 22.dp)
                            else -> RoundedCornerShape(6.dp)
                        }
                        ExpressiveNavDockIconItem(
                            screen = screen,
                            isSelected = isSelected,
                            shape = itemShape,
                            onClick = {
                                activeHoldLabel = null
                                onScreenSelected(screen)
                            },
                            onHold = {
                                triggerHold(screen)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpressiveNavDockIconItem(
    screen: Screen,
    isSelected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    onHold: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_item_scale"
    )

    val pillBgColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(pillBgColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onHold
            ),
        contentAlignment = Alignment.Center
    ) {
        screen.icon?.let { iconVec ->
            Icon(
                imageVector = iconVec,
                contentDescription = screen.title,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// =========================================================================
// Expressive Category Icon Chip & Reusable Hold Popup
// =========================================================================

/**
 * Reusable M3 Expressive Floating Hold Popup Pill.
 * Pops up with bouncy spring physics when any icon button is held.
 */
@Composable
fun ExpressiveHoldPopup(
    visible: Boolean,
    label: String?,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && !label.isNullOrBlank(),
        enter = fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { -it / 2 } +
                scaleIn(initialScale = 0.82f),
        exit = fadeOut(tween(140)) +
                slideOutVertically { -it / 2 } +
                scaleOut(targetScale = 0.88f),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 8.dp,
            tonalElevation = 6.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                }
                Text(
                    text = label.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

/**
 * M3 Expressive Icon-First Category Filter Pill:
 * Sleek icon button with spring compression and hold popup.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveCategoryChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isGradient: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else if (isSelected) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cat_chip_scale"
    )

    val backgroundBrush = if (isGradient) {
        Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFF8B5CF6)))
    } else null

    val containerColor = when {
        isGradient -> Color.Transparent
        isSelected -> accentColor
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when {
        isGradient -> Color.White
        isSelected -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = CircleShape,
        color = containerColor,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        tonalElevation = if (isSelected) 4.dp else 2.dp,
        modifier = modifier
            .height(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (backgroundBrush != null) Modifier.background(backgroundBrush, CircleShape)
                else Modifier
            )
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onHold
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * M3 Expressive Split Category Icon Button:
 * Icon-only button with M3 Expressive segmented geometry, spring compression,
 * and tactile hold popup support.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveSplitCategoryIconItem(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isGradient: Boolean = false,
    width: Dp = 48.dp,
    height: Dp = 40.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "split_cat_scale"
    )

    val backgroundBrush = if (isGradient) {
        Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFF8B5CF6)))
    } else null

    val containerColor = when {
        isGradient -> Color.Transparent
        isSelected -> accentColor
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when {
        isGradient || isSelected -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = shape,
        color = containerColor,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        tonalElevation = if (isSelected) 4.dp else 2.dp,
        modifier = modifier
            .size(width = width, height = height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (backgroundBrush != null) Modifier.background(backgroundBrush, shape)
                else Modifier
            )
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onHold
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Official Material 3 Expressive Icon-Only Split Button for Sorting:
 * [ Sort Direction Icon Button (4.dp inner) ] <3.dp gap> [ Menu Chevron Button (4.dp inner, 20.dp outer) ]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveSortSplitIconButton(
    onDirectionClick: () -> Unit,
    onMenuClick: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isMenuOpen: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 40.dp
) {
    val haptic = LocalHapticFeedback.current

    val chevronRotation by animateFloatAsState(
        targetValue = if (isMenuOpen) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "split_sort_icon_chevron_rotation"
    )

    val containerColor = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(height)
    ) {
        // Leading primary sort button (left = 4.dp, right = 4.dp)
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = containerColor,
            shadowElevation = if (isSelected) 4.dp else 1.dp,
            tonalElevation = if (isSelected) 4.dp else 2.dp,
            modifier = Modifier
                .size(width = 42.dp, height = height)
                .bouncyTouch(pressedScale = 0.90f)
                .clip(RoundedCornerShape(4.dp))
                .combinedClickable(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDirectionClick()
                    },
                    onLongClick = onHold
                )
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort Direction",
                    tint = contentColor,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // M3 Expressive Split Gap (3.dp)
        Spacer(modifier = Modifier.width(3.dp))

        // Trailing menu chevron button (left = 4.dp, right = 20.dp)
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp),
            color = containerColor,
            shadowElevation = if (isSelected) 4.dp else 1.dp,
            tonalElevation = if (isSelected) 4.dp else 2.dp,
            modifier = Modifier
                .size(width = 36.dp, height = height)
                .bouncyTouch(pressedScale = 0.90f)
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMenuClick()
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Sort Menu",
                    tint = contentColor,
                    modifier = Modifier
                        .size(19.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }
        }
    }
}

// =========================================================================
// 4. Material 3 Expressive Expandable Floating Action Pill Menu (FAB)
// =========================================================================

/**
 * Expandable FAB Menu:
 * Pill container with animated morphing icon from '+' to '✕' with spring physics.
 * Expands into staggered rounded action pills: [Add Media], [Surprise Me], [Admin Studio].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveFabMenu(
    onAddMedia: () -> Unit,
    onSurpriseMe: () -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    var fabHoldLabel by remember { mutableStateOf<String?>(null) }
    var dismissJob by remember { mutableStateOf<Job?>(null) }

    // Icon rotation animation from + to ✕ (135 degrees)
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "fab_rotation"
    )

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
    ) {
        // Hold Popup Pill for FAB
        ExpressiveHoldPopup(
            visible = fabHoldLabel != null,
            label = fabHoldLabel,
            icon = Icons.Default.AdminPanelSettings,
            accentColor = AccentGold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Expanded Staggered Pill Actions
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(180)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ),
            exit = fadeOut(tween(140)) + slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = tween(140)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                // Action 1: Add New Media
                FabActionPill(
                    icon = Icons.Default.Add,
                    label = "Add New Media",
                    accent = PrimaryRed,
                    onClick = {
                        isExpanded = false
                        onAddMedia()
                    }
                )

                // Action 2: Surprise Me / Random
                FabActionPill(
                    icon = Icons.Default.Shuffle,
                    label = "Surprise Me 🎲",
                    accent = AccentOrange,
                    onClick = {
                        isExpanded = false
                        onSurpriseMe()
                    }
                )

                // Action 3: Admin Studio
                FabActionPill(
                    icon = Icons.Default.AdminPanelSettings,
                    label = "Admin Studio ⚙️",
                    accent = AccentGold,
                    onClick = {
                        isExpanded = false
                        onOpenAdmin()
                    }
                )
            }
        }

        // Main Expressive FAB Pill Button
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, Color(0x44FFFFFF)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .size(56.dp)
                .bouncyTouch(pressedScale = 0.88f)
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = { isExpanded = !isExpanded },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        fabHoldLabel = if (isExpanded) "Close Studio Menu" else "Studio & Quick Actions ⚙️"
                        dismissJob?.cancel()
                        dismissJob = coroutineScope.launch {
                            delay(1800)
                            fabHoldLabel = null
                        }
                    }
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isExpanded) "Close Menu" else "Open Studio Menu",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { rotationZ = rotationAngle }
                )
            }
        }
    }
}

@Composable
private fun FabActionPill(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xEE1E1E2C),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .bouncyTouch(pressedScale = 0.92f)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =========================================================================
// 5. Material 3 Expressive Split Button
// =========================================================================

/**
 * Official Material 3 Expressive Split Button:
 * Reference: https://m3.material.io/components/split-button/overview
 *
 * Anatomy:
 * [ Leading Action Button (22.dp outer, 4.dp inner) ] <3.dp gap> [ Trailing Action/Menu (4.dp inner, 22.dp outer) ]
 */
@Composable
fun ExpressiveSplitButton(
    primaryText: String,
    primaryIcon: ImageVector,
    onPrimaryClick: () -> Unit,
    secondaryIcon: ImageVector,
    secondaryContentDescription: String,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = PrimaryRed
) {
    val haptic = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(44.dp)
    ) {
        // Leading primary action button (Outer Left = 22.dp, Inner Right = 4.dp)
        Surface(
            shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 4.dp, bottomEnd = 4.dp),
            color = primaryColor,
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .bouncyTouch(pressedScale = 0.95f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPrimaryClick()
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = primaryIcon,
                    contentDescription = primaryText,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = primaryText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // M3 Expressive Split Gap (3.dp)
        Spacer(modifier = Modifier.width(3.dp))

        // Trailing secondary menu/action button (Inner Left = 4.dp, Outer Right = 22.dp)
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp),
            color = primaryColor,
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(width = 44.dp, height = 44.dp)
                .bouncyTouch(pressedScale = 0.92f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSecondaryClick()
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = secondaryIcon,
                    contentDescription = secondaryContentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Material 3 Expressive Dual Selector Split Button (Season & Arc):
 * Reference: https://m3.material.io/components/split-button/overview
 * [ Season Button (16.dp, 4.dp) ] <3.dp gap> [ Arc Button (4.dp, 16.dp) ]
 */
@Composable
fun ExpressiveDualSelectorSplitButton(
    leftText: String,
    leftIcon: ImageVector = Icons.Default.Layers,
    onLeftClick: () -> Unit,
    rightText: String,
    rightIcon: ImageVector = Icons.Default.AutoStories,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier,
    leftAccent: Color = AccentOrange,
    rightAccent: Color = Color(0xFFB388FF),
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp)
) {
    val haptic = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(34.dp)
    ) {
        // Left Button (Season): Asymmetric Pill Left (18.dp, 4.dp)
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 4.dp, bottomEnd = 4.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, CardBorderDark),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxHeight()
                .bouncyTouch(pressedScale = 0.94f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLeftClick()
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Icon(
                    imageVector = leftIcon,
                    contentDescription = null,
                    tint = leftAccent,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = leftText,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = leftAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // M3 Expressive Split Gap (3.dp)
        Spacer(modifier = Modifier.width(3.dp))

        // Right Button (Arc): Asymmetric Pill Right (4.dp, 18.dp)
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, CardBorderDark),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxHeight()
                .bouncyTouch(pressedScale = 0.94f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRightClick()
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Icon(
                    imageVector = rightIcon,
                    contentDescription = null,
                    tint = rightAccent,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = rightText,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = rightAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Material 3 Expressive Squircle Studio / Creator Button.
 * Replaces the multi-expansion FAB with a dedicated, tactile Creator Studio button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveStudioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var holdLabel by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
    ) {
        ExpressiveHoldPopup(
            visible = holdLabel != null,
            label = holdLabel,
            icon = Icons.Default.Add,
            accentColor = AccentGold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, Color(0x55FFFFFF)),
            shadowElevation = 10.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .size(54.dp)
                .bouncyTouch(pressedScale = 0.88f)
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        holdLabel = "Add Media & Creator Studio 🎬"
                        job?.cancel()
                        job = scope.launch {
                            delay(1800)
                            holdLabel = null
                        }
                    }
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Media & Creator Studio",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Official Material 3 Expressive Split Button for Sorting:
 * Reference: https://m3.material.io/components/split-button/overview
 *
 * Visual anatomy:
 * [ Leading Action Button (20.dp left, 4.dp right) ] <3.dp gap> [ Dropdown Chevron (4.dp left, 20.dp right) ]
 *
 * - The split button has a separate menu button that spins and changes shape when activated.
 * - Primary Action: Toggles sort direction (High to Low / Low to High) with haptic feedback.
 * - Trailing Action: Opens the sort criteria dropdown menu with animated rotating chevron.
 */
@Composable
fun ExpressiveSortSplitButton(
    text: String,
    onDirectionClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isMenuOpen: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val haptic = LocalHapticFeedback.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (isMenuOpen) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "split_btn_chevron"
    )

    val trailingCornerRadius by animateDpAsState(
        targetValue = if (isMenuOpen) 20.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "split_btn_trailing_shape"
    )

    val containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
    val height = 38.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.wrapContentWidth()
    ) {
        // 1. Leading Action Button (Outer Left = 20.dp, Inner Right = 4.dp)
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                bottomStart = 20.dp,
                topEnd = 4.dp,
                bottomEnd = 4.dp
            ),
            color = containerColor,
            shadowElevation = 2.dp,
            tonalElevation = 2.dp,
            modifier = Modifier
                .height(height)
                .bouncyTouch(pressedScale = 0.93f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDirectionClick()
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Toggle Sort Direction",
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // 2. Official M3 Expressive Split Gap (3.dp)
        Spacer(modifier = Modifier.width(3.dp))

        // 3. Trailing Menu Button (Inner Left = 4.dp / morphs, Outer Right = 20.dp)
        Surface(
            shape = RoundedCornerShape(
                topStart = trailingCornerRadius,
                bottomStart = trailingCornerRadius,
                topEnd = 20.dp,
                bottomEnd = 20.dp
            ),
            color = containerColor,
            shadowElevation = 2.dp,
            tonalElevation = 2.dp,
            modifier = Modifier
                .size(width = 34.dp, height = height)
                .bouncyTouch(pressedScale = 0.90f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMenuClick()
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Sort Criterion",
                    tint = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }
        }
    }
}

