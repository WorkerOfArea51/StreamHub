package com.streamhub.app.ui.screens.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Concave oval shape for right-side double tap seek overlay (matching mpvEx / YouTube).
 */
object RightSideOvalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width, size.height)
            lineTo(size.width, 0f)
            lineTo(size.width / 10, 0f)
            cubicTo(
                size.width / 10,
                0f,
                -30f,
                size.height / 2,
                size.width / 10,
                size.height
            )
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Concave oval shape for left-side double tap seek overlay (matching mpvEx / YouTube).
 */
object LeftSideOvalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, size.height)
            lineTo(size.width - size.width / 10, size.height)
            cubicTo(
                size.width - size.width / 10,
                size.height,
                size.width,
                size.height / 2,
                size.width - size.width / 10,
                0f
            )
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Animated triple chevrons for double tap seeking with staggered travelling wave.
 */
@Composable
fun DoubleTapSeekChevrons(
    isForward: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha1 = remember { Animatable(0.2f) }
    val alpha2 = remember { Animatable(0.2f) }
    val alpha3 = remember { Animatable(0.2f) }
    val scale1 = remember { Animatable(0.85f) }
    val scale2 = remember { Animatable(0.85f) }
    val scale3 = remember { Animatable(0.85f) }

    LaunchedEffect(isForward) {
        while (true) {
            // Wave step 1: First chevron pops
            launch {
                alpha1.animateTo(1f, tween(110))
                scale1.animateTo(1.15f, tween(110))
                alpha1.animateTo(0.3f, tween(180))
                scale1.animateTo(0.9f, tween(180))
            }
            delay(70)

            // Wave step 2: Second chevron pops
            launch {
                alpha2.animateTo(1f, tween(110))
                scale2.animateTo(1.15f, tween(110))
                alpha2.animateTo(0.3f, tween(180))
                scale2.animateTo(0.9f, tween(180))
            }
            delay(70)

            // Wave step 3: Third chevron pops
            launch {
                alpha3.animateTo(1f, tween(110))
                scale3.animateTo(1.15f, tween(110))
                alpha3.animateTo(0.3f, tween(180))
                scale3.animateTo(0.9f, tween(180))
            }
            delay(200)
        }
    }

    val rotation = if (isForward) 0f else 180f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-6).dp),
        modifier = modifier.rotate(rotation)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale1.value
                    scaleY = scale1.value
                }
                .alpha(alpha1.value),
            tint = Color(0xFF00E5FF)
        )
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale2.value
                    scaleY = scale2.value
                }
                .alpha(alpha2.value),
            tint = Color(0xFF00E5FF)
        )
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale3.value
                    scaleY = scale3.value
                }
                .alpha(alpha3.value),
            tint = Color(0xFF00E5FF)
        )
    }
}

/**
 * Full concave oval overlay displayed when double tapping left or right side of the screen.
 */
@Composable
fun DoubleTapSeekRippleOverlay(
    visible: Boolean,
    isForward: Boolean,
    seekSecondsText: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.92f, animationSpec = tween(120)),
        exit = fadeOut(animationSpec = tween(280)) + scaleOut(targetScale = 0.95f, animationSpec = tween(280)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            val shape = if (isForward) RightSideOvalShape else LeftSideOvalShape
            val gradient = if (isForward) {
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x0000E5FF),
                        Color(0x2200E5FF),
                        Color(0x5500E5FF)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x5500E5FF),
                        Color(0x2200E5FF),
                        Color(0x0000E5FF)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.48f)
                    .clip(shape)
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    DoubleTapSeekChevrons(isForward = isForward)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xD90A0A14),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.8f)),
                        shadowElevation = 8.dp
                    ) {
                        Text(
                            text = seekSecondsText,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Center Play/Pause Ripple Overlay displayed when double-tapping the middle of the player.
 */
@Composable
fun CenterPlayPauseRippleOverlay(
    visible: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.6f, animationSpec = tween(150)) + fadeIn(animationSpec = tween(120)),
        exit = scaleOut(targetScale = 1.35f, animationSpec = tween(240)) + fadeOut(animationSpec = tween(240)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xCC0E0E1A),
                border = BorderStroke(2.dp, Brush.linearGradient(listOf(Color(0xFFE50914), Color(0xFF00E5FF)))),
                shadowElevation = 18.dp,
                modifier = Modifier.size(84.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(46.dp)
                    )
                }
            }
        }
    }
}
