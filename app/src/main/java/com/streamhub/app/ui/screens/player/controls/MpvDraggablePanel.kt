package com.streamhub.app.ui.screens.player.controls

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A draggable panel matching mpvEx DraggablePanel with horizontal dragging in landscape
 * and adaptive portrait centering.
 */
@Composable
fun MpvDraggablePanel(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var panelWidth by remember { mutableIntStateOf(0) }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (isPortrait) Alignment.Center else Alignment.CenterEnd
    ) {
        val density = LocalDensity.current
        val parentWidthPx = with(density) { maxWidth.toPx() }

        val freeSpace = (parentWidthPx - panelWidth).coerceAtLeast(0f)
        val maxOffset = 0f
        val minOffset = -freeSpace

        val panelMaxHeight = if (isPortrait) maxHeight * 0.65f else maxHeight

        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .onSizeChanged { panelWidth = it.width }
                .widthIn(max = 400.dp)
                .heightIn(max = panelMaxHeight),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF212121A),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            tonalElevation = 0.dp,
            shadowElevation = 16.dp
        ) {
            Column {
                // Drag Handle & Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .pointerInput(maxOffset, minOffset) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newOffset = offsetX + dragAmount.x
                                offsetX = newOffset.coerceIn(minOffset, maxOffset)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0x44FFFFFF))
                    )
                }

                // Fixed header (if provided)
                if (header != null) {
                    header()
                }

                // Scrollable content
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    content()
                }
            }
        }
    }
}
