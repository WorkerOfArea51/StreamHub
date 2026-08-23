package com.streamhub.app.ui.screens.player.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.theme.TextPrimary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlsButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    title: String? = null,
    color: Color = Color.White,
    backgroundColor: Color = Color(0x661A1A24),
    borderColor: Color = Color(0x33FFFFFF),
    size: Dp = 45.dp,
    iconSize: Dp = 20.dp,
    badgeText: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = ripple()
            ),
        shape = CircleShape,
        color = backgroundColor,
        contentColor = color,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(iconSize)
            )

            if (!badgeText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE50914),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlsTextBadgeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    textColor: Color = Color.White,
    backgroundColor: Color = Color(0x661A1A24),
    borderColor: Color = Color(0x33FFFFFF),
    height: Dp = 45.dp,
    isActive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val effectiveBg = if (isActive) Color(0xCC6750A4) else backgroundColor
    val effectiveBorder = if (isActive) Color(0xFFD0BCFF) else borderColor

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = ripple()
            ),
        shape = RoundedCornerShape(50),
        color = effectiveBg,
        contentColor = textColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, effectiveBorder)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ControlsGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}
