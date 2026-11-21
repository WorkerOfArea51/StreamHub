package com.streamhub.app.ui.screens.details

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * [DetailsBackdropHeader.kt] handles the rich visual hero section of the Media Details screen:
 *
 * 1. [DetailsHeroBackdrop]:
 *    - Displays the 16:9 cinematic backdrop banner (from TMDB banner, YouTube maxres thumbnail, or poster).
 *    - Glassmorphic "Watch Trailer" play badge that launches YouTube trailer in a popup player.
 *    - Top-left circular glass back button for seamless return navigation.
 *
 * 2. [DetailsTitleMetadataSection]:
 *    - High-res vertical 2:3 poster with rounded squircle corners.
 *    - Primary title & release year.
 *    - Rating badge (AniList blue for Anime, IMDb gold for Movies/Series) & maturity rating (PG-13, TV-MA).
 *    - Production studio and duration indicators.
 *    - Dynamic genre chips flow row.
 *
 * 3. [DetailsActionButtonsRow]:
 *    - "Play Movie" / "Resume Ep X" primary red pill button with offline-ready detection.
 *    - "My List" / "In My List" bookmark button with long-press custom folder selection.
 * =========================================================================================
 */

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.theme.bouncyTouch
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

/**
 * 16:9 Hero Trailer Banner with glass back button and trailer play badge.
 */
@Composable
fun DetailsHeroBackdrop(
    mediaItem: MediaItem,
    backdropUrl: String,
    cleanTrailerId: String,
    onBackClick: () -> Unit,
    onWatchTrailerClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .clickable(enabled = cleanTrailerId.isNotBlank()) {
                onWatchTrailerClick()
            }
    ) {
        // High-Res Cover Backdrop
        AsyncImage(
            model = backdropUrl,
            contentDescription = mediaItem.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    if (cleanTrailerId.isNotBlank()) {
                        onWatchTrailerClick()
                    }
                }
        )

        // Deep Cinematic Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x440A0A0F),
                            Color(0x880A0A0F),
                            BackgroundDark
                        )
                    )
                )
        )

        // Modern Glassmorphic Trailer Play Badge
        if (cleanTrailerId.isNotBlank()) {
            Surface(
                onClick = onWatchTrailerClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .bouncyTouch()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PrimaryRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Watch Trailer",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top-Left Back Button inside trailer header
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .bouncyTouch()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

/**
 * Poster box, Title with Release Year, AniList/IMDb rating badge, Studio & Genre chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsTitleMetadataSection(
    mediaItem: MediaItem
) {
    val isAnime = mediaItem.category.equals("Anime", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster Image
        AsyncImage(
            model = mediaItem.posterUrl,
            contentDescription = mediaItem.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(105.dp)
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val displayTitle = if (mediaItem.releaseYear.isNotBlank() && !mediaItem.title.contains(mediaItem.releaseYear)) {
                "${mediaItem.title} (${mediaItem.releaseYear})"
            } else {
                mediaItem.title
            }

            Text(
                text = displayTitle,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (mediaItem.rating.isNotBlank() || mediaItem.maturityRating.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mediaItem.rating.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isAnime) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF02A9FF)
                                ) {
                                    Text(
                                        text = "AniList",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = mediaItem.rating,
                                    color = Color(0xFF02A9FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF5C518)
                                ) {
                                    Text(
                                        text = "IMDb",
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = mediaItem.rating,
                                    color = Color(0xFFF5C518),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (mediaItem.maturityRating.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = mediaItem.maturityRating,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            val studioDisplay = if (mediaItem.studio.isNotBlank()) mediaItem.studio else if (isAnime) "Anime Studio" else "Production Studio"
            val cleanDuration = mediaItem.duration.replace(" min. per ep.", " min").replace(" min.", " min")
            val durationDisplay = if (cleanDuration.isNotBlank()) " • $cleanDuration" else ""
            Text(
                text = "${mediaItem.category} • $studioDisplay$durationDisplay",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            // Genre Badges in Header
            val headerGenres = remember(mediaItem.genres) {
                mediaItem.genres.filter {
                    !it.equals("Movie", ignoreCase = true) &&
                    !it.equals("Movies", ignoreCase = true) &&
                    !it.equals("TV Series", ignoreCase = true) &&
                    !it.equals("Series", ignoreCase = true) &&
                    !it.equals("Anime", ignoreCase = true)
                }
            }
            if (headerGenres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    headerGenres.take(4).forEach { genre ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = genre,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Action Buttons Row:
 * - Play / Resume button with offline warning support.
 * - My List button with long-press folder picker support.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailsActionButtonsRow(
    playButtonText: String,
    isOnline: Boolean,
    hasCompletedDownloads: Boolean,
    isBookmarked: Boolean,
    onPlayClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onBookmarkLongClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onPlayClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!isOnline && !hasCompletedDownloads) Color(0xFF38384E) else PrimaryRed
            ),
            shape = CircleShape,
            modifier = Modifier
                .weight(1.3f)
                .height(48.dp)
                .bouncyTouch()
        ) {
            Icon(
                imageVector = if (!isOnline && !hasCompletedDownloads) Icons.Default.WifiOff else Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = playButtonText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }

        val myListBg = if (isBookmarked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        val myListTint = if (isBookmarked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

        Surface(
            shape = CircleShape,
            color = myListBg,
            shadowElevation = 2.dp,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .bouncyTouch()
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onBookmarkToggle,
                    onLongClick = onBookmarkLongClick
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
                    tint = myListTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBookmarked) "In My List" else "My List",
                    color = myListTint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
