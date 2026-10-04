package com.streamhub.app.ui.screens.player.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.ui.screens.player.controls.ExpressiveSheetDragHandle
import com.streamhub.app.ui.screens.player.controls.MpvPlayerSheet
import com.streamhub.app.ui.theme.TextSecondary
import kotlin.math.abs

data class MpvAspectRatioItem(
    val id: String,
    val label: String,
    val ratio: Float? = null,
    val isCustom: Boolean = false
)

val DefaultAspectPresets = listOf(
    MpvAspectRatioItem("DEFAULT", "Default", null),
    MpvAspectRatioItem("FIT", "Fit", null),
    MpvAspectRatioItem("FILL", "Fill", null),
    MpvAspectRatioItem("4_3", "4:3", 4f / 3f),
    MpvAspectRatioItem("16_9", "16:9", 16f / 9f),
    MpvAspectRatioItem("16_10", "16:10", 16f / 10f),
    MpvAspectRatioItem("21_9", "21:9", 21f / 9f),
    MpvAspectRatioItem("32_9", "32:9", 32f / 9f),
    MpvAspectRatioItem("1_1", "1:1", 1f),
    MpvAspectRatioItem("2.35_1", "2.35:1", 2.35f),
    MpvAspectRatioItem("2.39_1", "2.39:1", 2.39f)
)

@Composable
fun MpvAspectRatioSheet(
    selectedId: String,
    onSelectRatio: (MpvAspectRatioItem) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val playerSettings by PlayerSettingsManager.settingsFlow.collectAsState()
    val customRatios = remember { mutableStateListOf<MpvAspectRatioItem>() }
    var widthInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
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
                Text(
                    text = "Aspect Ratio",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Remember for all videos switch (20.dp borderless tonal container)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x14FFFFFF),
                border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Remember Aspect Ratio",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Apply selected ratio automatically to all future videos",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = playerSettings.rememberAspectRatio,
                        onCheckedChange = { PlayerSettingsManager.updateRememberAspectRatio(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6750A4),
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Title
            Text(
                text = "PRESETS",
                color = Color(0xFFD0BCFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Presets Horizontal Row of Chips with Visual Silhouette Box
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DefaultAspectPresets) { item ->
                    val isSelected = selectedId == item.id
                    val previewWidth = when (item.id) {
                        "4_3" -> 16.dp
                        "1_1" -> 13.dp
                        "21_9", "32_9", "2.35_1", "2.39_1" -> 20.dp
                        else -> 17.dp
                    }
                    val previewHeight = when (item.id) {
                        "4_3", "1_1" -> 13.dp
                        "21_9", "32_9", "2.35_1", "2.39_1" -> 9.dp
                        else -> 10.dp
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFF6750A4) else Color(0x18FFFFFF),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFD0BCFF) else Color(0x1AFFFFFF)),
                        modifier = Modifier
                            .height(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (playerSettings.rememberAspectRatio) {
                                    PlayerSettingsManager.updateDefaultAspectRatio(item.id)
                                }
                                onSelectRatio(item)
                                onDismiss()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = previewWidth, height = previewHeight)
                                    .border(1.dp, if (isSelected) Color.White else Color(0x88FFFFFF), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = item.label,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Custom Ratios Section (if any added)
            if (customRatios.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "CUSTOM",
                    color = Color(0xFFD0BCFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(customRatios) { item ->
                        val isSelected = selectedId == item.id
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFF6750A4) else Color(0x18FFFFFF),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFD0BCFF) else Color(0x1AFFFFFF)),
                            modifier = Modifier
                                .height(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectRatio(item)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 14.dp, end = 8.dp)
                            ) {
                                Text(
                                    text = item.label,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete Custom Ratio",
                                    tint = Color(0x99FFFFFF),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            customRatios.remove(item)
                                        }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Add Custom Ratio Row
            Text(
                text = "Add Custom Ratio (e.g. 16:9)",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = widthInput,
                    onValueChange = { widthInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    placeholder = { Text("Width", fontSize = 12.sp, color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0x22FFFFFF),
                        focusedContainerColor = Color(0x14FFFFFF),
                        unfocusedContainerColor = Color(0x10FFFFFF)
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Text(":", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    placeholder = { Text("Height", fontSize = 12.sp, color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0x22FFFFFF),
                        focusedContainerColor = Color(0x14FFFFFF),
                        unfocusedContainerColor = Color(0x10FFFFFF)
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                val addInteractionSource = remember { MutableInteractionSource() }
                val isAddPressed by addInteractionSource.collectIsPressedAsState()
                val addScale by animateFloatAsState(
                    targetValue = if (isAddPressed) 0.88f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "AddRatioScale"
                )

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF6750A4),
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            scaleX = addScale
                            scaleY = addScale
                        }
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = addInteractionSource,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val w = widthInput.toFloatOrNull()
                                val h = heightInput.toFloatOrNull()
                                if (w != null && h != null && w > 0f && h > 0f) {
                                    val customRatio = w / h
                                    val newItem = MpvAspectRatioItem(
                                        id = "CUSTOM_${w}_${h}",
                                        label = "${widthInput}:${heightInput}",
                                        ratio = customRatio,
                                        isCustom = true
                                    )
                                    customRatios.add(newItem)
                                    widthInput = ""
                                    heightInput = ""
                                    keyboardController?.hide()
                                    onSelectRatio(newItem)
                                    onDismiss()
                                }
                            }
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Ratio",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
