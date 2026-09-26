package com.streamhub.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.PrimaryRed
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
                tintColor = PrimaryRed
            )
        }

        // Codec Badge
        if (mediaInfo.videoCodec.isNotBlank()) {
            BadgeItem(
                text = mediaInfo.videoCodec,
                tintColor = AccentOrange
            )
        }

        // File Size Badge
        if (mediaInfo.fileSize.isNotBlank()) {
            BadgeItem(
                text = mediaInfo.fileSize,
                tintColor = Color(0xFF3B82F6)
            )
        }

        // Audio Tracks Badges
        mediaInfo.audioTracks.forEach { audio ->
            val cleanAudio = audio.replace(Regex("^[🔊🎧\\s]+"), "").trim()
            if (cleanAudio.isNotBlank()) {
                BadgeItem(
                    text = cleanAudio,
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    tintColor = Color(0xFF10B981)
                )
            }
        }

        // Subtitles Badges
        mediaInfo.subtitleTracks.forEach { sub ->
            val cleanSub = sub.replace(Regex("^[💬📝CC\\s]+"), "").trim()
            if (cleanSub.isNotBlank()) {
                BadgeItem(
                    text = cleanSub,
                    icon = Icons.Default.Subtitles,
                    tintColor = Color(0xFF8B5CF6)
                )
            }
        }
    }
}

@Composable
fun BadgeItem(
    text: String,
    icon: ImageVector? = null,
    tintColor: Color = PrimaryRed
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
