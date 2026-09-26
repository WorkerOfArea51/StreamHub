package com.streamhub.app.ui.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyClickable
import kotlinx.coroutines.delay

/**
 * Category & Mood Scopes for the Surprise Me Roulette.
 */
enum class SurpriseFilter(val label: String, val emoji: String) {
    ALL("All", "🌐"),
    ANIME("Anime", "🎌"),
    MOVIES("Movies", "🎬"),
    SERIES("Series", "📺"),
    TOP_RATED("Top Rated 8.0+", "⭐")
}

/**
 * High-End Cinema-Grade "Surprise Me 🎰" Random Show Roulette Modal.
 *
 * Features:
 * - Tactile mechanical slot reel animation with rhythmic haptic ticks
 * - Grand Reveal bounce and radiant ambient cinema back-glow
 * - Category / Mood scoping chips (All, Anime, Movies, Series, Top Rated)
 * - Cinema slate winner presentation (High-res poster, genres, synopsis, ratings)
 * - Complete action suite: Direct "Play Now ▶", "Spin Again 🎲", "Add to My List 🔖", "View Details ℹ️"
 */
@Composable
fun SurpriseMeDialog(
    catalog: List<MediaItem>,
    initialCategoryFilter: String = "ALL",
    onDismiss: () -> Unit,
    onPlayEpisode: ((MediaItem, Int) -> Unit)? = null,
    onMediaClick: (MediaItem) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val initialFilter = remember(initialCategoryFilter) {
        when (initialCategoryFilter.uppercase()) {
            "ANIME" -> SurpriseFilter.ANIME
            "MOVIES", "MOVIE" -> SurpriseFilter.MOVIES
            "SERIES", "WEB_SERIES" -> SurpriseFilter.SERIES
            else -> SurpriseFilter.ALL
        }
    }

    var selectedFilter by remember { mutableStateOf(initialFilter) }
    var spinTrigger by remember { mutableIntStateOf(0) }
    var isSpinning by remember { mutableStateOf(true) }

    // Shuffled ticker titles displayed during reel roll
    var shufflingTitle by remember { mutableStateOf("") }
    var shufflingCategory by remember { mutableStateOf("") }
    var winnerShow by remember { mutableStateOf<MediaItem?>(null) }

    val myListIds by MyListManager.myListFlow.collectAsState()

    // Filter candidate pool according to selected mood chip
    val candidatePool = remember(catalog, selectedFilter) {
        when (selectedFilter) {
            SurpriseFilter.ALL -> catalog
            SurpriseFilter.ANIME -> catalog.filter { it.category.equals("ANIME", ignoreCase = true) }
            SurpriseFilter.MOVIES -> catalog.filter {
                it.category.equals("MOVIE", ignoreCase = true) ||
                it.category.equals("MOVIES", ignoreCase = true) ||
                it.type.equals("MOVIE", ignoreCase = true)
            }
            SurpriseFilter.SERIES -> catalog.filter {
                it.category.equals("SERIES", ignoreCase = true) ||
                it.category.equals("WEB_SERIES", ignoreCase = true)
            }
            SurpriseFilter.TOP_RATED -> catalog.filter {
                (it.rating.toDoubleOrNull() ?: 0.0) >= 8.0
            }
        }
    }

    // Spin reel animation loop
    LaunchedEffect(spinTrigger, selectedFilter) {
        if (candidatePool.isEmpty()) {
            isSpinning = false
            winnerShow = null
            return@LaunchedEffect
        }

        isSpinning = true
        // 14-step mechanical deceleration curve
        val totalSteps = 14
        for (i in 0 until totalSteps) {
            val randomPick = candidatePool.random()
            shufflingTitle = randomPick.title
            shufflingCategory = randomPick.category

            // Tactile mechanical slot tick
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

            // Progressive easing delay from 50ms up to 180ms
            val stepDelay = 50L + (i * i * 0.9f).toLong()
            delay(stepDelay)
        }

        // Final winner selection
        winnerShow = candidatePool.random()
        isSpinning = false

        // Firm lock vibration on reveal
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Infinite dice rotation while spinning
    val infiniteTransition = rememberInfiniteTransition(label = "dice_spin_trans")
    val diceRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dice_rotation"
    )

    // Winner reveal scale bounce
    val revealScale by animateFloatAsState(
        targetValue = if (isSpinning) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "reveal_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD90A0912))
                .clickable { onDismiss() }
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14131F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .width(360.dp)
                    .clickable(enabled = false) {}
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AccentOrange.copy(alpha = 0.7f),
                                Color(0xFFE11D48).copy(alpha = 0.4f),
                                CardBorderDark
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Header Row ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(AccentOrange.copy(alpha = 0.25f), Color(0xFFE11D48).copy(alpha = 0.25f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = "Casino Roulette",
                                    tint = AccentOrange,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .rotate(if (isSpinning) diceRotation else 0f)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = if (isSpinning) "Spinning Reel... 🎰" else "Your Surprise Pick! 🎉",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isSpinning) "Rolling through ${candidatePool.size} titles" else "Curated by StreamHub Roulette",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E2E))
                                .bouncyClickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Mood / Category Filter Chips ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SurpriseFilter.values().forEach { filter ->
                            val isSelected = filter == selectedFilter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) AccentOrange.copy(alpha = 0.2f) else Color(0xFF1E1E2C)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AccentOrange else CardBorderDark,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .bouncyClickable {
                                        if (selectedFilter != filter) {
                                            selectedFilter = filter
                                            spinTrigger++
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${filter.emoji} ${filter.label}",
                                    color = if (isSelected) AccentOrange else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Central Stage: Slot Reel HUD or Winning Media Card ──
                    if (candidatePool.isEmpty()) {
                        // Empty candidate pool handling
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1B1A28))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎭", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No titles found for ${selectedFilter.label}",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try switching to 'All' or a different category",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AccentOrange)
                                        .bouncyClickable {
                                            selectedFilter = SurpriseFilter.ALL
                                            spinTrigger++
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text("Reset to All 🌐", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (isSpinning) {
                        // ── Mechanical Spinning Chamber HUD ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF1C1B2A), Color(0xFF13121E))
                                    )
                                )
                                .border(1.dp, AccentOrange.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Pulsing neon circular ring
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(AccentOrange.copy(alpha = 0.15f), Color.Transparent)
                                        )
                                    )
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(AccentOrange, Color(0xFFE11D48))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = "Spinning",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .rotate(diceRotation)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Fast Shuffling Title Marquee Ticker
                                AnimatedContent(
                                    targetState = shufflingTitle,
                                    transitionSpec = { fadeIn(tween(80)) togetherWith fadeOut(tween(80)) },
                                    label = "ticker_anim"
                                ) { title ->
                                    Text(
                                        text = title.ifBlank { "Rolling..." },
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = shufflingCategory.ifBlank { "Reel Spinning" },
                                    color = AccentOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                com.streamhub.app.ui.components.ExpressiveLoadingIndicator(
                                    size = 28.dp,
                                    color = AccentOrange,
                                    accentColor = PrimaryRed
                                )
                            }
                        }
                    } else {
                        // ── Grand Reveal Winning Slate ──
                        val show = winnerShow ?: candidatePool.first()
                        val isInMyList = myListIds.contains(show.id)
                        val isMovie = show.category.equals("Movie", true) || show.category.equals("Movies", true)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(revealScale),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Poster Card with Ambient Radial Back-Glow
                            Box(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(230.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E1E2C))
                                    .border(
                                        1.5.dp,
                                        Brush.verticalGradient(
                                            listOf(AccentOrange.copy(alpha = 0.8f), CardBorderDark)
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(show.posterUrl)
                                        .crossfade(300)
                                        .build(),
                                    contentDescription = show.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Quality Pill Top Right
                                if (show.mediaInfo.resolution.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xCC000000))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = show.mediaInfo.resolution,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Rating Bottom Left
                                if (show.rating.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xCC000000))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Rating",
                                            tint = AccentGold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = show.rating,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Title
                            Text(
                                text = show.title,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            // Metadata Line
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isMovie) "Movie" else "Series",
                                    color = AccentOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (show.releaseYear.isNotBlank()) {
                                    Text("•", color = TextSecondary, fontSize = 12.sp)
                                    Text(show.releaseYear, color = TextSecondary, fontSize = 12.sp)
                                }

                                if (!isMovie && show.episodes.isNotEmpty()) {
                                    Text("•", color = TextSecondary, fontSize = 12.sp)
                                    Text("${show.episodes.size} eps", color = TextSecondary, fontSize = 12.sp)
                                }
                            }

                            // Genre Tags (up to 3)
                            if (show.genres.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    show.genres.take(3).forEach { genre ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF222133))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = genre.trim(),
                                                color = Color(0xFFD1D1E0),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            // Synopsis Snippet
                            if (show.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = show.description,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ── Action Buttons Row ──
                    if (candidatePool.isNotEmpty()) {
                        val activeWinner = winnerShow ?: candidatePool.firstOrNull()

                        // Primary Action Row: Play Now & Spin Again
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Spin Again Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E2C))
                                    .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
                                    .bouncyClickable(enabled = !isSpinning) {
                                        spinTrigger++
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = "Spin Again",
                                        tint = AccentOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Spin Again",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Play Now Button
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(PrimaryRed, Color(0xFFE50914))
                                        )
                                    )
                                    .bouncyClickable(enabled = !isSpinning && activeWinner != null) {
                                        activeWinner?.let { show ->
                                            onDismiss()
                                            if (onPlayEpisode != null) {
                                                onPlayEpisode(show, 0)
                                            } else {
                                                onMediaClick(show)
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Now",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Play Now",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Secondary Action Row: Add to My List & View Details
                        if (!isSpinning && activeWinner != null) {
                            val isBookmarked = myListIds.contains(activeWinner.id)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Add to My List Toggle
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E1E2C).copy(alpha = 0.6f))
                                        .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
                                        .bouncyClickable {
                                            MyListManager.toggleBookmark(activeWinner.id)
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "My List",
                                            tint = if (isBookmarked) AccentGold else TextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (isBookmarked) "In My List" else "Save to List",
                                            color = if (isBookmarked) AccentGold else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // View Details Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E1E2C).copy(alpha = 0.6f))
                                        .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
                                        .bouncyClickable {
                                            onDismiss()
                                            onMediaClick(activeWinner)
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Details",
                                            tint = Color(0xFFB388FF),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "View Details",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
