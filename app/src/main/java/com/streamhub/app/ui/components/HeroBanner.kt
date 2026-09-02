package com.streamhub.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.components.FolderSelectionDialog
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroBanner(
    media: MediaItem,
    onPlayClick: (MediaItem) -> Unit,
    onAddToListClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val myListSet by MyListManager.myListFlow.collectAsState()
    val isBookmarked = myListSet.contains(media.id)
    var showFolderSelectionDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        AsyncImage(
            model = media.bannerUrl.ifEmpty { media.posterUrl },
            contentDescription = media.title,
            contentScale = ContentScale.Crop,
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2A2A2A)),
            error = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2A2A2A)),
            fallback = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2A2A2A)),
            modifier = Modifier.fillMaxSize()
        )

        // Multi-layer Dark Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x770A0A0F),
                            Color(0xAA0A0A0F),
                            BackgroundDark
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            // Category & Maturity Tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = media.category,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x802A2A38))
                        .border(1.dp, CardBorderDark, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = media.maturityRating,
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${media.releaseYear} • ⭐ ${media.rating}",
                    color = AccentOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = media.title,
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Genres
            Text(
                text = media.genres.joinToString(" • "),
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onPlayClick(media) },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play Now",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBookmarked) Color(0x33FF9800) else Color(0x22181824),
                    border = BorderStroke(1.dp, if (isBookmarked) AccentOrange else Color(0x44FFFFFF)),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = {
                                val added = MyListManager.toggleBookmark(media.id)
                                val msg = if (added) "Added to My List • Hold to choose folder 📁" else "Removed from My List"
                                ToastManager.showToast(msg)
                                onAddToListClick(media)
                            },
                            onLongClick = {
                                showFolderSelectionDialog = true
                            }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = "My List",
                            tint = if (isBookmarked) AccentOrange else TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBookmarked) "In My List" else "My List",
                            color = if (isBookmarked) AccentOrange else TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (showFolderSelectionDialog) {
            FolderSelectionDialog(
                mediaItem = media,
                onDismiss = { showFolderSelectionDialog = false }
            )
        }
    }
}
