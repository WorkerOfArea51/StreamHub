package com.streamhub.app.ui.screens.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SlideToUnlock(
    isLocked: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLocked,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onUnlock
                ),
            shape = RoundedCornerShape(50),
            color = Color(0xD91E1E2C),
            border = BorderStroke(1.5.dp, Color(0xFFD0BCFF)),
            shadowElevation = 8.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Unlock Controls",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Screen Locked • Tap to Unlock",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
