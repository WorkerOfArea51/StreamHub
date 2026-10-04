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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

private val sheetSpringSpec = spring<Float>(
    dampingRatio = 0.82f,
    stiffness = 380f
)

private val scrimFadeSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

/**
 * Material 3 Expressive drag handle pill with tactile geometry and soft translucent styling.
 */
@Composable
fun ExpressiveSheetDragHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.5.dp)
                .clip(CircleShape)
                .background(Color(0x55FFFFFF))
        )
    }
}

/**
 * Base bottom sheet component matching Material 3 Expressive standards with Spring physics,
 * nested scrolling, background alpha fade, 28dp curvature, and adaptive landscape / portrait bounds.
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
    surfaceColor: Color = Color(0xF2101018),
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val latestOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val maxWidth = customMaxWidth ?: if (isLandscape) {
        660.dp
    } else {
        440.dp
    }
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val maxHeight = customMaxHeight ?: when {
        isImeVisible -> LocalConfiguration.current.screenHeightDp.dp
        LocalConfiguration.current.orientation == ORIENTATION_PORTRAIT ->
            LocalConfiguration.current.screenHeightDp.dp * 0.90f
        else -> LocalConfiguration.current.screenHeightDp.dp * 0.94f
    }

    var backgroundAlpha by remember { mutableFloatStateOf(0f) }
    val alpha by animateFloatAsState(
        backgroundAlpha,
        animationSpec = scrimFadeSpec,
        label = "alpha"
    )

    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = 1,
            snapAnimationSpec = sheetSpringSpec,
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
        val sheetShape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp,
            bottomStart = if (isLandscape) 28.dp else 0.dp,
            bottomEnd = if (isLandscape) 28.dp else 0.dp
        )

        Surface(
            modifier = Modifier
                .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
                .then(if (isLandscape) Modifier.padding(horizontal = 16.dp, vertical = 12.dp) else Modifier)
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
            shape = sheetShape,
            color = surfaceColor,
            border = BorderStroke(1.dp, Color(0x1FFFFFFF)),
            tonalElevation = tonalElevation,
            content = {
                BackHandler(
                    enabled = anchoredDraggableState.targetValue == 0,
                    onBack = internalOnDismissRequest
                )
                content()
            }
        )

        LaunchedEffect(Unit) {
            backgroundAlpha = 0.65f
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
