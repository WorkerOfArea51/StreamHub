package com.streamhub.app.ui.screens.player.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.screens.player.controls.MpvDraggablePanel
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlin.math.roundToInt

data class VideoFilterConfig(
    val brightness: Float = 0f,   // -100 to 100
    val contrast: Float = 0f,     // -100 to 100
    val saturation: Float = 0f,   // -100 to 100
    val gamma: Float = 0f         // -100 to 100
)

enum class MpvFilterPreset(
    val displayName: String,
    val description: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val gamma: Float
) {
    NONE("None", "Default standard colors", 0f, 0f, 0f, 0f),
    VIVID("Vivid", "Punchy colors and enhanced contrast", 5f, 15f, 25f, 0f),
    WARM_TONE("Warm Tone", "Golden hue for cozy lighting", 5f, 5f, 10f, 5f),
    COOL_TONE("Cool Tone", "Clean blue tint for modern aesthetics", 0f, 10f, 5f, 0f),
    SOFT_PASTEL("Soft Pastel", "Gentle muted colors", 10f, -10f, -15f, 5f),
    CINEMATIC("Cinematic", "Film-grade high contrast and depth", -5f, 20f, -10f, -5f),
    DRAMATIC("Dramatic", "Deep darks and extreme saturation", -10f, 30f, 15f, -10f),
    NIGHT_MODE("Night Mode", "Reduced brightness for dark rooms", -20f, 5f, -5f, -10f),
    NOSTALGIC("Nostalgic", "Vintage retro look", 5f, 10f, -20f, 0f),
    GHIBLI_STYLE("Ghibli Style", "Dreamy anime vibrancy", 8f, -5f, 15f, 5f),
    NEON_POP("Neon Pop", "Ultra-vibrant modern colors", 5f, 20f, 40f, 0f),
    DEEP_BLACK("Deep Black", "OLED-optimized deep shadows", -15f, 25f, 5f, -15f)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MpvVideoFiltersSheet(
    filterConfig: VideoFilterConfig,
    onUpdateConfig: (VideoFilterConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val currentPreset = MpvFilterPreset.entries.firstOrNull { preset ->
        preset.brightness == filterConfig.brightness &&
        preset.contrast == filterConfig.contrast &&
        preset.saturation == filterConfig.saturation &&
        preset.gamma == filterConfig.gamma
    } ?: if (filterConfig.brightness == 0f && filterConfig.contrast == 0f && filterConfig.saturation == 0f && filterConfig.gamma == 0f) {
        MpvFilterPreset.NONE
    } else null

    MpvDraggablePanel(
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Video Color Filters",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onUpdateConfig(VideoFilterConfig()) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Filter Presets Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Cinematic Presets",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Presets FlowRow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MpvFilterPreset.entries.forEach { preset ->
                    val isSelected = currentPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onUpdateConfig(
                                VideoFilterConfig(
                                    brightness = preset.brightness,
                                    contrast = preset.contrast,
                                    saturation = preset.saturation,
                                    gamma = preset.gamma
                                )
                            )
                        },
                        label = {
                            Text(
                                text = preset.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0x1EFFFFFF),
                            labelColor = Color.White,
                            selectedContainerColor = Color(0xFF6750A4),
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0x22FFFFFF),
                            selectedBorderColor = Color(0xFFD0BCFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Brightness Slider
            FilterSliderRow(
                title = "Brightness",
                value = filterConfig.brightness,
                range = -100f..100f,
                unit = "%",
                onValueChange = { onUpdateConfig(filterConfig.copy(brightness = it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contrast Slider
            FilterSliderRow(
                title = "Contrast",
                value = filterConfig.contrast,
                range = -100f..100f,
                unit = "%",
                onValueChange = { onUpdateConfig(filterConfig.copy(contrast = it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Saturation Slider
            FilterSliderRow(
                title = "Saturation",
                value = filterConfig.saturation,
                range = -100f..100f,
                unit = "%",
                onValueChange = { onUpdateConfig(filterConfig.copy(saturation = it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Gamma Slider
            FilterSliderRow(
                title = "Gamma",
                value = filterConfig.gamma,
                range = -100f..100f,
                unit = "%",
                onValueChange = { onUpdateConfig(filterConfig.copy(gamma = it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterSliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${if (value > 0) "+" else ""}${value.roundToInt()}$unit",
                color = if (value != 0f) Color(0xFFD0BCFF) else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFFD0BCFF),
                inactiveTrackColor = Color(0x33FFFFFF)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
