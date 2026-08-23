@file:Suppress("DEPRECATION")

package com.streamhub.app.ui.screens.player.controls

import android.annotation.SuppressLint
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val sheetAnimationSpec = tween<Float>(350)

/**
 * Base bottom sheet component matching mpvEx PlayerSheet with AnchoredDraggable physics,
 * nested scrolling, background alpha fade, and adaptive landscape (640dp) / portrait (420dp) bounds.
 */
@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun MpvPlayerSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 0.dp,
    customMaxWidth: Dp? = null,
    customMaxHeight: Dp? = null,
    surfaceColor: Color = Color(0xF212121A),
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val latestOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val maxWidth = customMaxWidth ?: if (LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE) {
        640.dp
    } else {
        420.dp
    }
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val maxHeight = customMaxHeight ?: when {
        isImeVisible -> LocalConfiguration.current.screenHeightDp.dp
        LocalConfiguration.current.orientation == ORIENTATION_PORTRAIT ->
            LocalConfiguration.current.screenHeightDp.dp * 0.90f
        else -> LocalConfiguration.current.screenHeightDp.dp * 0.95f
    }

    var backgroundAlpha by remember { mutableFloatStateOf(0f) }
    val alpha by animateFloatAsState(
        backgroundAlpha,
        animationSpec = sheetAnimationSpec,
        label = "alpha"
    )

    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = 1,
            snapAnimationSpec = sheetAnimationSpec,
            decayAnimationSpec = decayAnimationSpec,
            positionalThreshold = { with(density) { 56.dp.toPx() } },
            velocityThreshold = { with(density) { 125.dp.toPx() } }
        )
    }

    val internalOnDismissRequest = {
        if (anchoredDraggableState.currentValue == 0) {
            scope.launch {
                backgroundAlpha = 0f
                anchoredDraggableState.animateTo(1)
            }
        }
    }

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = internalOnDismissRequest
            )
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
            .onSizeChanged {
                val anchors = DraggableAnchors {
                    0 at 0f
                    1 at it.height.toFloat()
                }
                anchoredDraggableState.updateAnchors(anchors)
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .nestedScroll(
                    remember(anchoredDraggableState) {
                        anchoredDraggableState.preUpPostDownNestedScrollConnection()
                    }
                )
                .then(modifier)
                .offset {
                    IntOffset(
                        0,
                        anchoredDraggableState.offset
                            .takeIf { it.isFinite() }
                            ?.roundToInt()
                            ?: 0
                    )
                }
                .anchoredDraggable(
                    state = anchoredDraggableState,
                    orientation = Orientation.Vertical
                )
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .imePadding(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            tonalElevation = tonalElevation,
            content = {
                BackHandler(
                    enabled = anchoredDraggableState.targetValue == 0,
                    onBack = internalOnDismissRequest
                )
                content()
            }
        )

        LaunchedEffect(true) {
            backgroundAlpha = 0.6f
        }

        LaunchedEffect(anchoredDraggableState) {
            scope.launch { anchoredDraggableState.animateTo(0) }
            snapshotFlow { anchoredDraggableState.currentValue }
                .drop(1)
                .filter { it == 1 }
                .collectLatest { latestOnDismissRequest() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun <T> AnchoredDraggableState<T>.preUpPostDownNestedScrollConnection() =
    object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            val delta = available.toFloat()
            return if (delta < 0 && source == NestedScrollSource.UserInput) {
                dispatchRawDelta(delta).toOffset()
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset =
            if (source == NestedScrollSource.UserInput) {
                dispatchRawDelta(available.toFloat()).toOffset()
            } else {
                Offset.Zero
            }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val toFling = available.toFloat()
            return if (toFling < 0 && offset > 0f) {
                settle(toFling)
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity
        ): Velocity {
            val toFling = available.toFloat()
            return if (toFling > 0) {
                settle(toFling)
                available
            } else {
                Velocity.Zero
            }
        }

        private fun Float.toOffset(): Offset = Offset(0f, this)

        @JvmName("velocityToFloat")
        private fun Velocity.toFloat() = y

        private fun Offset.toFloat(): Float = y
    }
