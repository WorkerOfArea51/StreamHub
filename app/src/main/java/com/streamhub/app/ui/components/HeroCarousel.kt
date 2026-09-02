package com.streamhub.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import com.streamhub.app.ui.components.FolderSelectionDialog
import com.streamhub.app.ui.components.ToastManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    featuredItems: List<MediaItem>,
    onPlayClick: (MediaItem) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (featuredItems.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { featuredItems.size })
    val primaryColor = MaterialTheme.colorScheme.primary
    val myListSet by MyListManager.myListFlow.collectAsState()

    var lastInteractionTime by remember { mutableLongStateOf(0L) }
    var selectedMediaForFolder by remember { mutableStateOf<MediaItem?>(null) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Auto-advance hero carousel every 5 seconds (pauses 10s on tap/drag)
    LaunchedEffect(pagerState, featuredItems, lastInteractionTime) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                delay(5000)
                val isInteractionPaused = System.currentTimeMillis() - lastInteractionTime < 10_000L
                if (featuredItems.isNotEmpty() && !pagerState.isScrollInProgress && !isInteractionPaused) {
                    val nextPage = (pagerState.currentPage + 1) % featuredItems.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page !in featuredItems.indices) return@HorizontalPager
            val media = featuredItems[page]
            val isBookmarked = myListSet.contains(media.id)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onMediaClick(media) }
            ) {
                AsyncImage(
                    model = media.bannerUrl.ifEmpty { media.posterUrl },
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Multi-layer Vibrant Glassmorphic Dark Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x660A0A0F),
                                    Color(0xCC0A0A0F),
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
                    // Category Badge & Rating Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(primaryColor)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = media.category,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (media.rating.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x66000000))
                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(media.rating, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (media.releaseYear.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = media.releaseYear, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = media.title,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (media.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = media.description,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                onPlayClick(media)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Watch Now", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Watch Now 🍿", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isBookmarked) primaryColor.copy(alpha = 0.2f) else Color(0x22181824),
                            border = BorderStroke(1.dp, if (isBookmarked) primaryColor else Color(0x44FFFFFF)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {
                                        lastInteractionTime = System.currentTimeMillis()
                                        val added = MyListManager.toggleBookmark(media.id)
                                        val msg = if (added) "Added to My List • Hold to choose folder 📁" else "Removed from My List"
                                        ToastManager.showToast(msg)
                                    },
                                    onLongClick = {
                                        lastInteractionTime = System.currentTimeMillis()
                                        selectedMediaForFolder = media
                                    }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = "My List",
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBookmarked) "In My List" else "My List",
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Carousel Page Indicator Dots
        if (featuredItems.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(featuredItems.size) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 20.dp else 7.dp, 7.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) primaryColor else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }

        selectedMediaForFolder?.let { targetMedia ->
            FolderSelectionDialog(
                mediaItem = targetMedia,
                onDismiss = { selectedMediaForFolder = null }
            )
        }
    }
}
