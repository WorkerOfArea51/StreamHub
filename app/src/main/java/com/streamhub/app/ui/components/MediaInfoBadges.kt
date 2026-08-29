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
        if (mediaInfo.resolution.isNotBlank()) {
            BadgeItem(
                text = mediaInfo.resolution,
                borderColor = PrimaryRed,
                bgColor = Color(0x1AFF3B30)
            )
        }

        // Codec Badge
        if (mediaInfo.videoCodec.isNotBlank()) {
            BadgeItem(
                text = mediaInfo.videoCodec,
                borderColor = AccentOrange,
                bgColor = Color(0x1AFF9800)
            )
        }

        // File Size Badge
        if (mediaInfo.fileSize.isNotBlank()) {
            BadgeItem(
                text = mediaInfo.fileSize,
                borderColor = Color(0xFF3B82F6),
                bgColor = Color(0x1A3B82F6)
            )
        }

        // Audio Tracks Badges (🔊 Audio)
        mediaInfo.audioTracks.forEach { audio ->
            val cleanAudio = audio.trim()
            if (cleanAudio.isNotBlank()) {
                val label = if (cleanAudio.startsWith("🔊") || cleanAudio.startsWith("🎧")) cleanAudio else "🔊 $cleanAudio"
                BadgeItem(
                    text = label,
                    borderColor = Color(0xFF10B981),
                    bgColor = Color(0x1A10B981)
                )
            }
        }

        // Subtitles Badges (💬 Subtitle)
        mediaInfo.subtitleTracks.forEach { sub ->
            val cleanSub = sub.trim()
            if (cleanSub.isNotBlank()) {
                val label = if (cleanSub.startsWith("💬") || cleanSub.startsWith("📝") || cleanSub.startsWith("CC")) cleanSub else "💬 $cleanSub"
                BadgeItem(
                    text = label,
                    borderColor = Color(0xFF8B5CF6),
                    bgColor = Color(0x1A8B5CF6)
                )
            }
        }
    }
}

@Composable
fun BadgeItem(
    text: String,
    borderColor: Color = PrimaryRed,
    bgColor: Color = QualityBadgeBg
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
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
