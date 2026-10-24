package com.streamhub.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.StreamBackendConfig
import com.streamhub.app.data.StreamLinkResolver
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Tab 1: Episodes, Multi-Arc Stream Links & F2L Importer for Creator Studio.
 * Extracted into a standalone composable to isolate bytecode and DEX registers.
 */
@Composable
fun AdminLinksTab(
    state: AdminEditorState,
    scope: CoroutineScope
) {
    if (state.isMovieFormat) {
        // --- MOVIE FORMAT: SMART STREAM LINK & READY PREVIEW (SERIES-PARITY) ---
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        Text("2. Movie Direct Stream Link", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Paste the direct stream URL, Serv00 link, or direct media link for this movie.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        val isF2lLink = StreamBackendConfig.isBackendHost(state.startBatchLink)
        val isF2lTruncated = isF2lLink && (
            (state.startBatchLink.contains("/dl/") && state.startBatchLink.substringAfter("/dl/").trim().length < 32) ||
            (state.startBatchLink.contains("/stream/") && state.startBatchLink.substringAfter("/stream/").trim().length < 32)
        )

        OutlinedTextField(
            value = state.startBatchLink,
            onValueChange = {
                state.startBatchLink = it
                state.generatedEpisodesText = it
                state.batchError = null
                state.movieLinkProbeResult = null
            },
            label = { Text("Movie Stream URL / Direct Link *", color = TextSecondary) },
            placeholder = { Text("https://midnighthawk.serv00.net/dl/xxx or direct link", color = TextSecondary) },
            singleLine = true,
            isError = isF2lTruncated,
            modifier = Modifier.fillMaxWidth()
        )

        if (isF2lTruncated) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "⚠️ Incomplete Link: The F2L code appears truncated. Please paste the full 48-character link from the bot.",
                color = Color(0xFFFFB74D),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    state.batchError = null
                    val clipText = clipboardManager.getText()?.text?.trim()
                    if (clipText.isNullOrBlank()) {
                        state.batchError = "Clipboard is empty"
                        return@Button
                    }
                    val cleanLink = StreamLinkResolver.sanitizePlayableUrl(clipText)
                    state.startBatchLink = cleanLink
                    state.generatedEpisodesText = cleanLink
                    state.movieLinkProbeResult = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = CircleShape,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paste from Clipboard", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (state.startBatchLink.isNotBlank() || state.generatedEpisodesText.isNotBlank()) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryRed.copy(alpha = 0.15f),
                    modifier = Modifier
                        .bouncyTouch()
                        .clip(CircleShape)
                        .clickable {
                            state.startBatchLink = ""
                            state.generatedEpisodesText = ""
                            state.fetchedFileName = ""
                            state.fetchedDurationMs = 0L
                            state.batchError = null
                            state.movieLinkProbeResult = null
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = PrimaryRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        state.batchError?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, color = PrimaryRed, fontSize = 11.sp)
        }

        // Movie Stream Ready Preview Card (Series-Parity)
        val cleanActiveLink = StreamLinkResolver.sanitizePlayableUrl(state.startBatchLink.ifBlank { state.generatedEpisodesText }.trim())
        if (cleanActiveLink.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "1 Movie Stream Ready",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Health Check Action (Series-Parity: on-demand, non-blocking)
                        Surface(
                            shape = CircleShape,
                            color = if (state.isTestingMovieLink) Color(0xFFFF9800).copy(alpha = 0.2f)
                                    else if (state.movieLinkProbeResult?.isAlive == true) Color(0xFF10B981).copy(alpha = 0.2f)
                                    else if (state.movieLinkProbeResult?.isAlive == false) PrimaryRed.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(enabled = !state.isTestingMovieLink) {
                                    state.isTestingMovieLink = true
                                    scope.launch {
                                        val result = com.streamhub.app.data.api.StreamHealthChecker.probeUrl(cleanActiveLink)
                                        state.movieLinkProbeResult = result
                                        state.isTestingMovieLink = false
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                if (state.isTestingMovieLink) {
                                    ExpressiveLoadingIndicator(
                                        modifier = Modifier.size(11.dp),
                                        color = Color(0xFFFF9800),
                                        accentColor = AccentGold,
                                        size = 11.dp
                                    )
                                    Text(
                                        "Checking...",
                                        color = Color(0xFFFF9800),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = when {
                                            state.movieLinkProbeResult?.isAlive == true -> "Live (${state.movieLinkProbeResult?.latencyMs}ms)"
                                            state.movieLinkProbeResult?.isAlive == false -> "Unreachable (${state.movieLinkProbeResult?.errorMessage ?: "Error"})"
                                            else -> "Check Link"
                                        },
                                        color = when {
                                            state.movieLinkProbeResult?.isAlive == true -> Color(0xFF10B981)
                                            state.movieLinkProbeResult?.isAlive == false -> PrimaryRed
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stream link metadata row
                    val isBackend = StreamBackendConfig.isBackendHost(cleanActiveLink)
                    val streamBadge = when {
                        isBackend && cleanActiveLink.contains("/dl/") -> "Serv00 Direct Stream"
                        isBackend && cleanActiveLink.contains("/stream/") -> "F2L Direct Stream"
                        cleanActiveLink.contains("t.me/") -> "Telegram Direct"
                        else -> "Web Stream URL"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = state.title.ifBlank { "Main Feature Movie" },
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF38BDF8).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = streamBadge,
                                            color = Color(0xFF38BDF8),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cleanActiveLink,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- SERIES / ANIME FORMAT: SMART F2L, RAW TEXT PARSER & SEQUENTIAL GENERATOR ---
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val parsedSeasonNum = state.seasonNumberText.toIntOrNull() ?: 1

        Text("2. Stream Links & Episodes Importer", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Paste raw links, batch messages, or 1-Click F2L Bot Batch ID to import episodes.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Season / Arc configuration for Anime / Sagas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.seasonNumberText,
                onValueChange = { state.seasonNumberText = it },
                label = { Text("Season # *", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.arcNameText,
                onValueChange = { state.arcNameText = it },
                label = { Text("Arc / Saga Name (Optional)", color = TextSecondary) },
                placeholder = { Text("e.g. Shibuya Incident Arc", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.weight(2f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 1-CLICK F2L REST API IMPORTER ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1-Click F2L Bot REST API Importer",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.f2lBatchInput,
                        onValueChange = { state.f2lBatchInput = it; state.batchError = null },
                        label = { Text("F2L Batch ID or URL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                        placeholder = { Text("e.g. eb76ab230b4d...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (state.f2lBatchInput.isBlank()) {
                                state.batchError = "Please enter Batch ID or URL"
                                return@Button
                            }
                            state.isFetchingF2l = true
                            state.batchError = null
                            state.batchSuccess = null
                            scope.launch {
                                val res = com.streamhub.app.data.api.F2lApiClient.fetchBatch(state.f2lBatchInput, parsedSeasonNum, state.arcNameText)
                                res.fold(
                                    onSuccess = { eps ->
                                        if (eps.isNotEmpty()) {
                                            val merged = com.streamhub.app.data.EpisodeOrderingManager.mergeAndDeduplicate(state.currentEpisodes.toList(), eps)
                                            state.currentEpisodes.clear()
                                            state.currentEpisodes.addAll(merged)
                                            val jsonDump = com.streamhub.app.data.parser.BatchEpisodeParser.toJsonString(state.currentEpisodes)
                                            state.startBatchLink = jsonDump
                                            state.generatedEpisodesText = jsonDump
                                            state.f2lBatchInput = ""
                                            state.batchSuccess = "Fetched & Loaded ${eps.size} episodes! Check JSON below."
                                        } else {
                                            state.batchError = "No episodes returned by F2L API"
                                        }
                                    },
                                    onFailure = { err ->
                                        state.batchError = "F2L API Import Failed: ${err.message}"
                                    }
                                )
                                state.isFetchingF2l = false
                            }
                        },
                        enabled = state.f2lBatchInput.isNotBlank() && !state.isFetchingF2l,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = CircleShape,
                        modifier = Modifier
                            .height(50.dp)
                            .bouncyTouch()
                    ) {
                        if (state.isFetchingF2l) {
                            ExpressiveLoadingIndicator(size = 14.dp, color = Color.White, accentColor = AccentGold)
                        } else {
                            Text("Fetch API", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Smart F2L Bot Output / JSON / Links Input Area
        OutlinedTextField(
            value = state.generatedEpisodesText.ifBlank { state.startBatchLink },
            onValueChange = { newText ->
                state.startBatchLink = newText
                state.generatedEpisodesText = newText
                state.batchError = null
                state.batchSuccess = null

                val trimmed = newText.trim()
                val isBatchUrl = (trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
                                 (trimmed.contains("/batch/", ignoreCase = true) || trimmed.contains("api/batch", ignoreCase = true))
                if (isBatchUrl && !state.isFetchingF2l) {
                    state.f2lBatchInput = trimmed
                    state.isFetchingF2l = true
                    scope.launch {
                        val res = com.streamhub.app.data.api.F2lApiClient.fetchBatch(trimmed, parsedSeasonNum, state.arcNameText)
                        res.fold(
                            onSuccess = { eps ->
                                if (eps.isNotEmpty()) {
                                    val merged = com.streamhub.app.data.EpisodeOrderingManager.mergeAndDeduplicate(state.currentEpisodes.toList(), eps)
                                    state.currentEpisodes.clear()
                                    state.currentEpisodes.addAll(merged)
                                    val jsonDump = com.streamhub.app.data.parser.BatchEpisodeParser.toJsonString(state.currentEpisodes)
                                    state.startBatchLink = jsonDump
                                    state.generatedEpisodesText = jsonDump
                                    state.batchSuccess = "Fetched & Loaded ${eps.size} episodes! Check JSON below."
                                }
                            },
                            onFailure = { err ->
                                state.batchError = "F2L API Import: ${err.message}"
                            }
                        )
                        state.isFetchingF2l = false
                    }
                }
            },
            label = { Text("Smart Raw Dump / Episode Links / Batch Text *", color = TextSecondary) },
            placeholder = { Text("Paste raw links, batch messages, or JSON here...\n> Ep 01: https://cdn.example.com/ep01.mp4\n> Ep 02: https://cdn.example.com/ep02.mp4", color = TextSecondary) },
            minLines = 4,
            maxLines = 8,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color(0xFF2C2C3E),
                focusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    state.batchError = null
                    state.batchSuccess = null
                    val clipText = clipboardManager.getText()?.text?.trim()
                    if (clipText.isNullOrBlank()) {
                        state.batchError = "Clipboard is empty"
                        return@Button
                    }

                    val isBatchUrlOrId = clipText.contains("/batch/", ignoreCase = true) ||
                                         clipText.contains("api/batch", ignoreCase = true) ||
                                         (clipText.length in 32..64 && clipText.matches(Regex("^[a-fA-F0-9]+$")))

                    if (isBatchUrlOrId) {
                        state.f2lBatchInput = clipText
                        state.isFetchingF2l = true
                        scope.launch {
                            val res = com.streamhub.app.data.api.F2lApiClient.fetchBatch(clipText, parsedSeasonNum, state.arcNameText)
                            res.fold(
                                onSuccess = { eps ->
                                    if (eps.isNotEmpty()) {
                                        val merged = com.streamhub.app.data.EpisodeOrderingManager.mergeAndDeduplicate(state.currentEpisodes.toList(), eps)
                                        state.currentEpisodes.clear()
                                        state.currentEpisodes.addAll(merged)
                                        val jsonDump = com.streamhub.app.data.parser.BatchEpisodeParser.toJsonString(state.currentEpisodes)
                                        state.startBatchLink = jsonDump
                                        state.generatedEpisodesText = jsonDump
                                        state.batchSuccess = "Fetched & Loaded ${eps.size} episodes! Check JSON below."
                                    } else {
                                        state.batchError = "No episodes returned by F2L API"
                                    }
                                },
                                onFailure = { err ->
                                    state.batchError = "F2L API Import Failed: ${err.message}"
                                }
                            )
                            state.isFetchingF2l = false
                        }
                        return@Button
                    }

                    val parsed = com.streamhub.app.data.parser.BatchEpisodeParser.parseRawDump(clipText, parsedSeasonNum, state.arcNameText)
                        .ifEmpty { StreamLinkResolver.parseSmartBotMessageOrLinks(clipText, parsedSeasonNum, state.arcNameText) }
                    if (parsed.isNotEmpty()) {
                        val merged = com.streamhub.app.data.EpisodeOrderingManager.mergeAndDeduplicate(state.currentEpisodes.toList(), parsed)
                        state.currentEpisodes.clear()
                        state.currentEpisodes.addAll(merged)
                        state.startBatchLink = clipText
                        state.generatedEpisodesText = clipText
                        state.batchSuccess = "Parsed & Loaded ${parsed.size} episodes! Check JSON below."
                    } else {
                        state.batchError = "Could not parse episodes from clipboard text."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = CircleShape,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paste & Auto-Parse", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (state.startBatchLink.isNotBlank() || state.generatedEpisodesText.isNotBlank() || state.currentEpisodes.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryRed.copy(alpha = 0.15f),
                    modifier = Modifier
                        .bouncyTouch()
                        .clip(CircleShape)
                        .clickable {
                            state.startBatchLink = ""
                            state.endBatchLink = ""
                            state.generatedEpisodesText = ""
                            state.currentEpisodes.clear()
                            state.batchError = null
                            state.batchSuccess = null
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = PrimaryRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        state.batchError?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, color = PrimaryRed, fontSize = 11.sp)
        }

        state.batchSuccess?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Live Parsed Episodes Preview & Batch Tools
        if (state.currentEpisodes.isNotEmpty()) {
            val validation = remember(state.currentEpisodes.size) {
                com.streamhub.app.data.parser.BatchEpisodeParser.validateEpisodes(state.currentEpisodes)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (validation.isValid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (validation.isValid) Color(0xFF10B981) else Color(0xFFFFB74D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (validation.isValid) "${state.currentEpisodes.size} Episodes Ready" else "${state.currentEpisodes.size} Episodes Ready (${validation.warningMessages.size} Warnings)",
                                color = if (validation.isValid) Color(0xFF81C784) else Color(0xFFFFB74D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Health Check Action
                            Surface(
                                shape = CircleShape,
                                color = if (state.isCheckingHealth) Color(0xFFFF9800).copy(alpha = 0.2f) else if ((state.healthReport?.deadCount ?: 0) > 0) PrimaryRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = !state.isCheckingHealth && state.currentEpisodes.isNotEmpty()) {
                                        state.isCheckingHealth = true
                                        scope.launch {
                                            com.streamhub.app.data.api.StreamHealthChecker.checkEpisodesHealth(state.currentEpisodes).collect { report ->
                                                state.healthReport = report
                                                if (!report.isChecking) {
                                                    state.isCheckingHealth = false
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    if (state.isCheckingHealth) {
                                        ExpressiveLoadingIndicator(
                                            modifier = Modifier.size(11.dp),
                                            color = Color(0xFFFF9800),
                                            accentColor = AccentGold,
                                            size = 11.dp
                                        )
                                        Text(
                                            "Checking (${state.healthReport?.checkedCount ?: 0}/${state.currentEpisodes.size})...",
                                            color = Color(0xFFFF9800),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = if (state.healthReport != null) "${state.healthReport?.aliveCount} Live / ${state.healthReport?.deadCount} Dead" else "Check Links",
                                            color = if ((state.healthReport?.deadCount ?: 0) > 0) PrimaryRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Quick Batch Renumbering Action
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val sortedBase = com.streamhub.app.data.EpisodeOrderingManager.normalizeAndSort(state.currentEpisodes)
                                        val renumbered = sortedBase.groupBy { it.seasonNumber }.flatMap { (_, epList) ->
                                            epList.mapIndexed { idx, ep ->
                                                ep.copy(
                                                    episodeNumber = idx + 1,
                                                    title = if (ep.title.matches(Regex("^Episode \\d+$", RegexOption.IGNORE_CASE))) "Episode ${idx + 1}" else ep.title
                                                )
                                            }
                                        }
                                        state.currentEpisodes.clear()
                                        state.currentEpisodes.addAll(renumbered)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                                    Text(
                                        "Re-Index",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (validation.warningMessages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            validation.warningMessages.forEach { msg ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(12.dp))
                                    Text(msg, color = Color(0xFFFFB74D), fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    state.healthReport?.let { hr ->
                        if (hr.deadCount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(12.dp))
                                Text(
                                    "${hr.deadCount} stream link(s) broken/dead!",
                                    color = PrimaryRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        state.currentEpisodes.take(5).forEachIndexed { idx, ep ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "E${ep.episodeNumber.toString().padStart(2, '0')}",
                                        color = AccentGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = ep.title.removePrefix("Ep ${ep.episodeNumber}: ").ifBlank { ep.fileName },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                val durationText = if (ep.durationMs > 0) {
                                    val totalSec = (ep.durationMs / 1000).toInt()
                                    val h = totalSec / 3600
                                    val m = (totalSec % 3600) / 60
                                    val s = totalSec % 60
                                    if (h > 0) {
                                        String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
                                    } else {
                                        String.format(java.util.Locale.US, "%02d:%02d", m, s)
                                    }
                                } else ""
                                val metaChips = listOfNotNull(
                                    durationText.takeIf { it.isNotBlank() },
                                    ep.fileSize.takeIf { it.isNotBlank() }
                                ).joinToString(" • ")

                                if (metaChips.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = metaChips,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                val key = "${ep.seasonNumber}_${ep.arcName}_${ep.episodeNumber}"
                                val epHealth = state.healthReport?.results?.get(key)
                                if (epHealth != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (epHealth.isAlive) Color(0xFF10B981).copy(alpha = 0.15f) else PrimaryRed.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (epHealth.isAlive) "${epHealth.httpCode} (${epHealth.latencyMs}ms)" else (epHealth.errorMessage ?: "HTTP ${epHealth.httpCode}"),
                                            color = if (epHealth.isAlive) Color(0xFF10B981) else PrimaryRed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (state.currentEpisodes.size > 5) {
                            Text(
                                text = "+ ${state.currentEpisodes.size - 5} more episodes",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
