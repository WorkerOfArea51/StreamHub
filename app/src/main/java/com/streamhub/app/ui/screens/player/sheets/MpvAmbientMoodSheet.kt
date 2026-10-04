package com.streamhub.app.ui.screens.player.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.streamhub.app.ui.screens.player.controls.ExpressiveSheetDragHandle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.ui.screens.player.controls.MpvPlayerSheet
import com.streamhub.app.ui.theme.TextSecondary
import kotlin.math.roundToInt

data class AmbientMoodOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val defaultIntensity: Float
)

val AmbientMoodPresets = listOf(
    AmbientMoodOption(
        id = "COZY_CINEMA",
        title = "Cozy Cinema (Default)",
        subtitle = "Soft diffused indigo & deep sapphire. Gentle on the eyes.",
        icon = Icons.Default.AutoAwesome,
        gradientColors = listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF)),
        defaultIntensity = 0.15f
    ),
    AmbientMoodOption(
        id = "WARM_AMBER",
        title = "Warm Amber",
        subtitle = "Warm candlelight glow for comfortable bedtime watching.",
        icon = Icons.Default.NightsStay,
        gradientColors = listOf(Color(0xFFFFB74D), Color(0xFFFF7043)),
        defaultIntensity = 0.18f
    ),
    AmbientMoodOption(
        id = "OLED_SPACE",
        title = "OLED Deep Space",
        subtitle = "Minimal stealth midnight aura preserving infinite OLED blacks.",
        icon = Icons.Default.WbIncandescent,
        gradientColors = listOf(Color(0xFF3F51B5), Color(0xFF1A237E)),
        defaultIntensity = 0.10f
    ),
    AmbientMoodOption(
        id = "VIBRANT",
        title = "Vibrant Aurora",
        subtitle = "Dynamic colorful cinema bloom for daylight watching.",
        icon = Icons.Default.WbSunny,
        gradientColors = listOf(Color(0xFF9C27B0), Color(0xFF00E676)),
        defaultIntensity = 0.35f
    )
)

@Composable
fun MpvAmbientMoodSheet(
    onDismiss: () -> Unit
) {
    val playerSettings by PlayerSettingsManager.settingsFlow.collectAsState()
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    MpvPlayerSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            // Material 3 Expressive Drag Handle
            ExpressiveSheetDragHandle()

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Cinema Ambient Lighting",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Atmospheric diffused backlight",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Master Toggle Switch
                Switch(
                    checked = playerSettings.isAmbientEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        PlayerSettingsManager.updateAmbientEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF7C4DFF),
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (playerSettings.isAmbientEnabled) {
                // Intensity Slider (20.dp borderless tonal container)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x14FFFFFF),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Glow Intensity",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(playerSettings.ambientIntensity * 100).roundToInt()}%",
                                color = Color(0xFFD0BCFF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = playerSettings.ambientIntensity,
                            onValueChange = { PlayerSettingsManager.updateAmbientIntensity(it) },
                            valueRange = 0.05f..0.50f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFD0BCFF),
                                activeTrackColor = Color(0xFF7C4DFF),
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MOOD PRESETS",
                    color = Color(0xFFD0BCFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Mood Selection Cards (20.dp borderless cards)
                AmbientMoodPresets.forEach { preset ->
                    val isSelected = playerSettings.ambientMoodId == preset.id
                    val moodInteractionSource = remember { MutableInteractionSource() }
                    val isPressed by moodInteractionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.96f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "MoodCardScale"
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0x337C4DFF) else Color(0x14FFFFFF),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFD0BCFF) else Color(0x1AFFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = moodInteractionSource,
                                indication = null,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    PlayerSettingsManager.updateAmbientMood(preset.id)
                                    PlayerSettingsManager.updateAmbientIntensity(preset.defaultIntensity)
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Color swatch preview
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(preset.gradientColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = preset.title,
                                        color = if (isSelected) Color.White else Color(0xFFE0E0E0),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFFD0BCFF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
