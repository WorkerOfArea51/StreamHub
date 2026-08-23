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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
 * Animated triple chevrons for double tap seeking.
 */
@Composable
fun DoubleTapSeekChevrons(
    isForward: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha1 = remember { Animatable(0f) }
    val alpha2 = remember { Animatable(0f) }
    val alpha3 = remember { Animatable(0f) }

    LaunchedEffect(isForward) {
        while (true) {
            alpha1.animateTo(1f, animationSpec = tween(150))
            alpha2.animateTo(1f, animationSpec = tween(150))
            alpha3.animateTo(1f, animationSpec = tween(150))
            alpha1.animateTo(0.2f, animationSpec = tween(150))
            alpha2.animateTo(0.2f, animationSpec = tween(150))
            alpha3.animateTo(0.2f, animationSpec = tween(150))
        }
    }

    val rotation = if (isForward) 0f else 180f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-4).dp),
        modifier = modifier.rotate(rotation)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(22.dp).alpha(alpha1.value),
            tint = Color.White
        )
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(22.dp).alpha(alpha2.value),
            tint = Color.White
        )
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(22.dp).alpha(alpha3.value),
            tint = Color.White
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
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            val shape = if (isForward) RightSideOvalShape else LeftSideOvalShape
            val gradient = if (isForward) {
                Brush.horizontalGradient(
                    colors = listOf(Color(0x00D0BCFF), Color(0x33D0BCFF), Color(0x55D0BCFF))
                )
            } else {
                Brush.horizontalGradient(
                    colors = listOf(Color(0x55D0BCFF), Color(0x33D0BCFF), Color(0x00D0BCFF))
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = seekSecondsText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
