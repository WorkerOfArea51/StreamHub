package com.streamhub.app.ui.screens.player.sheets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.SubtitleConfig
import com.streamhub.app.ui.screens.player.controls.ExpressiveSheetDragHandle
import com.streamhub.app.ui.screens.player.controls.MpvDraggablePanel
import com.streamhub.app.ui.screens.player.controls.MpvPlayerSheet
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun MpvSubtitleTracksSheet(
    tracks: List<String>,
    selectedTrackId: String?,
    onSelectTrack: (String) -> Unit,
    onAddExternalSubtitle: (Uri) -> Unit = {},
    onOpenSubtitleSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onRemoveSubtitle: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onAddExternalSubtitle(uri)
    }

    MpvPlayerSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // M3 Expressive Drag Handle
            ExpressiveSheetDragHandle(modifier = Modifier.padding(bottom = 6.dp))

            // Header Row: Back button, "Subtitles" title, and clean action pills on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val backInteractionSource = remember { MutableInteractionSource() }
                    val isBackPressed by backInteractionSource.collectIsPressedAsState()
                    val backScale by animateFloatAsState(
                        targetValue = if (isBackPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "SubBackBtnScale"
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        },
                        interactionSource = backInteractionSource,
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer { scaleX = backScale; scaleY = backScale }
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Subtitles",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Online (M3 Expressive Pill)
                    val searchInteractionSource = remember { MutableInteractionSource() }
                    val isSearchPressed by searchInteractionSource.collectIsPressedAsState()
                    val searchScale by animateFloatAsState(
                        targetValue = if (isSearchPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "SearchBtnScale"
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0x336750A4),
                        border = BorderStroke(1.dp, Color(0x55D0BCFF)),
                        modifier = Modifier
                            .graphicsLayer { scaleX = searchScale; scaleY = searchScale }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = searchInteractionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenSearch()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Search Online",
                                color = Color(0xFFD0BCFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Add External Subtitle (M3 Expressive Pill)
                    val extInteractionSource = remember { MutableInteractionSource() }
                    val isExtPressed by extInteractionSource.collectIsPressedAsState()
                    val extScale by animateFloatAsState(
                        targetValue = if (isExtPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "ExtBtnScale"
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0x22FFFFFF),
                        modifier = Modifier
                            .graphicsLayer { scaleX = extScale; scaleY = extScale }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = extInteractionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "External",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Subtitle Settings Menu ⚙
                    val settingsInteractionSource = remember { MutableInteractionSource() }
                    val isSettingsPressed by settingsInteractionSource.collectIsPressedAsState()
                    val settingsScale by animateFloatAsState(
                        targetValue = if (isSettingsPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "SubSettingsBtnScale"
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenSubtitleSettings()
                        },
                        interactionSource = settingsInteractionSource,
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer { scaleX = settingsScale; scaleY = settingsScale }
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Subtitle Settings & Sync",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = Color(0x1FFFFFFF)
            )

            // Available Subtitles Header
            Text(
                text = "Available Subtitles",
                color = Color(0xFFD0BCFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Subtitle Tracks M3 Expressive Card List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(tracks) { trackName ->
                    val isSelected = selectedTrackId == trackName || (selectedTrackId.isNullOrBlank() && trackName == "Off")
                    val isExternal = trackName.startsWith("[Ext]") || trackName.contains("(External)")
                    val itemInteractionSource = remember { MutableInteractionSource() }
                    val isItemPressed by itemInteractionSource.collectIsPressedAsState()
                    val itemScale by animateFloatAsState(
                        targetValue = if (isItemPressed) 0.96f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "SubTrackScale"
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0x336750A4) else Color(0x14FFFFFF),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0x66D0BCFF) else Color(0x14FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { scaleX = itemScale; scaleY = itemScale }
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = itemInteractionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTrack(trackName)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // M3 Expressive Selection Indicator
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFF6750A4) else Color(0x22FFFFFF))
                                    .border(
                                        width = if (isSelected) 0.dp else 1.5.dp,
                                        color = if (isSelected) Color.Transparent else Color(0x44FFFFFF),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            Text(
                                text = trackName,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            if (isExternal && onRemoveSubtitle != null) {
                                val deleteInteractionSource = remember { MutableInteractionSource() }
                                val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
                                val deleteScale by animateFloatAsState(
                                    targetValue = if (isDeletePressed) 0.88f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "DeleteSubScale"
                                )
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onRemoveSubtitle(trackName)
                                    },
                                    interactionSource = deleteInteractionSource,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .graphicsLayer { scaleX = deleteScale; scaleY = deleteScale }
                                        .clip(CircleShape)
                                        .background(Color(0x20FF8A80))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp))
                                }
                            }
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
    subtitleDelayMs: Long = 0L,
    onSubtitleDelayChange: (Long) -> Unit = {},
    onOpenSubtitleDelay: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    MpvPlayerSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            // Material 3 Expressive Drag Handle
            ExpressiveSheetDragHandle()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subtitle Settings",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Typography, styling & delay sync",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                val closeInteractionSource = remember { MutableInteractionSource() }
                val isClosePressed by closeInteractionSource.collectIsPressedAsState()
                val closeScale by animateFloatAsState(
                    targetValue = if (isClosePressed) 0.88f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "SubSettingsCloseScale"
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    interactionSource = closeInteractionSource,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer { scaleX = closeScale; scaleY = closeScale }
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

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Delay Sync Card (M3 Expressive Borderless Container)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x14FFFFFF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MoreTime,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Subtitle Delay Sync",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${if (subtitleDelayMs > 0) "+" else ""}${subtitleDelayMs}ms",
                                color = Color(0xFFD0BCFF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val advInteractionSource = remember { MutableInteractionSource() }
                            val isAdvPressed by advInteractionSource.collectIsPressedAsState()
                            val advScale by animateFloatAsState(
                                targetValue = if (isAdvPressed) 0.92f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "AdvSteppersScale"
                            )
                            Text(
                                text = "Advanced Steppers ▸",
                                color = Color(0xFFD0BCFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .graphicsLayer { scaleX = advScale; scaleY = advScale }
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                                    .clickable(
                                        interactionSource = advInteractionSource,
                                        indication = null
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onOpenSubtitleDelay()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = subtitleDelayMs.toFloat(),
                        onValueChange = { onSubtitleDelayChange(it.toLong()) },
                        valueRange = -3000f..3000f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFD0BCFF),
                            inactiveTrackColor = Color(0x28FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Typography Tools (M3 Expressive Pill Toolbar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x14FFFFFF))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bold Toggle
                val boldInteractionSource = remember { MutableInteractionSource() }
                val isBoldPressed by boldInteractionSource.collectIsPressedAsState()
                val boldScale by animateFloatAsState(
                    targetValue = if (isBoldPressed) 0.88f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "BoldScale"
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateConfig(config.copy(bold = !config.bold))
                    },
                    interactionSource = boldInteractionSource,
                    modifier = Modifier
                        .size(38.dp)
                        .graphicsLayer { scaleX = boldScale; scaleY = boldScale }
                        .clip(CircleShape)
                        .background(if (config.bold) Color(0xFF6750A4) else Color.Transparent)
                ) {
                    Icon(Icons.Default.FormatBold, null, tint = Color.White)
                }

                // Italic Toggle
                val italicInteractionSource = remember { MutableInteractionSource() }
                val isItalicPressed by italicInteractionSource.collectIsPressedAsState()
                val italicScale by animateFloatAsState(
                    targetValue = if (isItalicPressed) 0.88f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "ItalicScale"
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateConfig(config.copy(italic = !config.italic))
                    },
                    interactionSource = italicInteractionSource,
                    modifier = Modifier
                        .size(38.dp)
                        .graphicsLayer { scaleX = italicScale; scaleY = italicScale }
                        .clip(CircleShape)
                        .background(if (config.italic) Color(0xFF6750A4) else Color.Transparent)
                ) {
                    Icon(Icons.Default.FormatItalic, null, tint = Color.White)
                }

                // Alignment Toggle
                val alignInteractionSource = remember { MutableInteractionSource() }
                val isAlignPressed by alignInteractionSource.collectIsPressedAsState()
                val alignScale by animateFloatAsState(
                    targetValue = if (isAlignPressed) 0.88f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "AlignScale"
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val next = when (config.alignment) {
                            "LEFT" -> "CENTER"
                            "CENTER" -> "RIGHT"
                            else -> "LEFT"
                        }
                        onUpdateConfig(config.copy(alignment = next))
                    },
                    interactionSource = alignInteractionSource,
                    modifier = Modifier
                        .size(38.dp)
                        .graphicsLayer { scaleX = alignScale; scaleY = alignScale }
                        .clip(CircleShape)
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

                // Reset Defaults
                val resetInteractionSource = remember { MutableInteractionSource() }
                val isResetPressed by resetInteractionSource.collectIsPressedAsState()
                val resetScale by animateFloatAsState(
                    targetValue = if (isResetPressed) 0.88f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "ResetScale"
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUpdateConfig(SubtitleConfig())
                    },
                    interactionSource = resetInteractionSource,
                    modifier = Modifier
                        .size(38.dp)
                        .graphicsLayer { scaleX = resetScale; scaleY = resetScale }
                        .clip(CircleShape)
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
                    inactiveTrackColor = Color(0x28FFFFFF)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle Edge Style (CaptionStyleCompat)
            Text(
                text = "Edge Style",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_NONE to "None",
                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE to "Outline",
                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW to "Shadow",
                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_RAISED to "Raised",
                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DEPRESSED to "Depressed"
                ).forEach { (edgeType, label) ->
                    val isSelected = config.edgeType == edgeType
                    val edgeInteractionSource = remember { MutableInteractionSource() }
                    val isEdgePressed by edgeInteractionSource.collectIsPressedAsState()
                    val edgeScale by animateFloatAsState(
                        targetValue = if (isEdgePressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "EdgePillScale_$label"
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFF6750A4) else Color(0x14FFFFFF),
                        border = if (isSelected) BorderStroke(1.dp, Color(0xFFD0BCFF)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { scaleX = edgeScale; scaleY = edgeScale }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = edgeInteractionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateConfig(config.copy(edgeType = edgeType))
                            }
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scale by Window Switch (M3 Expressive Borderless Card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x14FFFFFF))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateConfig(config.copy(scaleByWindow = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6750A4),
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Force Clean Typography Switch (M3 Expressive Borderless Card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x14FFFFFF))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Force clean typography",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Override embedded fonts, colors, and positioning with clean user style",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = config.forceCleanTypography,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateConfig(config.copy(forceCleanTypography = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6750A4),
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vertical Screen Position / Height Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vertical screen position", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${(config.bottomPaddingFraction * 100).toInt()}%", color = Color(0xFFD0BCFF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = config.bottomPaddingFraction,
                onValueChange = { onUpdateConfig(config.copy(bottomPaddingFraction = it)) },
                valueRange = 0.02f..0.85f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFFD0BCFF),
                    inactiveTrackColor = Color(0x28FFFFFF)
                )
            )

            // Quick Position Presets (M3 Expressive Pills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Bottom" to 0.08f,
                    "Raised" to 0.16f,
                    "Middle" to 0.45f,
                    "Top" to 0.85f
                ).forEach { (label, fraction) ->
                    val isSelected = kotlin.math.abs(config.bottomPaddingFraction - fraction) < 0.03f
                    val posInteractionSource = remember { MutableInteractionSource() }
                    val isPosPressed by posInteractionSource.collectIsPressedAsState()
                    val posScale by animateFloatAsState(
                        targetValue = if (isPosPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "PosPillScale_$label"
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFF6750A4) else Color(0x14FFFFFF),
                        border = if (isSelected) BorderStroke(1.dp, Color(0xFFD0BCFF)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { scaleX = posScale; scaleY = posScale }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = posInteractionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateConfig(config.copy(bottomPaddingFraction = fraction))
                            }
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 7.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Colors Customizer Section
            Text(
                text = "Subtitle Colors",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Text Color Swatches (CircleShape with Bouncy Spring Scale)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Text:", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp))
                listOf(
                    0xFFFFFFFFL to "White",
                    0xFFFFEB3BL to "Yellow",
                    0xFF00E5FFL to "Cyan",
                    0xFF69F0AEL to "Green",
                    0xFFFF5252L to "Red"
                ).forEach { (colorVal, _) ->
                    val isColorSelected = config.textColorArgb == colorVal
                    val colorInteractionSource = remember { MutableInteractionSource() }
                    val isColorPressed by colorInteractionSource.collectIsPressedAsState()
                    val colorScale by animateFloatAsState(
                        targetValue = if (isColorPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "TextColorScale_$colorVal"
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color(colorVal),
                        border = if (isColorSelected) BorderStroke(2.dp, Color(0xFFD0BCFF)) else BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer { scaleX = colorScale; scaleY = colorScale }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = colorInteractionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateConfig(config.copy(textColorArgb = colorVal))
                            }
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
                    val isBgSelected = config.backgroundColorArgb == colorVal
                    val bgInteractionSource = remember { MutableInteractionSource() }
                    val isBgPressed by bgInteractionSource.collectIsPressedAsState()
                    val bgScale by animateFloatAsState(
                        targetValue = if (isBgPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "BgColorScale_$colorVal"
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (colorVal == 0x00000000L) Color.Transparent else Color(colorVal),
                        border = if (isBgSelected) BorderStroke(2.dp, Color(0xFFD0BCFF)) else BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                            .graphicsLayer { scaleX = bgScale; scaleY = bgScale }
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = bgInteractionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateConfig(config.copy(backgroundColorArgb = colorVal))
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                color = if (colorVal == 0x00000000L) Color(0x88FFFFFF) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isBgSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Live Preview Card (Obsidian Container with Soft Border)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0A0A12),
                border = BorderStroke(1.dp, Color(0x1FFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(config.backgroundColorArgb)
                    ) {
                        Text(
                            text = "Sample Subtitle Preview",
                            color = Color(config.textColorArgb),
                            fontSize = config.fontSizeSp.sp,
                            fontWeight = if (config.bold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (config.italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
}
