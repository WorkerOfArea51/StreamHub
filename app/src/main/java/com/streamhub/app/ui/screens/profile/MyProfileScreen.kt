package com.streamhub.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.AccessGateManager
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.UserProfileManager
import com.streamhub.app.data.UserStatsManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyClickable

/**
 * =====================================================================================
 * WHAT IS THIS FILE?
 * =====================================================================================
 * MyProfileScreen.kt is the dedicated user pulse and viewing analytics screen.
 * It provides in-depth metrics regarding watch habits, library depth, active streaks,
 * and genre taste distribution.
 *
 * Key components:
 *  1. Top Bar: "My Profile" with back navigation and a prominent "Edit profile" action.
 *  2. MyProfileHeroCard: Displays user photo/preset avatar, VIP tier badge, bio tagline,
 *     and 1-tap copyable Member ID.
 *  3. 6-Card Overview Grid:
 *     - "In progress" (Ready to resume count)
 *     - "Completed" (Marked as watched count)
 *     - "Library" (Saved titles in My List)
 *     - "Tracked time" (Total playback hours)
 *     - "This week" (Recent activity in last 7 days)
 *     - "Streak" (Consecutive daily streaming streak)
 *  4. Taste DNA Card: Computes a personalized viewer persona (e.g. "Anime Connoisseur",
 *     "Cinema Devotee", "Series Binge-Watcher") and renders proportional distribution bars.
 * =====================================================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    onBackClick: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToMyList: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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

    // Calculated Overview Metrics (6-Grid Dashboard)
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
                    // Sleek M3 Top Action Button: "Edit profile"
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
            // ── Hero Profile Card ──
            item(key = "my_profile_hero_card") {
                MyProfileHeroCard(
                    userProfile = userProfile,
                    isAdmin = isAdminMode,
                    isAccessKeyVerified = isAccessKeyUnlocked,
                    remainingDays = remainingVoucherDays,
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
                    ProfileMetricCard(
                        title = "In progress",
                        value = inProgressCount.toString(),
                        subtitle = "Ready to resume",
                        icon = Icons.Default.PlayArrow,
                        accentColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHistory
                    )
                    ProfileMetricCard(
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
                    ProfileMetricCard(
                        title = "Library",
                        value = libraryCount.toString(),
                        subtitle = "Saved titles",
                        icon = Icons.Default.Bookmark,
                        accentColor = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMyList
                    )
                    ProfileMetricCard(
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
                    ProfileMetricCard(
                        title = "This week",
                        value = thisWeekCount.toString(),
                        subtitle = "Recent activity",
                        icon = Icons.Default.Notifications,
                        accentColor = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHistory
                    )
                    ProfileMetricCard(
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
                ProfileTasteDnaCard(
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
