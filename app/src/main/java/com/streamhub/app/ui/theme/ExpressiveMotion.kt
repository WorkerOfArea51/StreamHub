package com.streamhub.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role

/**
 * Material 3 Expressive motion system for StreamHub.
 * Provides tactile, bouncy spring physics on touch press and release.
 */

/**
 * Attaches a spring scale press effect to any composable (Button, Card, Surface, etc.)
 * by observing pointer gestures non-intrusively without consuming the event.
 */
fun Modifier.bouncyTouch(
    pressedScale: Float = 0.95f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isPressed) Spring.StiffnessMediumLow else Spring.StiffnessLow
        ),
        label = "bouncy_touch_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}

/**
 * Material 3 Expressive bouncy clickable modifier.
 * Combines touch press detection with spring scale animation and an onClick callback.
 */
fun Modifier.bouncyClickable(
    pressedScale: Float = 0.94f,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isPressed) Spring.StiffnessMediumLow else Spring.StiffnessLow
        ),
        label = "bouncy_clickable_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Scale animation provides the primary tactile feedback
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/**
 * Attaches bouncy spring scale to a composable sharing a [MutableInteractionSource].
 */
fun Modifier.bouncyPress(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.95f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isPressed) Spring.StiffnessMediumLow else Spring.StiffnessLow
        ),
        label = "bouncy_press_scale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
