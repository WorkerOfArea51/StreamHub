package com.streamhub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Shield
import com.streamhub.app.ui.components.ProxySettingsDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.UserStatsManager
import com.streamhub.app.data.telegram.QrCodeGenerator
import com.streamhub.app.data.telegram.TelegramAuthManager
import com.streamhub.app.data.telegram.TelegramAuthState
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

data class CountryCode(
    val name: String,
    val flag: String,
    val dialCode: String
)

val countryCodesList = listOf(
    CountryCode("Afghanistan", "🇦🇫", "+93"),
    CountryCode("Albania", "🇦🇱", "+355"),
    CountryCode("Algeria", "🇩🇿", "+213"),
    CountryCode("Andorra", "🇦🇩", "+376"),
    CountryCode("Angola", "🇦🇴", "+244"),
    CountryCode("Argentina", "🇦🇷", "+54"),
    CountryCode("Armenia", "🇦🇲", "+374"),
    CountryCode("Australia", "🇦🇺", "+61"),
    CountryCode("Austria", "🇦🇹", "+43"),
    CountryCode("Azerbaijan", "🇦🇿", "+994"),
    CountryCode("Bahrain", "🇧🇭", "+973"),
    CountryCode("Bangladesh", "🇧🇩", "+880"),
    CountryCode("Belarus", "🇧🇾", "+375"),
    CountryCode("Belgium", "🇧🇪", "+32"),
    CountryCode("Bolivia", "🇧🇴", "+591"),
    CountryCode("Bosnia and Herzegovina", "🇧🇦", "+387"),
    CountryCode("Brazil", "🇧🇷", "+55"),
    CountryCode("Bulgaria", "🇧🇬", "+359"),
    CountryCode("Cambodia", "🇰🇭", "+855"),
    CountryCode("Cameroon", "🇨🇲", "+237"),
    CountryCode("Canada", "🇨🇦", "+1"),
    CountryCode("Chile", "🇨🇱", "+56"),
    CountryCode("China", "🇨🇳", "+86"),
    CountryCode("Colombia", "🇨🇴", "+57"),
    CountryCode("Costa Rica", "🇨🇷", "+506"),
    CountryCode("Croatia", "🇭🇷", "+385"),
    CountryCode("Cuba", "🇨🇺", "+53"),
    CountryCode("Cyprus", "🇨🇾", "+357"),
    CountryCode("Czech Republic", "🇨🇿", "+420"),
    CountryCode("Denmark", "🇩🇰", "+45"),
    CountryCode("Dominican Republic", "🇩🇴", "+1"),
    CountryCode("Ecuador", "🇪🇨", "+593"),
    CountryCode("Egypt", "🇪🇬", "+20"),
    CountryCode("Estonia", "🇪🇪", "+372"),
    CountryCode("Ethiopia", "🇪🇹", "+251"),
    CountryCode("Finland", "🇫🇮", "+358"),
    CountryCode("France", "🇫🇷", "+33"),
    CountryCode("Georgia", "🇬🇪", "+995"),
    CountryCode("Germany", "🇩🇪", "+49"),
    CountryCode("Ghana", "🇬🇭", "+233"),
    CountryCode("Greece", "🇬🇷", "+30"),
    CountryCode("Guatemala", "🇬🇹", "+502"),
    CountryCode("Hong Kong", "🇭🇰", "+852"),
    CountryCode("Hungary", "🇭🇺", "+36"),
    CountryCode("Iceland", "🇮🇸", "+354"),
    CountryCode("India", "🇮🇳", "+91"),
    CountryCode("Indonesia", "🇮🇩", "+62"),
    CountryCode("Iran", "🇮🇷", "+98"),
    CountryCode("Iraq", "🇮🇶", "+964"),
    CountryCode("Ireland", "🇮🇪", "+353"),
    CountryCode("Israel", "🇮🇱", "+972"),
    CountryCode("Italy", "🇮🇹", "+39"),
    CountryCode("Jamaica", "🇯🇲", "+1"),
    CountryCode("Japan", "🇯🇵", "+81"),
    CountryCode("Jordan", "🇯🇴", "+962"),
    CountryCode("Kazakhstan", "🇰🇿", "+7"),
    CountryCode("Kenya", "🇰🇪", "+254"),
    CountryCode("Kuwait", "🇰🇼", "+965"),
    CountryCode("Kyrgyzstan", "🇰🇬", "+996"),
    CountryCode("Latvia", "🇱🇻", "+371"),
    CountryCode("Lebanon", "🇱🇧", "+961"),
    CountryCode("Libya", "🇱🇾", "+218"),
    CountryCode("Lithuania", "🇱🇹", "+370"),
    CountryCode("Luxembourg", "🇱🇺", "+352"),
    CountryCode("Malaysia", "🇲🇾", "+60"),
    CountryCode("Maldives", "🇲🇻", "+960"),
    CountryCode("Mexico", "🇲🇽", "+52"),
    CountryCode("Moldova", "🇲🇩", "+373"),
    CountryCode("Monaco", "🇲🇨", "+377"),
    CountryCode("Mongolia", "🇲🇳", "+976"),
    CountryCode("Montenegro", "🇲🇪", "+382"),
    CountryCode("Morocco", "🇲🇦", "+212"),
    CountryCode("Myanmar", "🇲🇲", "+95"),
    CountryCode("Nepal", "🇳🇵", "+977"),
    CountryCode("Netherlands", "🇳🇱", "+31"),
    CountryCode("New Zealand", "🇳🇿", "+64"),
    CountryCode("Nigeria", "🇳🇬", "+234"),
    CountryCode("North Macedonia", "🇲🇰", "+389"),
    CountryCode("Norway", "🇳🇴", "+47"),
    CountryCode("Oman", "🇴🇲", "+968"),
    CountryCode("Pakistan", "🇵🇰", "+92"),
    CountryCode("Palestine", "🇵🇸", "+970"),
    CountryCode("Panama", "🇵🇦", "+507"),
    CountryCode("Paraguay", "🇵🇾", "+595"),
    CountryCode("Peru", "🇵🇪", "+51"),
    CountryCode("Philippines", "🇵🇭", "+63"),
    CountryCode("Poland", "🇵🇱", "+48"),
    CountryCode("Portugal", "🇵🇹", "+351"),
    CountryCode("Qatar", "🇶🇦", "+974"),
    CountryCode("Romania", "🇷🇴", "+40"),
    CountryCode("Russia", "🇷🇺", "+7"),
    CountryCode("Saudi Arabia", "🇸🇦", "+966"),
    CountryCode("Senegal", "🇸🇳", "+221"),
    CountryCode("Serbia", "🇷🇸", "+381"),
    CountryCode("Singapore", "🇸🇬", "+65"),
    CountryCode("Slovakia", "🇸🇰", "+421"),
    CountryCode("Slovenia", "🇸🇮", "+386"),
    CountryCode("South Africa", "🇿🇦", "+27"),
    CountryCode("South Korea", "🇰🇷", "+82"),
    CountryCode("Spain", "🇪🇸", "+34"),
    CountryCode("Sri Lanka", "🇱🇰", "+94"),
    CountryCode("Sweden", "🇸🇪", "+46"),
    CountryCode("Switzerland", "🇨🇭", "+41"),
    CountryCode("Syria", "🇸🇾", "+963"),
    CountryCode("Taiwan", "🇹🇼", "+886"),
    CountryCode("Tajikistan", "🇹🇯", "+992"),
    CountryCode("Tanzania", "🇹🇿", "+255"),
    CountryCode("Thailand", "🇹🇭", "+66"),
    CountryCode("Tunisia", "🇹🇳", "+216"),
    CountryCode("Turkey", "🇹🇷", "+90"),
    CountryCode("Turkmenistan", "🇹🇲", "+993"),
    CountryCode("Uganda", "🇺🇬", "+256"),
    CountryCode("Ukraine", "🇺🇦", "+380"),
    CountryCode("United Arab Emirates", "🇦🇪", "+971"),
    CountryCode("United Kingdom", "🇬🇧", "+44"),
    CountryCode("United States", "🇺🇸", "+1"),
    CountryCode("Uruguay", "🇺🇾", "+598"),
    CountryCode("Uzbekistan", "🇺🇿", "+998"),
    CountryCode("Venezuela", "🇻🇪", "+58"),
    CountryCode("Vietnam", "🇻🇳", "+84"),
    CountryCode("Yemen", "🇾🇪", "+967"),
    CountryCode("Zambia", "🇿🇲", "+260"),
    CountryCode("Zimbabwe", "🇿🇼", "+263")
)

@Composable
fun CountryPickerDialog(
    countries: List<CountryCode>,
    selectedCountry: CountryCode,
    primaryColor: Color,
    onSelectCountry: (CountryCode) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) countries
        else countries.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.dialCode.contains(searchQuery)
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            decorFitsSystemWindows = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {} // prevent dismissing when clicking inside card
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select Country 🌍",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = onDismiss) {
                            Text("✕", color = TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search country or dial code...", color = TextSecondary, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = primaryColor, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF2C2C3E),
                            focusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        "${filteredCountries.size} countries available",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        items(items = filteredCountries, key = { it.name + it.dialCode }) { country ->
                            val isSelected = country.name == selectedCountry.name && country.dialCode == selectedCountry.dialCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        onSelectCountry(country)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(country.flag, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        country.name,
                                        color = if (isSelected) primaryColor else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    country.dialCode,
                                    color = primaryColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToVideoSettings: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by TelegramAuthManager.authState.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    val totalWatchHours by UserStatsManager.totalWatchHours.collectAsState()
    val dailyWatchTime by UserStatsManager.dailyWatchFormatted.collectAsState()
    val streakDays by UserStatsManager.streakDays.collectAsState()

    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var showCountryPickerDialog by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(countryCodesList[0]) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Bar: Title ──
        item(key = "profile_top_bar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profiles & Account 👤",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Authenticated: Show Profile Card & Admin Control Dashboard ──
        if (authState is TelegramAuthState.Authenticated) {
            val user = (authState as TelegramAuthState.Authenticated).user
            val isOwner = (authState as TelegramAuthState.Authenticated).isOwner

            item(key = "profile_card") {
                M3ExpressiveVipProfileCard(
                    user = user,
                    isOwner = isOwner,
                    primaryColor = primaryColor,
                    onOpenTelegram = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/${user.username}"))
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    },
                    onLogout = { TelegramAuthManager.logout() }
                )
            }

            item(key = "admin_owner_dashboard") {
                AdminOwnerDashboardCard(
                    isOwner = isOwner,
                    primaryColor = primaryColor,
                    onOpenAdminPanel = onOpenAdminPanel,
                    onUnlockAdmin = { showAdminPasswordDialog = true }
                )
            }
        }

        // ── Not Authenticated: Show Telegram Login Card ──
        if (authState !is TelegramAuthState.Authenticated) {
            item(key = "telegram_login") {
                TelegramLoginCard(
                    authState = authState,
                    primaryColor = primaryColor
                )
            }

            // ── MTProto Proxy Shortcut (Bypass Censorship / Unblock Telegram) ──
            item(key = "mtproto_proxy_shortcut") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(16.dp))
                        .clickable { showProxyDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2A1010)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "Proxy",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Telegram Blocked in Your Country?",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Configure MTProto Proxy 🛡️ (Bypass Censorship)",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Configure Proxy",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ── Watch Stats Row ──
        item(key = "stats_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    icon = Icons.Default.PlayArrow,
                    label = "Watch Hours",
                    value = totalWatchHours,
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                StatCard(
                    icon = Icons.Default.ElectricBolt,
                    label = "Today",
                    value = dailyWatchTime,
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                StatCard(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Streak",
                    value = "${streakDays}d",
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Settings Card ──
        item(key = "settings_entry") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSettings() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Settings & Preferences", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Theme, notifications, downloads, proxy, about", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // ── Admin Panel Entry (only shown for owner accounts) ──
        if (authState is TelegramAuthState.Authenticated && (authState as TelegramAuthState.Authenticated).isOwner) {
            item(key = "admin_entry") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { showAdminPasswordDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Panel",
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Owner Admin Dashboard", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Manage catalog, users and content", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Owner Admin Password Verification Dialog
    if (showAdminPasswordDialog) {
        AdminPasswordDialog(
            onDismiss = { showAdminPasswordDialog = false },
            onSuccess = {
                showAdminPasswordDialog = false
                onOpenAdminPanel()
            }
        )
    }

    // MTProto Proxy Configuration Dialog
    if (showProxyDialog) {
        ProxySettingsDialog(
            onDismiss = { showProxyDialog = false }
        )
    }
}

@Composable
fun M3ExpressiveVipProfileCard(
    user: com.streamhub.app.data.telegram.TelegramUser,
    isOwner: Boolean,
    primaryColor: Color,
    onOpenTelegram: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { onOpenTelegram() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, primaryColor, CircleShape)
                    ) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = user.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.displayName,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, contentDescription = "Verified", tint = primaryColor, modifier = Modifier.size(16.dp))
                        }

                        if (user.formattedUsername.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onOpenTelegram() }
                            ) {
                                Text(
                                    text = user.formattedUsername,
                                    color = primaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Telegram", tint = primaryColor, modifier = Modifier.size(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        AssistChip(
                            onClick = { onOpenTelegram() },
                            label = { Text(if (isOwner) "Owner Account ✅" else "Telegram Connected ✅", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = primaryColor.copy(alpha = 0.15f),
                                labelColor = primaryColor
                            ),
                            border = AssistChipDefaults.assistChipBorder(borderColor = primaryColor.copy(alpha = 0.3f), enabled = true)
                        )
                    }
                }

                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out", tint = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun TelegramLoginCard(
    authState: TelegramAuthState,
    primaryColor: Color
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCountry by remember { mutableStateOf(countryCodesList[0]) }
    var showCountryPickerDialog by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }

    // Generate real scannable QR Code image bitmap
    val qrBitmap = remember(authState) {
        val qrUrl = if (authState is TelegramAuthState.WaitingQRCode) authState.qrLink else "tg://login?token=streamhub_${System.currentTimeMillis()}"
        QrCodeGenerator.generateQrBitmap(qrUrl, 512)
    }

    androidx.compose.runtime.DisposableEffect(qrBitmap) {
        onDispose {
            runCatching { qrBitmap?.asAndroidBitmap()?.recycle() }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Connect Telegram Account 📱", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Log in with your Telegram account to access private anime and movie channels.", color = TextSecondary, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF14141E),
                contentColor = primaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = primaryColor
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = "Phone Auth", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phone SMS Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = "QR Scan", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("QR Code Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // PHONE NUMBER AUTH FORM (Telegram Country Code Selector)
                when (authState) {
                    is TelegramAuthState.WaitingCode -> {
                        Text("Enter Telegram Verification Code sent to ${authState.phoneNumber}:", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = smsCode,
                            onValueChange = { smsCode = it },
                            placeholder = { Text("e.g. 58392", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2C2C3E),
                                focusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { TelegramAuthManager.submitVerificationCode(smsCode) },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verify Code", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    is TelegramAuthState.WaitingPassword -> {
                        // 2FA Password entry (for accounts with two-factor authentication)
                        Text("This account has Two-Factor Authentication enabled.", color = TextSecondary, fontSize = 11.sp)
                        if (authState.passwordHint.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Hint: ${authState.passwordHint}", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        var twoFaPassword by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = twoFaPassword,
                            onValueChange = { twoFaPassword = it },
                            placeholder = { Text("Enter 2FA Password", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2C2C3E),
                                focusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { TelegramAuthManager.submitPassword(twoFaPassword) },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Password", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        if (authState is TelegramAuthState.Error) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(authState.message, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = { TelegramAuthManager.resetState() },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Dismiss", color = primaryColor, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        Text("Country", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))

                        // TELEGRAM-STYLE SEARCHABLE COUNTRY SELECTOR BOX
                        Card(
                            onClick = { showCountryPickerDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(selectedCountry.flag, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        selectedCountry.name,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(selectedCountry.dialCode, color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search Country",
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // PHONE NUMBER FIELD WITH COUNTRY DIAL CODE PREFIX
                        Text("Phone Number", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            prefix = {
                                Text("${selectedCountry.flag} ${selectedCountry.dialCode} ", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            },
                            placeholder = { Text("000 000 0000", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2C2C3E),
                                focusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val fullNumber = if (phoneNumber.startsWith("+")) phoneNumber else "${selectedCountry.dialCode}$phoneNumber"
                                TelegramAuthManager.startPhoneAuth(fullNumber)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Send Verification Code", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // REAL SCANNABLE QR CODE DISPLAY
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(10.dp)
                ) {
                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "Scannable Telegram QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        CircularProgressIndicator(color = primaryColor)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Open Telegram App on your Phone", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Go to Settings → Devices → Link Desktop Device and scan the QR code above to log in.", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { TelegramAuthManager.generateQRCodeAuth() },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Refresh QR Code", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCountryPickerDialog) {
        CountryPickerDialog(
            countries = countryCodesList,
            selectedCountry = selectedCountry,
            primaryColor = primaryColor,
            onSelectCountry = { country ->
                selectedCountry = country
                showCountryPickerDialog = false
            },
            onDismiss = { showCountryPickerDialog = false }
        )
    }
}

@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = modifier.border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = primaryColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(label, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
fun AdminPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Owner Admin Dashboard 🛡️", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter the Admin Password to unlock catalog management:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password", color = TextSecondary) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMsg, color = Color(0xFFEF4444), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (com.streamhub.app.data.AdminManager.verifyAndEnableAdmin(password)) {
                        onSuccess()
                    } else {
                        errorMsg = "Incorrect Admin Password."
                    }
                }
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdminOwnerDashboardCard(
    isOwner: Boolean,
    primaryColor: Color,
    onOpenAdminPanel: () -> Unit,
    onUnlockAdmin: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161622)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, primaryColor, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = primaryColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Channel Owner Dashboard 👑", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(if (isOwner) "Admin & Creator Mode Active ⚡" else "Channel Admin Access Verified", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, primaryColor)
                ) {
                    Text("OWNER", color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenAdminPanel,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Post", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Post New Content", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenAdminPanel,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, primaryColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Manage", tint = primaryColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Admin Panel", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
