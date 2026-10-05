package com.streamhub.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.AccessGateManager
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.UserProfile
import com.streamhub.app.data.UserProfileManager
import com.streamhub.app.data.UserStatsManager
import com.streamhub.app.data.WatchHistoryManager

import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.components.UserProfileTierBadge
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    onBackClick: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToMyList: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    // User Profile & Stats States
    val userProfile by UserProfileManager.profileFlow.collectAsState()
    val totalWatchHours by UserStatsManager.totalWatchHours.collectAsState()
    val streakDays by UserStatsManager.streakDays.collectAsState()
    val animePercent by UserStatsManager.animePercent.collectAsState()
    val moviePercent by UserStatsManager.moviePercent.collectAsState()
    val seriesPercent by UserStatsManager.seriesPercent.collectAsState()

    // Admin & Access Status
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    val isAccessKeyUnlocked by AccessGateManager.isUnlocked.collectAsState()
    val remainingVoucherDays by AccessGateManager.remainingDays.collectAsState()



    // Watch History & My List data
    val historyMap by WatchHistoryManager.historyFlow.collectAsState()
    val myListItems by MyListManager.itemsFlow.collectAsState()

    // Calculated Overview Metrics (Matching Nuvio 6-Grid)
    val inProgressCount = remember(historyMap) {
        historyMap.values.count { !it.isCompleted && it.positionMs > 0 && (it.durationMs == 0L || it.positionMs < it.durationMs * 0.9) }
    }
    val completedCount = remember(historyMap) {
        historyMap.values.count { it.isCompleted || (it.durationMs > 0 && it.positionMs >= it.durationMs * 0.9) }
    }
    val libraryCount = remember(myListItems) {
        myListItems.size
    }
    val thisWeekCount = remember(historyMap) {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 86_400_000L)
        historyMap.values.count { it.lastUpdated >= sevenDaysAgo }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Profile",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Sleek M3 Top Action Button: "Edit profile" (matching Nuvio photo 2)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .bouncyClickable { onNavigateToEditProfile() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Edit profile",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Hero Profile Card (Matches Nuvio Photo 2 Hero Card + Background + Status) ──
            item(key = "my_profile_hero_card") {
                MyProfileHeroCard(
                    userProfile = userProfile,
                    isAdmin = isAdminMode,
                    isAccessKeyVerified = isAccessKeyUnlocked,
                    remainingDays = remainingVoucherDays,
                    inProgressCount = inProgressCount,
                    libraryCount = libraryCount,
                    streakDays = streakDays,
                    primaryColor = primaryColor
                )
            }

            // ── Section 1: OVERVIEW Header ──
            item(key = "overview_header") {
                Text(
                    text = "OVERVIEW",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // ── 6-Card Overview Grid (2 Columns, 3 Rows) ──
            item(key = "overview_row_1") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NuvioMetricCard(
                        title = "In progress",
                        value = inProgressCount.toString(),
                        subtitle = "Ready to resume",
                        icon = Icons.Default.PlayArrow,
                        accentColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHistory
                    )
                    NuvioMetricCard(
                        title = "Completed",
                        value = completedCount.toString(),
                        subtitle = "Marked as watched",
                        icon = Icons.Default.Favorite,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHistory
                    )
                }
            }

            item(key = "overview_row_2") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NuvioMetricCard(
                        title = "Library",
                        value = libraryCount.toString(),
                        subtitle = "Saved titles",
                        icon = Icons.Default.Bookmark,
                        accentColor = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMyList
                    )
                    NuvioMetricCard(
                        title = "Tracked time",
                        value = totalWatchHours,
                        subtitle = "From playback progress",
                        icon = Icons.Default.AutoAwesome,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "overview_row_3") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NuvioMetricCard(
                        title = "This week",
                        value = thisWeekCount.toString(),
                        subtitle = "Recent activity",
                        icon = Icons.Default.Notifications,
                        accentColor = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHistory
                    )
                    NuvioMetricCard(
                        title = "Streak",
                        value = "${streakDays}d",
                        subtitle = if (streakDays > 0) "Daily streak on fire!" else "Start watching today",
                        icon = Icons.Default.LocalFireDepartment,
                        accentColor = Color(0xFFFF5722),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Section 2: TASTE PROFILE Header ──
            item(key = "taste_profile_header") {
                Text(
                    text = "TASTE PROFILE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            // ── Taste DNA Card (Tri-Pillar: Movies, Anime, Web Series) ──
            item(key = "taste_dna_card") {
                NuvioTasteDnaCard(
                    moviePercent = moviePercent,
                    animePercent = animePercent,
                    seriesPercent = seriesPercent,
                    primaryColor = primaryColor
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }


}

/**
 * Hero Profile Card (Avatar with 5-tap easter egg, Name + Tier Badge, Tagline, Member ID, and 3 Quick Metrics)
 */
@Composable
private fun MyProfileHeroCard(
    userProfile: UserProfile,
    isAdmin: Boolean,
    isAccessKeyVerified: Boolean,
    remainingDays: Int,
    inProgressCount: Int,
    libraryCount: Int,
    streakDays: Int,
    primaryColor: Color
) {
    val context = LocalContext.current
    val presets = UserProfileManager.PRESET_AVATARS
    val activePreset = presets.getOrNull(userProfile.avatarPresetIndex) ?: presets.first()
    val displayName = userProfile.customName.ifBlank { "StreamHub Explorer" }
    val displayTagline = userProfile.customTagline.ifBlank {
        "Viewing history, library signals, and releases are tracked for this profile."
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Optional custom background backdrop with gradient scrim
            if (userProfile.backgroundUri.isNotBlank()) {
                AsyncImage(
                    model = userProfile.backgroundUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Large Avatar
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(2.dp, primaryColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.avatarUri.isNotBlank()) {
                            AsyncImage(
                                model = userProfile.avatarUri,
                                contentDescription = displayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = activePreset.drawableResId),
                                contentDescription = activePreset.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Name + Tier Badge + Bio + Member ID
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            UserProfileTierBadge(
                                isAdmin = isAdmin,
                                isAccessKeyVerified = isAccessKeyVerified,
                                remainingDays = remainingDays
                            )
                        }

                        Text(
                            text = displayTagline,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Member ID Pill with 1-tap clipboard copy
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            modifier = Modifier.bouncyClickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("StreamHub Member ID", userProfile.memberId))
                                ToastManager.showToast("Copied Member ID: ${userProfile.memberId}", Icons.Default.ContentCopy)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text("ID:", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(userProfile.memberId, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }

                // Quick Metric Pills (In progress, Library, Streak)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickMetricPill(
                        value = inProgressCount.toString(),
                        label = "In progress",
                        modifier = Modifier.weight(1f)
                    )
                    QuickMetricPill(
                        value = libraryCount.toString(),
                        label = "Library",
                        modifier = Modifier.weight(1f)
                    )
                    QuickMetricPill(
                        value = "${streakDays}d",
                        label = "Streak",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Compact stat pill inside Hero Card (e.g. "2 In progress", "0 Library", "1d Streak")
 */
@Composable
private fun QuickMetricPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 2-Column Overview Metric Card (Matches Nuvio Photo 2 Grid Items)
 */
@Composable
private fun NuvioMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.then(
            if (onClick != null) Modifier.bouncyClickable { onClick() } else Modifier
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Value + Icon Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Title + Subtitle
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Nuvio-style Taste DNA Card with 3-Pillar Distribution: Movies, Anime & Web Series
 */
@Composable
private fun NuvioTasteDnaCard(
    moviePercent: Int,
    animePercent: Int,
    seriesPercent: Int,
    primaryColor: Color
) {
    val total = (moviePercent + animePercent + seriesPercent).coerceAtLeast(1)
    val normMovie = (moviePercent * 100) / total
    val normAnime = (animePercent * 100) / total
    val normSeries = (100 - normMovie - normAnime).coerceAtLeast(0)

    val (archetypeTitle, leaningLabel, badgeText) = when {
        normAnime >= 40 && normAnime >= normMovie && normAnime >= normSeries ->
            Triple("Anime Connoisseur", "Anime leaning", "🎌 Otaku Spirit")
        normMovie >= 40 && normMovie >= normAnime && normMovie >= normSeries ->
            Triple("Cinema Devotee", "Movie leaning", "🎬 Cinema-First")
        normSeries >= 40 && normSeries >= normMovie && normSeries >= normAnime ->
            Triple("Series Binge-Watcher", "Series leaning", "⚡ Binge Maestro")
        else ->
            Triple("Eclectic Cinephile", "Balanced taste", "🌟 Universal Pulse")
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Sparkle Icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Taste DNA",
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Taste DNA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Text(
                        text = archetypeTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Based on saved genres, watch history & catalog types.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Proportional 3-Segment Balance Bar Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Media Catalog Distribution",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = leaningLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                // Tri-Pillar Proportional Segment Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    if (normMovie > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(normMovie.toFloat().coerceAtLeast(0.1f))
                                .background(Color(0xFF00E5FF))
                        )
                    }
                    if (normAnime > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(normAnime.toFloat().coerceAtLeast(0.1f))
                                .background(Color(0xFFE50914))
                        )
                    }
                    if (normSeries > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(normSeries.toFloat().coerceAtLeast(0.1f))
                                .background(Color(0xFF10B981))
                        )
                    }
                }

                // 3 Pillar Percentage Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF00E5FF)))
                        Text(
                            text = "Movies $normMovie%",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFE50914)))
                        Text(
                            text = "Anime $normAnime%",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Text(
                            text = "Series $normSeries%",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Persona Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
