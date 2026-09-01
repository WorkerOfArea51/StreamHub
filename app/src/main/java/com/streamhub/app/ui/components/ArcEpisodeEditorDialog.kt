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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WarningAmber
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
import com.streamhub.app.data.SeasonArcOption
import com.streamhub.app.data.TelegramLinkResolver
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.parser.BatchEpisodeParser
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
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

    var rawSnippetText by remember { mutableStateOf("") }
    var f2lBatchInput by remember { mutableStateOf("") }
    var isFetchingF2l by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successToast by remember { mutableStateOf<String?>(null) }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF13131D),
            border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f)),
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C4DFF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = Color(0xFFB388FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = arcOption.title.ifBlank { "Edit Arc Episodes" },
                                color = Color.White,
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Missing Gap Warning Banner
                if (missingGaps.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x33FF9800),
                        border = BorderStroke(1.dp, AccentOrange),
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

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1-Click F2L Bot Importer for this Arc
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚡ 1-Click F2L Bot Importer (For this Arc Only)",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = f2lBatchInput,
                                        onValueChange = { f2lBatchInput = it },
                                        placeholder = { Text("Paste Arc F2L Batch ID or URL", color = TextSecondary, fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = Color(0xFF2C2C3E),
                                            focusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = {
                                            if (f2lBatchInput.isBlank()) return@Button
                                            isFetchingF2l = true
                                            errorMessage = null
                                            scope.launch {
                                                val res = com.streamhub.app.data.api.F2lApiClient.fetchBatch(
                                                    batchInput = f2lBatchInput,
                                                    seasonNumber = targetSeasonNumber,
                                                    arcName = targetArcName
                                                )
                                                res.fold(
                                                    onSuccess = { fetchedEps ->
                                                        if (fetchedEps.isNotEmpty()) {
                                                            val map = arcEpisodes.associateBy { it.episodeNumber }.toMutableMap()
                                                            fetchedEps.forEach { ep ->
                                                                map[ep.episodeNumber] = ep.copy(
                                                                    seasonNumber = targetSeasonNumber,
                                                                    arcName = targetArcName
                                                                )
                                                            }
                                                            arcEpisodes.clear()
                                                            arcEpisodes.addAll(map.values.sortedBy { it.episodeNumber })
                                                            f2lBatchInput = ""
                                                            successToast = "Fetched ${fetchedEps.size} episodes for this Arc"
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
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(50.dp)
                                    ) {
                                        if (isFetchingF2l) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Text("Fetch Arc", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Smart Single / Multi Snippet Paste Box
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "➕ Paste Missing Episode JSON Snippet or Links",
                                    color = Color(0xFFB388FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Paste single episode JSON object (e.g. EP 16) or links. It will be placed into the arc automatically.",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = rawSnippetText,
                                    onValueChange = { rawSnippetText = it },
                                    placeholder = { Text("{\n  \"episode_num\": 16,\n  \"title\": \"EP - 16 - The Encounter, Abarai Renji!\",\n  \"stream_link\": \"https://...\"\n}", color = TextSecondary, fontSize = 11.sp) },
                                    minLines = 3,
                                    maxLines = 6,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF7C4DFF),
                                        unfocusedBorderColor = Color(0xFF2C2C3E),
                                        focusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            errorMessage = null
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

                                            val parsed = BatchEpisodeParser.parseRawDump(jsonCandidate, targetSeasonNumber, targetArcName)
                                                .ifEmpty { TelegramLinkResolver.parseSmartBotMessageOrLinks(jsonCandidate, targetSeasonNumber, targetArcName) }

                                            if (parsed.isNotEmpty()) {
                                                val map = arcEpisodes.associateBy { it.episodeNumber }.toMutableMap()
                                                parsed.forEach { ep ->
                                                    map[ep.episodeNumber] = ep.copy(
                                                        seasonNumber = targetSeasonNumber,
                                                        arcName = targetArcName
                                                    )
                                                }
                                                arcEpisodes.clear()
                                                arcEpisodes.addAll(map.values.sortedBy { it.episodeNumber })
                                                rawSnippetText = ""
                                                successToast = "Inserted ${parsed.size} episode(s) into Arc!"
                                            } else {
                                                errorMessage = "Could not parse episode from snippet/link"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Paste & Insert into Arc", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (rawSnippetText.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = { rawSnippetText = "" },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                            border = BorderStroke(1.dp, Color(0x44FFFFFF))
                                        ) {
                                            Text("Clear", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    errorMessage?.let { msg ->
                        item {
                            Text(msg, color = PrimaryRed, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    successToast?.let { msg ->
                        item {
                            Text(msg, color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Episodes in this Arc List
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episodes in this Arc (${arcEpisodes.size})",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

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

                    itemsIndexed(arcEpisodes, key = { _, ep -> "${ep.seasonNumber}_${ep.episodeNumber}_${ep.title}" }) { index, ep ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceDark,
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
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF7C4DFF).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "EP ${ep.episodeNumber.toString().padStart(2, '0')}",
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val cleanTitle = TelegramLinkResolver.cleanEpisodeTitle(ep.title, ep.episodeNumber)
                                    Text(
                                        text = cleanTitle.ifBlank { ep.fileName },
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val specs = listOfNotNull(
                                        ep.fileSize.takeIf { it.isNotBlank() },
                                        if (ep.durationMs > 0L) "${ep.durationMs / 60000}m" else null
                                    ).joinToString(" • ")

                                    if (specs.isNotBlank()) {
                                        Text(specs, color = TextSecondary, fontSize = 10.sp)
                                    }
                                }

                                IconButton(
                                    onClick = { arcEpisodes.removeAt(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Episode",
                                        tint = Color(0x66FF5252),
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onSaveArcEpisodes(arcEpisodes.toList())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier
                            .weight(1.8f)
                            .height(46.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(Color(0xFF7C4DFF), Color(0xFF651FFF))),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
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
