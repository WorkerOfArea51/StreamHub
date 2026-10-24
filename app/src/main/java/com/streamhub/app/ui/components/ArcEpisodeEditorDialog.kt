package com.streamhub.app.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streamhub.app.data.EpisodeOrderingManager
import com.streamhub.app.data.SeasonArcOption
import com.streamhub.app.data.StreamLinkResolver
import com.streamhub.app.data.api.F2lApiClient
import com.streamhub.app.data.api.StreamHealthChecker
import com.streamhub.app.data.api.StreamHealthReport
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.parser.BatchEpisodeParser
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.SurfaceVariantDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch
import kotlinx.coroutines.launch

@Composable
fun ArcEpisodeEditorDialog(
    mediaItem: MediaItem,
    arcOption: SeasonArcOption,
    onDismiss: () -> Unit,
    onSaveArcEpisodes: (updatedArcEpisodes: List<Episode>) -> Unit
) {
    val targetArcName = arcOption.internalArcName
    val targetSeasonNumber = arcOption.internalSeasonNumber.coerceAtLeast(1)

    // Initial episodes for this specific arc
    val initialArcEpisodes = remember(mediaItem.episodes, targetArcName, targetSeasonNumber) {
        mediaItem.episodes.filter { ep ->
            if (targetArcName.isNotBlank()) {
                ep.arcName.equals(targetArcName, ignoreCase = true)
            } else {
                ep.seasonNumber == targetSeasonNumber
            }
        }.sortedBy { it.episodeNumber }
    }

    val arcEpisodes = remember { mutableStateListOf<Episode>().apply { addAll(initialArcEpisodes) } }

    var currentArcName by remember(arcOption.internalArcName) {
        mutableStateOf(arcOption.internalArcName.ifBlank { "Arc ${arcOption.internalSeasonNumber}" })
    }
    var rawSnippetText by remember { mutableStateOf("") }
    var f2lBatchInput by remember { mutableStateOf("") }
    var isFetchingF2l by remember { mutableStateOf(false) }

    // Import Modes: Replace (Overwrite) vs Merge
    var replaceF2lArcMode by remember { mutableStateOf(true) }
    var replaceSnippetMode by remember { mutableStateOf(false) }

    // Clear Arc Confirmation Dialog
    var showClearArcDialog by remember { mutableStateOf(false) }

    // Live Feedback Banners
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successToast by remember { mutableStateOf<String?>(null) }

    // Health Checking
    var isCheckingHealth by remember { mutableStateOf(false) }
    var healthReport by remember { mutableStateOf<StreamHealthReport?>(null) }

    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Gap Detection for this specific Arc
    val missingGaps = remember(arcEpisodes.size, arcEpisodes.map { it.episodeNumber }) {
        if (arcEpisodes.size >= 2) {
            val sortedNums = arcEpisodes.map { it.episodeNumber }.distinct().sorted()
            val minEp = sortedNums.first()
            val maxEp = sortedNums.last()
            val existingSet = sortedNums.toSet()
            (minEp..maxEp).filter { it !in existingSet }
        } else {
            emptyList()
        }
    }

    // Confirmation dialog before clearing all episodes in this arc
    if (showClearArcDialog) {
        AlertDialog(
            onDismissRequest = { showClearArcDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = PrimaryRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Clear Arc Episodes?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to remove all ${arcEpisodes.size} episodes from \"${currentArcName}\"? This arc will be emptied. You can re-import or paste new episodes anytime.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = arcEpisodes.size
                        arcEpisodes.clear()
                        showClearArcDialog = false
                        errorMessage = null
                        successToast = "Cleared $count episodes from this Arc"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.bouncyTouch()
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearArcDialog = false },
                    modifier = Modifier.bouncyTouch()
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurfaceDark,
            tonalElevation = 8.dp
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, CardBorderDark),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(AccentOrange.copy(alpha = 0.25f), AccentGold.copy(alpha = 0.25f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = arcOption.title.ifBlank { "Edit Arc Episodes" },
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${mediaItem.title} • ${arcEpisodes.size} Episodes in this Arc",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x18FFFFFF))
                            .bouncyTouch()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Missing Gap Warning Banner
                if (missingGaps.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentOrange.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Missing in this Arc: ${missingGaps.joinToString(", ") { "EP $it" }}",
                                color = Color(0xFFFFCC80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // High-Visibility Feedback Alert Banners
                errorMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PrimaryRed.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = PrimaryRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = msg,
                                color = Color(0xFFFF8A80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { errorMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                successToast?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = msg,
                                color = Color(0xFFA7F3D0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { successToast = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Story Arc Name / Saga Title Edit Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceVariantDark,
                            border = BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Story Arc / Saga Title",
                                        color = AccentGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = currentArcName,
                                    onValueChange = { currentArcName = it },
                                    label = { Text("Arc Name (Rename this entire Arc)", color = TextSecondary, fontSize = 11.sp) },
                                    placeholder = { Text("e.g. Indigo League", color = TextSecondary, fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentGold,
                                        unfocusedBorderColor = CardBorderDark,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 1-Click F2L Bot Importer for this Arc
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceVariantDark,
                            border = BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = AccentOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "1-Click F2L Bot Importer (For this Arc Only)",
                                            color = AccentOrange,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Mode Selector: Replace vs Merge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Option 1: Replace Arc (Recommended)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (replaceF2lArcMode) AccentOrange.copy(alpha = 0.2f) else SurfaceDark,
                                        border = BorderStroke(1.dp, if (replaceF2lArcMode) AccentOrange else CardBorderDark),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { replaceF2lArcMode = true }
                                            .bouncyTouch()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = if (replaceF2lArcMode) AccentOrange else TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Replace Arc (${arcEpisodes.size} → New)",
                                                color = if (replaceF2lArcMode) AccentOrange else TextSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = if (replaceF2lArcMode) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }

                                    // Option 2: Merge into Arc
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (!replaceF2lArcMode) AccentGold.copy(alpha = 0.2f) else SurfaceDark,
                                        border = BorderStroke(1.dp, if (!replaceF2lArcMode) AccentGold else CardBorderDark),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { replaceF2lArcMode = false }
                                            .bouncyTouch()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = if (!replaceF2lArcMode) AccentGold else TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Merge / Keep Rest",
                                                color = if (!replaceF2lArcMode) AccentGold else TextSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = if (!replaceF2lArcMode) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = f2lBatchInput,
                                        onValueChange = { f2lBatchInput = it },
                                        placeholder = { Text("Paste Arc F2L Batch ID or URL", color = TextSecondary, fontSize = 11.sp) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    val clip = clipboardManager.getText()?.text?.trim()
                                                    if (!clip.isNullOrBlank()) {
                                                        f2lBatchInput = clip
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = "Paste",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentOrange,
                                            unfocusedBorderColor = CardBorderDark,
                                            focusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = {
                                            if (f2lBatchInput.isBlank()) return@Button
                                            isFetchingF2l = true
                                            errorMessage = null
                                            successToast = null
                                            scope.launch {
                                                val res = F2lApiClient.fetchBatch(
                                                    batchInput = f2lBatchInput,
                                                    seasonNumber = targetSeasonNumber,
                                                    arcName = currentArcName.trim().ifBlank { targetArcName }
                                                )
                                                res.fold(
                                                    onSuccess = { fetchedEps ->
                                                        if (fetchedEps.isNotEmpty()) {
                                                            val resolvedArcName = currentArcName.trim().ifBlank { targetArcName }
                                                            val formattedEps = fetchedEps.map { ep ->
                                                                val resolved = EpisodeOrderingManager.resolveEffectiveEpisode(ep)
                                                                resolved.copy(
                                                                    seasonNumber = targetSeasonNumber,
                                                                    arcName = resolvedArcName
                                                                )
                                                            }.sortedBy { it.episodeNumber }

                                                            if (replaceF2lArcMode) {
                                                                val prevCount = arcEpisodes.size
                                                                arcEpisodes.clear()
                                                                arcEpisodes.addAll(formattedEps)
                                                                f2lBatchInput = ""
                                                                errorMessage = null
                                                                successToast = "Replaced Arc: Overwrote $prevCount old episodes with ${formattedEps.size} newly fetched episodes!"
                                                            } else {
                                                                val map = arcEpisodes.associateBy { it.episodeNumber }.toMutableMap()
                                                                formattedEps.forEach { ep -> map[ep.episodeNumber] = ep }
                                                                arcEpisodes.clear()
                                                                arcEpisodes.addAll(map.values.sortedBy { it.episodeNumber })
                                                                f2lBatchInput = ""
                                                                errorMessage = null
                                                                successToast = "Merged ${formattedEps.size} episodes into Arc (Total: ${arcEpisodes.size})"
                                                            }
                                                        } else {
                                                            errorMessage = "F2L API returned no episodes"
                                                        }
                                                    },
                                                    onFailure = { err ->
                                                        errorMessage = "F2L Fetch Failed: ${err.message}"
                                                    }
                                                )
                                                isFetchingF2l = false
                                            }
                                        },
                                        enabled = f2lBatchInput.isNotBlank() && !isFetchingF2l,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .height(50.dp)
                                            .bouncyTouch()
                                    ) {
                                        if (isFetchingF2l) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bolt,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Fetch Arc",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Smart Single / Multi Snippet Paste Box
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceVariantDark,
                            border = BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Paste Missing Episode JSON Snippet or Links",
                                        color = AccentGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Paste single episode JSON object (e.g. EP 16), batch message, or links. It will be placed into the arc automatically.",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = rawSnippetText,
                                    onValueChange = { rawSnippetText = it },
                                    placeholder = {
                                        Text(
                                            text = "{\n  \"episode_num\": 16,\n  \"title\": \"EP - 16 - The Encounter, Abarai Renji!\",\n  \"stream_link\": \"https://...\"\n}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    },
                                    minLines = 3,
                                    maxLines = 6,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentGold,
                                        unfocusedBorderColor = CardBorderDark,
                                        focusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            errorMessage = null
                                            successToast = null
                                            val textToParse = rawSnippetText.ifBlank {
                                                clipboardManager.getText()?.text?.trim() ?: ""
                                            }
                                            if (textToParse.isBlank()) {
                                                errorMessage = "Paste box and clipboard are empty"
                                                return@Button
                                            }

                                            // Attempt single object JSON wrapping if wrapped in {}
                                            val jsonCandidate = if (textToParse.startsWith("{") && !textToParse.startsWith("[")) {
                                                "[$textToParse]"
                                            } else {
                                                textToParse
                                            }

                                            val resolvedArcName = currentArcName.trim().ifBlank { targetArcName }
                                            val parsed = BatchEpisodeParser.parseRawDump(jsonCandidate, targetSeasonNumber, resolvedArcName)
                                                .ifEmpty { StreamLinkResolver.parseSmartBotMessageOrLinks(jsonCandidate, targetSeasonNumber, resolvedArcName) }

                                            if (parsed.isNotEmpty()) {
                                                val formattedEps = parsed.map { ep ->
                                                    val resolved = EpisodeOrderingManager.resolveEffectiveEpisode(ep)
                                                    resolved.copy(
                                                        seasonNumber = targetSeasonNumber,
                                                        arcName = resolvedArcName
                                                    )
                                                }
                                                if (replaceSnippetMode) {
                                                    val prevCount = arcEpisodes.size
                                                    arcEpisodes.clear()
                                                    arcEpisodes.addAll(formattedEps.sortedBy { it.episodeNumber })
                                                    rawSnippetText = ""
                                                    errorMessage = null
                                                    successToast = "Replaced Arc: Overwrote $prevCount old episodes with ${formattedEps.size} parsed episodes!"
                                                } else {
                                                    val map = arcEpisodes.associateBy { it.episodeNumber }.toMutableMap()
                                                    formattedEps.forEach { ep -> map[ep.episodeNumber] = ep }
                                                    arcEpisodes.clear()
                                                    arcEpisodes.addAll(map.values.sortedBy { it.episodeNumber })
                                                    rawSnippetText = ""
                                                    errorMessage = null
                                                    successToast = "Inserted ${formattedEps.size} episode(s) into Arc! (Total: ${arcEpisodes.size})"
                                                }
                                            } else {
                                                errorMessage = "Could not parse episode from snippet/link"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                brush = Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF38BDF8))),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .bouncyTouch()
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Paste & Insert into Arc",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (rawSnippetText.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = { rawSnippetText = "" },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                            border = BorderStroke(1.dp, CardBorderDark),
                                            modifier = Modifier.bouncyTouch()
                                        ) {
                                            Text("Clear", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Episodes in this Arc List Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episodes in this Arc (${arcEpisodes.size})",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (arcEpisodes.isNotEmpty()) {
                                    // Health Check Action
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCheckingHealth) AccentOrange.copy(alpha = 0.2f) else if ((healthReport?.deadCount ?: 0) > 0) PrimaryRed.copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, if (isCheckingHealth) AccentOrange else if ((healthReport?.deadCount ?: 0) > 0) PrimaryRed else Color(0xFF10B981)),
                                        modifier = Modifier
                                            .clickable(enabled = !isCheckingHealth && arcEpisodes.isNotEmpty()) {
                                                isCheckingHealth = true
                                                scope.launch {
                                                    StreamHealthChecker.checkEpisodesHealth(arcEpisodes).collect { report ->
                                                        healthReport = report
                                                        if (!report.isChecking) {
                                                            isCheckingHealth = false
                                                        }
                                                    }
                                                }
                                            }
                                            .bouncyTouch()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            if (isCheckingHealth) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(10.dp),
                                                    color = AccentOrange,
                                                    strokeWidth = 1.5.dp
                                                )
                                                Text(
                                                    text = "Checking (${healthReport?.checkedCount ?: 0}/${arcEpisodes.size})...",
                                                    color = AccentOrange,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Text(
                                                    text = if (healthReport != null) "🩺 ${healthReport?.aliveCount} Live / ${healthReport?.deadCount} Dead" else "🩺 Check Links",
                                                    color = if ((healthReport?.deadCount ?: 0) > 0) PrimaryRed else Color(0xFF10B981),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Clear All Episodes Action Button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PrimaryRed.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, PrimaryRed.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .clickable { showClearArcDialog = true }
                                            .bouncyTouch()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteForever,
                                                contentDescription = "Clear All",
                                                tint = PrimaryRed,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Clear All",
                                                color = PrimaryRed,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (arcEpisodes.isNotEmpty()) {
                                    Text(
                                        text = "Sorted 1..N",
                                        color = AccentGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (arcEpisodes.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceVariantDark,
                                border = BorderStroke(1.dp, CardBorderDark),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No episodes in this Arc yet",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Use the 1-Click F2L Bot Importer or snippet paste box above to populate this Arc.",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(arcEpisodes, key = { _, ep -> "${ep.seasonNumber}_${ep.episodeNumber}_${ep.title}" }) { index, ep ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceVariantDark,
                            border = BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentGold.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "EP ${ep.episodeNumber.toString().padStart(2, '0')}",
                                        color = AccentGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val cleanTitle = StreamLinkResolver.cleanEpisodeTitle(ep.title, ep.episodeNumber)
                                    Text(
                                        text = cleanTitle.ifBlank { ep.fileName },
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val specs = listOfNotNull(
                                        ep.fileSize.takeIf { it.isNotBlank() },
                                        if (ep.durationMs > 0L) "${ep.durationMs / 60000}m" else null
                                    ).joinToString(" • ")

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (specs.isNotBlank()) {
                                            Text(specs, color = TextSecondary, fontSize = 10.sp)
                                        }

                                        val key = "${ep.seasonNumber}_${ep.arcName}_${ep.episodeNumber}"
                                        val epHealth = healthReport?.results?.get(key)
                                        if (epHealth != null) {
                                            Text(
                                                text = if (epHealth.isAlive) "🟢 ${epHealth.httpCode} (${epHealth.latencyMs}ms)" else "🔴 ${epHealth.errorMessage ?: "HTTP ${epHealth.httpCode}"}",
                                                color = if (epHealth.isAlive) Color(0xFF81C784) else Color(0xFFFF5252),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { arcEpisodes.removeAt(index) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .bouncyTouch()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Episode",
                                        tint = PrimaryRed.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bouncyTouch()
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val finalArcName = currentArcName.trim().ifBlank { targetArcName }
                            val updatedEpisodes = arcEpisodes.map { ep ->
                                ep.copy(arcName = finalArcName)
                            }
                            onSaveArcEpisodes(updatedEpisodes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier
                            .weight(1.8f)
                            .height(48.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(PrimaryRed, AccentOrange)),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .bouncyTouch()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Arc (${arcEpisodes.size} Episodes)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
