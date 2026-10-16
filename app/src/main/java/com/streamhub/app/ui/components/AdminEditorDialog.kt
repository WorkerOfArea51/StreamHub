package com.streamhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streamhub.app.data.TelegramLinkResolver
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch
import kotlinx.coroutines.launch

/**
 * Modernized VIP Creator & Admin Studio Dialog.
 *
 * Decomposed into isolated tab composables (AdminOverviewTab, AdminLinksTab, AdminSpecsTab, AdminToolsTab)
 * to prevent DEX register allocation overflow and ART runtime VerifyError.
 */
@Composable
fun AdminEditorDialog(
    initialItem: MediaItem? = null,
    existingIds: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onSave: (MediaItem) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val state = rememberAdminEditorState(initialItem)
    var selectedTab by remember { mutableIntStateOf(0) }

    val itemForDeletion = state.activeEditItem ?: initialItem
    if (state.showDeleteConfirmDialog && itemForDeletion != null) {
        AlertDialog(
            onDismissRequest = { state.showDeleteConfirmDialog = false },
            title = {
                Text(
                    "Delete Media?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete \"${itemForDeletion.title}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        state.showDeleteConfirmDialog = false
                        if (onDelete != null) {
                            onDelete(itemForDeletion.id)
                        } else {
                            scope.launch {
                                FirebaseRepository.getInstance().deleteMediaItem(itemForDeletion.id)
                            }
                        }
                        onDismiss()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { state.showDeleteConfirmDialog = false },
                    shape = CircleShape
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    if (state.showBackupDialog) {
        CatalogBackupDialog(
            repository = FirebaseRepository.getInstance(),
            onDismiss = { state.showBackupDialog = false }
        )
    }

    if (state.showMigrationDialog) {
        ServerMigrationDialog(
            repository = FirebaseRepository.getInstance(),
            onDismiss = { state.showMigrationDialog = false }
        )
    }

    if (state.showVoucherDialog) {
        VoucherManagerDialog(
            onDismiss = { state.showVoucherDialog = false }
        )
    }

    if (state.showMetadataInspector) {
        MetadataInspectorDialog(
            repository = FirebaseRepository.getInstance(),
            onDismiss = { state.showMetadataInspector = false },
            onEditShow = { itemToEdit ->
                state.showMetadataInspector = false
                state.loadItem(itemToEdit)
                selectedTab = 0
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar (Title + Close Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (state.activeEditItem != null || initialItem != null) "Edit Media Catalog" else "Creator Studio",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Professional anime & movie publication dashboard",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
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

                // Custom Tab Switcher Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabButton(
                        icon = Icons.Default.Star,
                        title = "Overview",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    TabButton(
                        icon = Icons.Default.Link,
                        title = if (state.isMovieFormat) "Movie Stream" else "Episodes (${state.currentEpisodes.size})",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    TabButton(
                        icon = Icons.Default.Tune,
                        title = "Specs",
                        isSelected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    TabButton(
                        icon = Icons.Default.Build,
                        title = "Tools",
                        isSelected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content Body with Isolated Tabs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> AdminOverviewTab(state = state, scope = scope)
                        1 -> AdminLinksTab(state = state, scope = scope)
                        2 -> AdminSpecsTab(state = state)
                        3 -> AdminToolsTab(state = state)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                state.validationError?.let {
                    Text(it, color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (selectedTab != 3) {
                    // ==========================================
                    // BOTTOM SAVE BAR (Fixed Footer)
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                contentColor = TextSecondary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(46.dp)
                                .bouncyTouch()
                        ) {
                            Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        if (state.activeEditItem != null || initialItem != null) {
                            Button(
                                onClick = {
                                    state.showDeleteConfirmDialog = true
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryRed.copy(alpha = 0.15f),
                                    contentColor = PrimaryRed
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .bouncyTouch()
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = PrimaryRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", color = PrimaryRed, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        Button(
                            onClick = {
                                state.validationError = null
                                when {
                                    state.title.isBlank() -> state.validationError = "Title is required (Type title in Tab 1)"
                                    state.category.isBlank() -> state.validationError = "Category is required"
                                    else -> {
                                        val isMovieItem = state.isMovieFormat ||
                                                state.relationType.equals("Movie", ignoreCase = true) ||
                                                state.category.equals("Movie", ignoreCase = true) ||
                                                state.category.equals("Movies", ignoreCase = true)
                                        val parsedSeasonNum = state.seasonNumberText.trim().toIntOrNull() ?: if (isMovieItem) 0 else 1
                                        val episodes = when {
                                            state.isMovieFormat && state.currentEpisodes.size <= 1 && (state.startBatchLink.isNotBlank() || state.generatedEpisodesText.isNotBlank()) -> {
                                                val link = TelegramLinkResolver.sanitizePlayableUrl(state.startBatchLink.ifBlank { state.generatedEpisodesText }.trim())
                                                val existingEp = initialItem?.episodes?.firstOrNull()
                                                listOf(
                                                    Episode(
                                                        episodeNumber = 1,
                                                        seasonNumber = if (isMovieItem) 0 else 1,
                                                        title = state.title.ifBlank { "Movie Stream" },
                                                        streamUrl = link,
                                                        mirrorStreamUrl = link,
                                                        telegramFileId = TelegramLinkResolver.extractTelegramMessageOrFileId(link),
                                                        fileSize = state.fileSize.ifBlank { existingEp?.fileSize ?: "" },
                                                        durationMs = if (state.fetchedDurationMs > 0) state.fetchedDurationMs else (existingEp?.durationMs ?: 0L),
                                                        fileName = state.fetchedFileName.ifBlank { existingEp?.fileName ?: "" }
                                                    )
                                                )
                                            }
                                            else -> {
                                                val pendingText = state.generatedEpisodesText.ifBlank { state.startBatchLink }.trim()
                                                if (pendingText.isNotBlank()) {
                                                    val parsedPending = com.streamhub.app.data.parser.BatchEpisodeParser.parseRawDump(pendingText, defaultSeason = parsedSeasonNum, defaultArc = state.arcNameText)
                                                        .ifEmpty { TelegramLinkResolver.parseSmartBotMessageOrLinks(pendingText, seasonNumber = parsedSeasonNum, arcName = state.arcNameText) }
                                                    if (parsedPending.isNotEmpty()) {
                                                        com.streamhub.app.data.EpisodeOrderingManager.mergeAndDeduplicate(state.currentEpisodes.toList(), parsedPending)
                                                    } else {
                                                        state.currentEpisodes.toList()
                                                    }
                                                } else {
                                                    state.currentEpisodes.toList()
                                                }
                                            }
                                        }

                                        val generatedId = generateReadableMediaId(
                                            title = state.title,
                                            releaseYear = state.premiered.take(4),
                                            seasonTitle = state.seasonTitle,
                                            partNumber = state.partNumberText.toIntOrNull() ?: 0
                                        )

                                        val editTarget = state.activeEditItem ?: initialItem
                                        val finalId = if (editTarget != null && editTarget.id.isNotBlank()) {
                                            editTarget.id
                                        } else {
                                            var uniqueId = generatedId
                                            var suffix = 2
                                            while (existingIds.contains(uniqueId)) {
                                                uniqueId = "${generatedId}_$suffix"
                                                suffix++
                                            }
                                            uniqueId
                                        }

                                        val cleanTitle = state.title.trim()
                                        val finalFranchiseId = state.franchiseId.ifBlank {
                                            com.streamhub.app.data.FranchiseManager.getFranchiseId(MediaItem(title = cleanTitle))
                                        }
                                        val finalFranchiseTitle = state.franchiseTitle.ifBlank {
                                            com.streamhub.app.data.FranchiseManager.getFranchiseTitle(MediaItem(title = cleanTitle))
                                        }

                                        val baseItem = state.activeEditItem ?: initialItem ?: MediaItem(id = finalId)
                                        val mediaItem = baseItem.copy(
                                            id = finalId,
                                            title = cleanTitle,
                                            type = state.type,
                                            category = state.category,
                                            genres = state.genresText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                            rating = state.rating,
                                            releaseYear = state.premiered.take(4),
                                            maturityRating = state.maturityRating,
                                            studio = state.studio,
                                            trailerId = state.trailerId.trim().takeIf { !it.equals("null", ignoreCase = true) } ?: "",
                                            anilistId = if (state.isAnimeCat) state.anilistId.trim() else "",
                                            tmdbId = if (state.isAnimeCat) "" else state.tmdbId.trim(),
                                            synonyms = state.synonyms,
                                            totalEpisodes = state.totalEpisodes,
                                            status = state.status,
                                            aired = state.aired,
                                            premiered = state.premiered,
                                            producers = state.producers,
                                            source = state.source,
                                            duration = state.duration,
                                            castList = state.castText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                            posterUrl = state.posterUrl,
                                            bannerUrl = state.bannerUrl.ifBlank { state.posterUrl },
                                            description = state.description,
                                            isFeatured = state.isFeatured,
                                            isTrending = state.isTrending,
                                            trendingAt = if (state.isTrending) {
                                                if (state.trendingAt > 0L) state.trendingAt else System.currentTimeMillis()
                                            } else 0L,
                                            franchiseId = finalFranchiseId,
                                            franchiseTitle = finalFranchiseTitle,
                                            seasonNumber = parsedSeasonNum,
                                            partNumber = state.partNumberText.toIntOrNull() ?: 0,
                                            franchiseOrder = state.franchiseOrderText.toDoubleOrNull() ?: 0.0,
                                            seasonTitle = state.seasonTitle,
                                            relationType = state.relationType,
                                            mediaInfo = MediaInfo(
                                                resolution = state.resolution,
                                                videoCodec = state.videoCodec,
                                                bitrate = state.bitrate,
                                                frameRate = state.frameRate,
                                                aspectRatio = state.aspectRatio,
                                                fileSize = state.fileSize,
                                                audioTracks = state.audioTracksText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                                subtitleTracks = state.subtitleTracksText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                                qualityBadges = listOfNotNull(
                                                    state.resolution.takeIf { it.isNotBlank() },
                                                    state.videoCodec.takeIf { it.isNotBlank() }
                                                )
                                            ),
                                            episodes = episodes
                                        )
                                        onSave(mediaItem)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier
                                .weight(1.8f)
                                .height(46.dp)
                                .bouncyTouch()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (initialItem == null) "Save & Publish" else "Save Changes",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    // Fixed Footer for Tools Tab
                    Button(
                        onClick = onDismiss,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .bouncyTouch()
                    ) {
                        Text("Close Studio", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) PrimaryRed else Color.Transparent,
        modifier = modifier
            .bouncyTouch()
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

internal fun generateReadableMediaId(
    title: String,
    releaseYear: String,
    seasonTitle: String = "",
    partNumber: Int = 0
): String {
    var raw = title.lowercase()
        .replace("&", "and")
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    val sTitleSlug = seasonTitle.lowercase()
        .replace("&", "and")
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    if (sTitleSlug.isNotBlank() && !raw.contains(sTitleSlug)) {
        raw = "${raw}_$sTitleSlug"
    }

    if (partNumber > 0 && !raw.contains("part") && !raw.contains("pt")) {
        raw = "${raw}_pt$partNumber"
    }

    val cleanSlug = raw.trim('_').take(120)
    val year = releaseYear.trim().take(4)
    return buildString {
        append(cleanSlug.ifBlank { "item_${System.currentTimeMillis()}" })
        if (year.isNotBlank() && !cleanSlug.endsWith(year)) {
            append("_$year")
        }
    }
}
