package com.streamhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.QualityBadgeBg
import com.streamhub.app.ui.theme.TextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaInfoBadges(
    mediaInfo: MediaInfo,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Resolution Badge
        if (mediaInfo.resolution.isNotBlank()) BadgeItem(text = mediaInfo.resolution, borderColor = PrimaryRed)

        // Codec Badge
        if (mediaInfo.videoCodec.isNotBlank()) BadgeItem(text = mediaInfo.videoCodec, borderColor = AccentOrange)

        // File Size Badge
        if (mediaInfo.fileSize.isNotBlank()) BadgeItem(text = mediaInfo.fileSize, borderColor = Color(0xFF3B82F6))

        // Audio Tracks Badges
        mediaInfo.audioTracks.forEach { audio ->
            BadgeItem(text = audio, borderColor = Color(0xFF10B981))
        }

        // Subtitles Badges
        mediaInfo.subtitleTracks.forEach { sub ->
            BadgeItem(text = sub, borderColor = Color(0xFF8B5CF6))
        }
    }
}

@Composable
fun BadgeItem(
    text: String,
    borderColor: Color = PrimaryRed
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(QualityBadgeBg)
            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
