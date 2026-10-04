package com.streamhub.app.ui.screens.player.sheets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.api.OnlineSubtitle
import com.streamhub.app.data.api.OnlineSubtitleService
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.screens.player.controls.ExpressiveSheetDragHandle
import com.streamhub.app.ui.screens.player.controls.MpvPlayerSheet
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch
import kotlinx.coroutines.launch

data class OnlineSubtitleResult(
    val id: String,
    val title: String,
    val language: String,
    val format: String,
    val provider: String,
    val downloadUrl: String
)

@Composable
fun MpvOnlineSubtitleSearchSheet(
    initialQuery: String,
    isMovie: Boolean = false,
    initialSeason: Int = 1,
    initialEpisode: Int = 1,
    imdbId: String? = null,
    onApplySubtitle: (Uri, String) -> Unit = { _, _ -> },
    onSelectSubtitle: ((OnlineSubtitleResult) -> Unit)? = null,
    onAddLocalSubtitleUri: ((Uri) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val cleanInitialQuery = remember(initialQuery) {
        OnlineSubtitleService.sanitizeTitle(initialQuery)
    }

    var searchQuery by remember { mutableStateOf(cleanInitialQuery) }
    var currentSeason by remember { mutableIntStateOf(if (initialSeason > 0) initialSeason else 1) }
    var currentEpisode by remember { mutableIntStateOf(if (initialEpisode > 0) initialEpisode else 1) }
    var selectedLanguageCode by remember { mutableStateOf("all") }

    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var allSubtitles by remember { mutableStateOf<List<OnlineSubtitle>>(emptyList()) }
    var downloadingSubtitleId by remember { mutableStateOf<String?>(null) }

    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onAddLocalSubtitleUri?.invoke(uri)
            onDismiss()
        }
    }

    val performSearch: () -> Unit = {
        if (searchQuery.isNotBlank()) {
            keyboardController?.hide()
            isSearching = true
            searchError = null
            scope.launch {
                try {
                    val results = OnlineSubtitleService.searchSubtitles(
                        query = searchQuery,
                        isMovie = isMovie,
                        season = currentSeason,
                        episode = currentEpisode,
                        imdbId = imdbId
                    )
                    allSubtitles = results
                    if (results.isEmpty()) {
                        searchError = "No online subtitles found. Try refining title or adjust episode."
                    }
                } catch (e: Exception) {
                    searchError = "Search error: ${e.localizedMessage ?: "Network connection error"}"
                } finally {
                    isSearching = false
                }
            }
        }
    }

    LaunchedEffect(cleanInitialQuery, currentSeason, currentEpisode) {
        performSearch()
    }

    val filteredSubtitles = remember(allSubtitles, selectedLanguageCode) {
        if (selectedLanguageCode == "all") {
            allSubtitles
        } else {
            allSubtitles.filter { it.languageCode.equals(selectedLanguageCode, ignoreCase = true) }
        }
    }

    val handleDownloadAndApply: (OnlineSubtitle) -> Unit = { sub ->
        downloadingSubtitleId = sub.id
        scope.launch {
            val file = OnlineSubtitleService.downloadSubtitle(context, sub)
            downloadingSubtitleId = null
            if (file != null && file.exists()) {
                val uri = Uri.fromFile(file)
                val label = "${sub.languageName} (Online)"
                onApplySubtitle(uri, label)
                onAddLocalSubtitleUri?.invoke(uri)
                onSelectSubtitle?.invoke(
                    OnlineSubtitleResult(
                        id = sub.id,
                        title = sub.title,
                        language = sub.languageName,
                        format = sub.format,
                        provider = sub.provider,
                        downloadUrl = sub.downloadUrl
                    )
                )
                onDismiss()
            } else {
                ToastManager.showToast("Failed to download subtitle file", Icons.Default.Close)
            }
        }
    }

    val haptic = LocalHapticFeedback.current

    MpvPlayerSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // M3 Expressive Drag Handle
            ExpressiveSheetDragHandle(modifier = Modifier.padding(bottom = 6.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Online Subtitles",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (allSubtitles.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0x336750A4),
                            border = BorderStroke(1.dp, Color(0x556750A4))
                        ) {
                            Text(
                                text = "${allSubtitles.size} Tracks",
                                color = Color(0xFFD0BCFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                val closeInteractionSource = remember { MutableInteractionSource() }
                val isClosePressed by closeInteractionSource.collectIsPressedAsState()
                val closeScale by animateFloatAsState(
                    targetValue = if (isClosePressed) 0.88f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "OnlineSubCloseScale"
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    interactionSource = closeInteractionSource,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer { scaleX = closeScale; scaleY = closeScale }
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

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar & Episode Selector (M3 Expressive 20dp container)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search movie or anime title...", color = TextSecondary, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x14FFFFFF),
                    unfocusedContainerColor = Color(0x14FFFFFF),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Color(0xFFD0BCFF),
                    focusedIndicatorColor = Color(0xFFD0BCFF),
                    unfocusedIndicatorColor = Color.Transparent
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    searchQuery = ""
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                performSearch()
                            },
                            enabled = !isSearching && searchQuery.isNotBlank(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color(0xFFD0BCFF),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Search",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch() })
            )

            // Series Season & Episode Steppers (M3 Expressive Borderless Cards)
            if (!isMovie) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Season Stepper
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x14FFFFFF),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "S$currentSeason",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val sMinusInteraction = remember { MutableInteractionSource() }
                                val isSMinusPressed by sMinusInteraction.collectIsPressedAsState()
                                val sMinusScale by animateFloatAsState(if (isSMinusPressed) 0.88f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "SMinus")
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .graphicsLayer { scaleX = sMinusScale; scaleY = sMinusScale }
                                        .clip(CircleShape)
                                        .background(Color(0x22FFFFFF))
                                        .clickable(interactionSource = sMinusInteraction, indication = null) {
                                            if (currentSeason > 1) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                currentSeason--
                                                performSearch()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                val sPlusInteraction = remember { MutableInteractionSource() }
                                val isSPlusPressed by sPlusInteraction.collectIsPressedAsState()
                                val sPlusScale by animateFloatAsState(if (isSPlusPressed) 0.88f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "SPlus")
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .graphicsLayer { scaleX = sPlusScale; scaleY = sPlusScale }
                                        .clip(CircleShape)
                                        .background(Color(0x22FFFFFF))
                                        .clickable(interactionSource = sPlusInteraction, indication = null) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            currentSeason++
                                            performSearch()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Episode Stepper
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x14FFFFFF),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ep $currentEpisode",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val epMinusInteraction = remember { MutableInteractionSource() }
                                val isEpMinusPressed by epMinusInteraction.collectIsPressedAsState()
                                val epMinusScale by animateFloatAsState(if (isEpMinusPressed) 0.88f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "EpMinus")
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .graphicsLayer { scaleX = epMinusScale; scaleY = epMinusScale }
                                        .clip(CircleShape)
                                        .background(Color(0x22FFFFFF))
                                        .clickable(interactionSource = epMinusInteraction, indication = null) {
                                            if (currentEpisode > 1) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                currentEpisode--
                                                performSearch()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                val epPlusInteraction = remember { MutableInteractionSource() }
                                val isEpPlusPressed by epPlusInteraction.collectIsPressedAsState()
                                val epPlusScale by animateFloatAsState(if (isEpPlusPressed) 0.88f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "EpPlus")
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .graphicsLayer { scaleX = epPlusScale; scaleY = epPlusScale }
                                        .clip(CircleShape)
                                        .background(Color(0x22FFFFFF))
                                        .clickable(interactionSource = epPlusInteraction, indication = null) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            currentEpisode++
                                            performSearch()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontally Scrollable Language Filter Chips (M3 Expressive CircleShape Pills)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(OnlineSubtitleService.supportedLanguages) { lang ->
                    val isSelected = selectedLanguageCode == lang.code
                    val count = if (lang.code == "all") {
                        allSubtitles.size
                    } else {
                        allSubtitles.count { it.languageCode.equals(lang.code, ignoreCase = true) }
                    }
                    val langInteraction = remember { MutableInteractionSource() }
                    val isLangPressed by langInteraction.collectIsPressedAsState()
                    val langScale by animateFloatAsState(
                        targetValue = if (isLangPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "LangChipScale_${lang.code}"
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFF6750A4) else Color(0x14FFFFFF),
                        border = if (isSelected) BorderStroke(1.dp, Color(0xFFD0BCFF)) else null,
                        modifier = Modifier
                            .graphicsLayer { scaleX = langScale; scaleY = langScale }
                            .clip(CircleShape)
                            .clickable(interactionSource = langInteraction, indication = null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedLanguageCode = lang.code
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.displayName,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (count > 0 && !isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($count)",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle Results List (M3 Expressive Borderless Cards)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 340.dp)
            ) {
                when {
                    isSearching -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            com.streamhub.app.ui.components.ExpressiveLoadingIndicator(
                                color = Color(0xFFD0BCFF),
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Searching OpenSubtitles repository...",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    filteredSubtitles.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredSubtitles, key = { it.id }) { sub ->
                                val isDownloading = downloadingSubtitleId == sub.id
                                val cardInteraction = remember { MutableInteractionSource() }
                                val isCardPressed by cardInteraction.collectIsPressedAsState()
                                val cardScale by animateFloatAsState(
                                    targetValue = if (isCardPressed) 0.96f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "OnlineSubCard_${sub.id}"
                                )

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0x14FFFFFF),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable(
                                            interactionSource = cardInteraction,
                                            indication = null,
                                            enabled = !isDownloading
                                        ) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            handleDownloadAndApply(sub)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Language Badge Pill
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF6750A4).copy(alpha = 0.3f))
                                                    .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f), CircleShape)
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = sub.languageCode.uppercase(),
                                                    color = Color(0xFFD0BCFF),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sub.title,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = sub.languageName,
                                                        color = TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "• ${sub.format.uppercase()}",
                                                        color = Color(0xFF10B981),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (sub.encoding.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "• ${sub.encoding}",
                                                            color = TextSecondary,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // 1-Tap Download & Apply Button (CircleShape Pill with Spring Scale)
                                        val applyInteraction = remember { MutableInteractionSource() }
                                        val isApplyPressed by applyInteraction.collectIsPressedAsState()
                                        val applyScale by animateFloatAsState(
                                            targetValue = if (isApplyPressed) 0.92f else 1f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                            label = "ApplyBtn_${sub.id}"
                                        )
                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                handleDownloadAndApply(sub)
                                            },
                                            enabled = !isDownloading,
                                            interactionSource = applyInteraction,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .height(36.dp)
                                                .graphicsLayer { scaleX = applyScale; scaleY = applyScale }
                                        ) {
                                            if (isDownloading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDownload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = searchError ?: "No subtitles found for selected language.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    performSearch()
                                },
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0x33FFFFFF))
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Search", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x1FFFFFFF))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Add Local Subtitle & Done (M3 Expressive Pills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val pickInteraction = remember { MutableInteractionSource() }
                val isPickPressed by pickInteraction.collectIsPressedAsState()
                val pickScale by animateFloatAsState(
                    targetValue = if (isPickPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "PickLocalScale"
                )
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
                    },
                    interactionSource = pickInteraction,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .graphicsLayer { scaleX = pickScale; scaleY = pickScale }
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Local File", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                val closeBottomInteraction = remember { MutableInteractionSource() }
                val isCloseBottomPressed by closeBottomInteraction.collectIsPressedAsState()
                val closeBottomScale by animateFloatAsState(
                    targetValue = if (isCloseBottomPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "CloseBottomScale"
                )
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    interactionSource = closeBottomInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(46.dp)
                        .graphicsLayer { scaleX = closeBottomScale; scaleY = closeBottomScale }
                ) {
                    Text("Close", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
