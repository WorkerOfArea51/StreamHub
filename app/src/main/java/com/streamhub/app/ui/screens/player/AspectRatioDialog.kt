package com.streamhub.app.ui.screens.player

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

data class AspectRatioOption(
    val id: String,
    val label: String,
    val category: String, // "Screen", "Standard", "Cinema"
    val ratio: Float? = null,
    val icon: ImageVector? = null
)

val AllAspectRatioOptions = listOf(
    // Screen category
    AspectRatioOption("FIT", "Fit", "Screen", null, Icons.Default.FitScreen),
    AspectRatioOption("FILL", "Fill", "Screen", null, Icons.Default.Crop),
    AspectRatioOption("ORIGINAL", "Original", "Screen", null, Icons.Default.AspectRatio),
    AspectRatioOption("STRETCH", "Stretch", "Screen", null, Icons.Default.Fullscreen),

    // Standard category
    AspectRatioOption("16_9", "16:9", "Standard", 16f / 9f),
    AspectRatioOption("4_3", "4:3", "Standard", 4f / 3f),
    AspectRatioOption("18_9", "18:9", "Standard", 18f / 9f),
    AspectRatioOption("19.5_9", "19.5:9", "Standard", 19.5f / 9f),
    AspectRatioOption("20_9", "20:9", "Standard", 20f / 9f),
    AspectRatioOption("21_9", "21:9", "Standard", 21f / 9f),

    // Cinema category
    AspectRatioOption("1.85_1", "1.85:1", "Cinema", 1.85f),
    AspectRatioOption("2.21_1", "2.21:1", "Cinema", 2.21f),
    AspectRatioOption("2.35_1", "2.35:1", "Cinema", 2.35f),
    AspectRatioOption("2.39_1", "2.39:1", "Cinema", 2.39f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AspectRatioDrawer(
    selectedId: String,
    onSelect: (AspectRatioOption) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF212121A),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x44FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    text = "Ratio",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Screen Group
            Text("Screen", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AllAspectRatioOptions.filter { it.category == "Screen" }.forEach { opt ->
                    val isSelected = selectedId == opt.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(opt) }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFF4CAF50) else Color(0x22FFFFFF),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                opt.icon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = opt.label,
                                        tint = if (isSelected) Color.White else TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = opt.label,
                            color = if (isSelected) Color(0xFF4CAF50) else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Standard Group
            Text("Standard", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AllAspectRatioOptions.filter { it.category == "Standard" }) { opt ->
                    val isSelected = selectedId == opt.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF4CAF50) else Color(0x22FFFFFF),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF4CAF50) else Color(0x1AFFFFFF)),
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(opt) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = opt.label,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Cinema Group
            Text("Cinema", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AllAspectRatioOptions.filter { it.category == "Cinema" }) { opt ->
                    val isSelected = selectedId == opt.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF4CAF50) else Color(0x22FFFFFF),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF4CAF50) else Color(0x1AFFFFFF)),
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(opt) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = opt.label,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
