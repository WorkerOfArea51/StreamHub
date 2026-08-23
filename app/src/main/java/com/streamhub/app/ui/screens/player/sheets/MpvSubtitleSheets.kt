package com.streamhub.app.ui.screens.player.sheets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.streamhub.app.ui.screens.player.controls.MpvPlayerSheet
import com.streamhub.app.ui.screens.player.controls.MpvDraggablePanel
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.SubtitleConfig
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun MpvSubtitleTracksSheet(
    tracks: List<String>,
    selectedTrackId: String?,
    onSelectTrack: (String) -> Unit,
    onAddExternalSubtitle: (Uri) -> Unit = {},
    onOpenSubtitleSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onAddExternalSubtitle(uri)
    }

    MpvPlayerSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0x44FFFFFF))
                )
            }

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add External",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add external subtitles",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search Online Icon
                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Subtitles",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Style / Typography Palette Icon
                        IconButton(
                            onClick = onOpenSubtitleSettings,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Subtitle Settings",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
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

                Spacer(modifier = Modifier.height(14.dp))

                Text("Embedded Subtitles", color = Color(0xFFD0BCFF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle Track List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                ) {
                    items(tracks) { trackName ->
                        val isSelected = selectedTrackId == trackName || (selectedTrackId.isNullOrBlank() && trackName == "Off")
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0x336750A4) else Color(0x14FFFFFF),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) Color(0xFFD0BCFF) else Color(0x1AFFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectTrack(trackName)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        onSelectTrack(trackName)
                                        onDismiss()
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF6750A4),
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = trackName,
                                    color = if (isSelected) Color(0xFFD0BCFF) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
    }
}

@Composable
fun MpvSubtitleSettingsDrawer(
    config: SubtitleConfig,
    onUpdateConfig: (SubtitleConfig) -> Unit,
    onDismiss: () -> Unit
) {
    MpvDraggablePanel(
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Subtitle Settings",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {

                Spacer(modifier = Modifier.height(16.dp))

                // Typography Tools (Bold, Italic, Alignment, Reset)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onUpdateConfig(config.copy(bold = !config.bold)) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (config.bold) Color(0xFF6750A4) else Color.Transparent)
                    ) {
                        Icon(Icons.Default.FormatBold, null, tint = Color.White)
                    }

                    IconButton(
                        onClick = { onUpdateConfig(config.copy(italic = !config.italic)) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (config.italic) Color(0xFF6750A4) else Color.Transparent)
                    ) {
                        Icon(Icons.Default.FormatItalic, null, tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            val next = when (config.alignment) {
                                "LEFT" -> "CENTER"
                                "CENTER" -> "RIGHT"
                                else -> "LEFT"
                            }
                            onUpdateConfig(config.copy(alignment = next))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = when (config.alignment) {
                                "LEFT" -> Icons.AutoMirrored.Filled.FormatAlignLeft
                                "RIGHT" -> Icons.AutoMirrored.Filled.FormatAlignRight
                                else -> Icons.Default.FormatAlignCenter
                            },
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { onUpdateConfig(SubtitleConfig()) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, null, tint = Color(0xFFD0BCFF))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Font size", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${config.fontSizeSp.toInt()} sp", color = Color(0xFFD0BCFF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.fontSizeSp,
                    onValueChange = { onUpdateConfig(config.copy(fontSizeSp = it)) },
                    valueRange = 12f..48f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFD0BCFF),
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Border / Outline Width Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Border outline size", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${config.outlineWidth.toInt()} dp", color = Color(0xFFD0BCFF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.outlineWidth,
                    onValueChange = { onUpdateConfig(config.copy(outlineWidth = it)) },
                    valueRange = 0f..8f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFD0BCFF),
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Shadow Offset Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Shadow offset", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${config.shadowOffset.toInt()} dp", color = Color(0xFFD0BCFF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.shadowOffset,
                    onValueChange = { onUpdateConfig(config.copy(shadowOffset = it)) },
                    valueRange = 0f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFD0BCFF),
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scale by Window Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scale by window",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Scale subtitles automatically with video aspect size",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = config.scaleByWindow,
                        onCheckedChange = { onUpdateConfig(config.copy(scaleByWindow = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6750A4),
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Colors Customizer Section (Matching mpvEx SubtitleSettingsColorsCard)
                Text(
                    text = "Subtitle Colors",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Text Color Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Text:", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp))
                    listOf(
                        0xFFFFFFFF to "White",
                        0xFFFFEB3B to "Yellow",
                        0xFF00E5FF to "Cyan",
                        0xFF69F0AE to "Green",
                        0xFFFF5252 to "Red"
                    ).forEach { (colorVal, _) ->
                        Surface(
                            shape = CircleShape,
                            color = Color(colorVal),
                            border = if (config.textColorArgb == colorVal) BorderStroke(2.dp, Color(0xFFD0BCFF)) else BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { onUpdateConfig(config.copy(textColorArgb = colorVal)) }
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Background Color Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Box:", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp))
                    listOf(
                        0x00000000L to "None",
                        0x80000000L to "Semi",
                        0xCC000000L to "Dark",
                        0xFF000000L to "Solid",
                        0x801E1E28L to "Obsidian"
                    ).forEach { (colorVal, label) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (colorVal == 0x00000000L) Color.Transparent else Color(colorVal),
                            border = if (config.backgroundColorArgb == colorVal) BorderStroke(2.dp, Color(0xFFD0BCFF)) else BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .height(28.dp)
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onUpdateConfig(config.copy(backgroundColorArgb = colorVal)) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (colorVal == 0x00000000L) Color(0x88FFFFFF) else Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Live Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0A0A0F),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(config.backgroundColorArgb)
                        ) {
                            Text(
                                text = "Sample Subtitle Preview",
                                color = Color(config.textColorArgb),
                                fontSize = config.fontSizeSp.sp,
                                fontWeight = if (config.bold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (config.italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
    }
}

