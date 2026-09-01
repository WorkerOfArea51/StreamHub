package com.streamhub.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.SeasonArcOption
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Edit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SeasonArcSelectorSheet(
    title: String = "Select Season",
    universeTitle: String,
    options: List<SeasonArcOption>,
    selectedSeasonNumber: Int = 1,
    selectedArcName: String = "",
    currentMedia: com.streamhub.app.data.models.MediaItem? = null,
    onDismiss: () -> Unit,
    onSelectOption: (SeasonArcOption) -> Unit,
    onEditArc: ((SeasonArcOption) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isArcMode = title.contains("Arc", ignoreCase = true)
    val headerIcon = if (isArcMode) Icons.Default.AutoStories else Icons.Default.Layers
    val headerTint = if (isArcMode) Color(0xFFB388FF) else AccentOrange
    val headerBg = if (isArcMode) Color(0x267C4DFF) else Color(0x26FF9800)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14131C),
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x44FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(headerBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = headerTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (universeTitle.isNotBlank()) {
                            Text(
                                text = if (isArcMode) universeTitle else "$universeTitle Universe",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

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
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Options List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val currentMediaItem = currentMedia ?: options.firstOrNull { it.isCurrent }?.targetMediaItem

                items(options, key = { it.id }) { opt ->
                    val isSelected = if (isArcMode) {
                        opt.internalArcName.equals(selectedArcName, ignoreCase = true)
                    } else if (opt.isExternalMedia) {
                        opt.isCurrent
                    } else {
                        opt.internalSeasonNumber == selectedSeasonNumber && opt.isCurrent
                    }

                    val optTag = if (!isArcMode && opt.targetMediaItem != null && currentMediaItem != null) {
                        com.streamhub.app.data.FranchiseManager.getFranchiseTag(opt.targetMediaItem, currentMediaItem)
                    } else ""

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF261D18) else SurfaceDark,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AccentGold else CardBorderDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = { onSelectOption(opt) },
                                onLongClick = {
                                    if (isArcMode && onEditArc != null) {
                                        onEditArc(opt)
                                    }
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge Tag (e.g. S1, S2, ARC 1, MOVIE)
                            val badgeColor = when {
                                isSelected || opt.isCurrent || optTag.startsWith("CURRENT") -> AccentGold
                                optTag.startsWith("SEQUEL") -> Color(0xFF00E676)
                                optTag.startsWith("PREQUEL") -> Color(0xFF7C4DFF)
                                optTag.startsWith("SIDE STORY") || optTag.startsWith("SPIN-OFF") || optTag.contains("OVA") || optTag.contains("ONA") || optTag.contains("SPECIAL") -> Color(0xFF38BDF8)
                                opt.badge.startsWith("ARC", true) -> Color(0xFF7C4DFF)
                                opt.badge.equals("MOVIE", true) || optTag.contains("MOVIE") -> AccentOrange
                                else -> AccentGold
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeColor.copy(alpha = 0.18f))
                                    .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt.badge.ifBlank { "S${opt.internalSeasonNumber}" },
                                    color = badgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Title & Subtitle Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.title,
                                    color = if (isSelected) AccentGold else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (opt.subtitle.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = opt.subtitle,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Quick Edit Arc Action (if arc mode & callback provided)
                            if (isArcMode && onEditArc != null) {
                                IconButton(
                                    onClick = { onEditArc(opt) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Arc Links",
                                        tint = Color(0xFFB388FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            // Checkmark or chevron
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = AccentGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0x55FFFFFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
