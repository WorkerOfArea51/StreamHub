package com.streamhub.app.ui.screens.player.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

enum class MpvPanelType {
    NONE,
    SUBTITLE_SETTINGS,
    SUBTITLE_DELAY,
    AUDIO_DELAY,
    VIDEO_FILTERS
}

@Composable
fun MpvPlayerPanels(
    panelShown: MpvPanelType,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (MpvPanelType) -> Unit
) {
    AnimatedContent(
        targetState = panelShown,
        label = "panels",
        contentAlignment = Alignment.CenterEnd,
        contentKey = { it.name },
        transitionSpec = {
            (fadeIn() + slideInHorizontally { it / 3 }) togetherWith (fadeOut() + slideOutHorizontally { it / 2 })
        },
        modifier = modifier
    ) { currentPanel ->
        if (currentPanel == MpvPanelType.NONE) {
            Box(Modifier.fillMaxHeight())
        } else {
            content(currentPanel)
        }
    }
}
