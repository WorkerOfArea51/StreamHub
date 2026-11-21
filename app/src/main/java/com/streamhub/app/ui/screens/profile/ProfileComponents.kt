package com.streamhub.app.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.R
import com.streamhub.app.data.UserProfile
import com.streamhub.app.data.UserProfileManager
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.components.UserProfileTierBadge
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyClickable

/**
 * =====================================================================================
 * WHAT IS THIS FILE?
 * =====================================================================================
 * ProfileComponents.kt houses the foundational design tokens, color palette, and reusable
 * UI cards shared across StreamHub's Profile ecosystem (ProfileScreen, MyProfileScreen,
 * and EditProfileScreen).
 *
 * Core components defined in this file:
 *  1. Figma Color Palette: Warm editorial dark-mode design tokens (FigmaHeroCream,
 *     FigmaDarkChocolate, FigmaBurgundy, FigmaLavender, etc.).
 *  2. FigmaHeroProfileCard: The primary asymmetrical hero card featured on the main
 *     Personal Hub screen. Includes a 5-tap secret easter egg counter to unlock Admin mode.
 *  3. MyProfileHeroCard: The rich header card on the My Profile screen, supporting custom
 *     backdrop imagery, VIP tier badges, and one-tap member ID copying.
 *  4. FigmaProfileListItem: Standardized preference row item with Figma SVG icons,
 *     lavender pill badges, and forward chevrons.
 *  5. ProfileMetricCard: Two-column grid card displaying watch stats, library counts,
 *     and streak counters with custom accent tints.
 *  6. ProfileTasteDnaCard: Dynamic media distribution analysis visualizing user taste
 *     across Movies, Anime, and Web Series with proportional bar segments.
 * =====================================================================================
 */

// ── FIGMA DESIGN SYSTEM TOKENS ──
val FigmaHeroCream = Color(0xFFFFD0B8)
val FigmaDarkChocolate = Color(0xFF330A02)
val FigmaBurgundy = Color(0xFF5C2B21)
val FigmaLavender = Color(0xFFD6C3F5)
val FigmaDeepPlum = Color(0xFF30203F)
val FigmaAccentOrange = Color(0xFFFF6348)
val FigmaMutedHeader = Color(0xFFC4B4AF)
val FigmaPrefContainer = Color(0xFF242020)
val FigmaDivider = Color(0xFF302928)
val FigmaTextCream = Color(0xFFFFF1EB)

/**
 * Pixel-Perfect Figma Hero Profile Card (372×213dp, Cream #FFD0B8, Dark Chocolate #330A02)
 * Asymmetric corner shape: topStart=32dp, topEnd=32dp, bottomEnd=32dp, bottomStart=12dp.
 * Strictly displays real dynamic user profile data (custom avatar/preset, custom name, real member ID).
 */
@Composable
fun FigmaHeroProfileCard(
    userProfile: UserProfile,
    isAdmin: Boolean,
    isAccessKeyVerified: Boolean,
    onSecretTapUnlock: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val presets = UserProfileManager.PRESET_AVATARS
    val activePreset = presets.getOrNull(userProfile.avatarPresetIndex) ?: presets.first()
    val displayName = userProfile.customName.ifBlank { "My Profile" }
    val memberId = userProfile.memberId.ifBlank { "SH-User" }

    // 5-Tap secret admin easter egg counter
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // Asymmetric Squircle matching Figma path M0 32C0 14.3269 14.3269 0 32 0H340C357.673 0 372 14.3269 372 32V181C372 198.673 357.673 213 340 213H12C5.37259 213 0 207.627 0 201V32Z
    val heroCardShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
        bottomEnd = 32.dp,
        bottomStart = 12.dp
    )

    Card(
        shape = heroCardShape,
        colors = CardDefaults.cardColors(containerColor = FigmaHeroCream),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Row: Avatar (64dp Circle) + Role Text & Display Name + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Circular Avatar (64dp) with 5-Tap Secret Unlock (fits presets and user photos seamlessly)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(FigmaDarkChocolate)
                            .bouncyClickable {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTime > 3000L) {
                                    tapCount = 1
                                } else {
                                    tapCount++
                                }
                                lastTapTime = now

                                if (tapCount >= 5) {
                                    tapCount = 0
                                    onSecretTapUnlock()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.avatarUri.isNotBlank()) {
                            AsyncImage(
                                model = userProfile.avatarUri,
                                contentDescription = displayName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = activePreset.drawableResId),
                                contentDescription = activePreset.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Role Text - Clean unbordered text matching Figma
                        Text(
                            text = if (isAdmin) {
                                "✦ OWNER"
                            } else if (isAccessKeyVerified) {
                                "✦ VIP MEMBER"
                            } else {
                                "✦ FREE TIER"
                            },
                            color = FigmaDarkChocolate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.0.sp
                        )

                        Text(
                            text = displayName,
                            color = FigmaDarkChocolate,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_chevron_right),
                    contentDescription = "Open Profile",
                    tint = FigmaDarkChocolate,
                    modifier = Modifier.size(width = 9.dp, height = 14.dp)
                )
            }

            // Middle Description
            Text(
                text = "Your viewing pulse, library signals, and profile actions.",
                color = FigmaDarkChocolate.copy(alpha = 0.75f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // Bottom Row: Real Member ID + 1-Tap Bare Copy Button (Matching Figma)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ID:",
                        color = FigmaDarkChocolate.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = memberId,
                        color = FigmaDarkChocolate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_copy),
                    contentDescription = "Copy Member ID",
                    tint = FigmaDarkChocolate,
                    modifier = Modifier
                        .size(20.dp)
                        .bouncyClickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("StreamHub Member ID", memberId))
                            ToastManager.showToast("Copied Member ID: $memberId", Icons.Default.ContentCopy)
                        }
                )
            }
        }
    }
}

/**
 * Grouped Preference Row List Item matching Figma (#242020 container, bare 20dp vector icon in #FFD0B8, #FFF1EB title, #C4B4AF subtitle, #D6C3F5 lavender badge, #C4B4AF chevron)
 */
@Composable
fun FigmaProfileListItem(
    painter: Painter,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = FigmaHeroCream,
                modifier = Modifier.size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = FigmaTextCream,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = FigmaMutedHeader,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            badge?.let {
                Text(
                    text = it,
                    color = FigmaLavender,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_figma_chevron_right),
                contentDescription = null,
                tint = FigmaMutedHeader,
                modifier = Modifier.size(width = 9.dp, height = 14.dp)
            )
        }
    }
}

/**
 * Hero Profile Card (Avatar, Name + Tier Badge, Tagline, and Member ID) used on MyProfileScreen
 */
@Composable
fun MyProfileHeroCard(
    userProfile: UserProfile,
    isAdmin: Boolean,
    isAccessKeyVerified: Boolean,
    remainingDays: Int,
    primaryColor: Color,
    modifier: Modifier = Modifier
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
        modifier = modifier.fillMaxWidth()
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
            }
        }
    }
}

/**
 * 2-Column Overview Metric Card used on MyProfileScreen
 */
@Composable
fun ProfileMetricCard(
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
 * Taste DNA Card with 3-Pillar Distribution: Movies, Anime & Web Series
 */
@Composable
fun ProfileTasteDnaCard(
    moviePercent: Int,
    animePercent: Int,
    seriesPercent: Int,
    primaryColor: Color,
    modifier: Modifier = Modifier
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
        modifier = modifier.fillMaxWidth()
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
