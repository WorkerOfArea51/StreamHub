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
import androidx.compose.ui.platform.LocalContext
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

    MpvPlayerSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0x44FFFFFF))
                )
            }

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
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x336750A4),
                            border = BorderStroke(1.dp, Color(0x556750A4))
                        ) {
                            Text(
                                text = "${allSubtitles.size} Tracks",
                                color = Color(0xFFD0BCFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar & Episode Selector
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search movie or anime title...", color = TextSecondary, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Color(0xFFD0BCFF),
                    focusedIndicatorColor = Color(0xFFD0BCFF),
                    unfocusedIndicatorColor = CardBorderDark
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
                                onClick = { searchQuery = "" },
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
                            onClick = performSearch,
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

            // Series Season & Episode Steppers
            if (!isMovie) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Season Stepper
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "S$currentSeason",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x22FFFFFF))
                                        .clickable {
                                            if (currentSeason > 1) {
                                                currentSeason--
                                                performSearch()
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x22FFFFFF))
                                        .clickable {
                                            currentSeason++
                                            performSearch()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Episode Stepper
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ep $currentEpisode",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x22FFFFFF))
                                        .clickable {
                                            if (currentEpisode > 1) {
                                                currentEpisode--
                                                performSearch()
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x22FFFFFF))
                                        .clickable {
                                            currentEpisode++
                                            performSearch()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontally Scrollable Language Filter Chips
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

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF6750A4) else SurfaceDark,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFD0BCFF) else CardBorderDark),
                        modifier = Modifier
                            .clickable { selectedLanguageCode = lang.code }
                            .bouncyTouch()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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

            // Subtitle Results List
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

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceDark,
                                    border = BorderStroke(1.dp, CardBorderDark),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isDownloading) { handleDownloadAndApply(sub) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
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
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF6750A4).copy(alpha = 0.25f))
                                                    .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
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

                                        // 1-Tap Download & Apply Button
                                        Button(
                                            onClick = { handleDownloadAndApply(sub) },
                                            enabled = !isDownloading,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
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
                                onClick = performSearch,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CardBorderDark)
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
            HorizontalDivider(color = CardBorderDark)
            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Add Local Subtitle & Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
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

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Close", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
