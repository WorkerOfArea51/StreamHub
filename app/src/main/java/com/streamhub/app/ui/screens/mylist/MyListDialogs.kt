package com.streamhub.app.ui.screens.mylist

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * [MyListDialogs.kt] contains the modal dialogs and bottom sheets used for
 * custom folder management and library cleanup in My List:
 *
 * 1. [AddShowToFolderDialog]: Quick search to add titles directly into a custom collection.
 * 2. [CreateFolderDialog]: Dialog to create a new custom folder.
 * 3. [RenameFolderDialog]: Dialog to rename an existing custom folder.
 * 4. [DeleteFolderDialog]: Confirmation to delete a folder without deleting the saved shows.
 * 5. [ClearCompletedDialog]: Confirmation to remove all finished titles in one tap.
 * 6. [FolderOptionsSheet]: Modal bottom sheet with Rename & Delete options for folders.
 * =========================================================================================
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.MyListItem
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.AccentGold

@Composable
fun AddShowToFolderDialog(
    selectedCollection: String,
    catalog: List<MediaItem>,
    myItemsMap: Map<String, MyListItem>,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var addShowSearchQuery by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Add to '$selectedCollection'",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = addShowSearchQuery,
                    onValueChange = { addShowSearchQuery = it },
                    placeholder = { Text("Search catalog shows...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                val searchResults = remember(catalog, addShowSearchQuery) {
                    val q = addShowSearchQuery.trim().lowercase()
                    if (q.isBlank()) catalog.take(15)
                    else catalog.filter { it.title.lowercase().contains(q) }.take(20)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(searchResults) { show ->
                        val alreadyInCol = myItemsMap[show.id]?.collection.equals(selectedCollection, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                            .clickable {
                                MyListManager.toggleBookmark(show.id)
                                MyListManager.setCollection(show.id, selectedCollection)
                                ToastManager.showToast("Added '${show.title}' to $selectedCollection")
                            }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = show.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = if (alreadyInCol) "Saved" else "+ Add",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alreadyInCol) AccentGold else primaryColor
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = primaryColor, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onFolderCreated: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var newCollectionName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Create New Folder", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Organize your favorite movies and shows into custom lists.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newCollectionName,
                    onValueChange = { newCollectionName = it },
                    placeholder = { Text("Folder Name (e.g. Weekend Binges)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = newCollectionName.trim()
                    if (trimmed.isNotBlank()) {
                        if (MyListManager.addCustomCollection(trimmed)) {
                            onFolderCreated(trimmed)
                            onDismiss()
                            ToastManager.showToast("Folder '$trimmed' created")
                        } else {
                            ToastManager.showToast("Folder already exists or is reserved")
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Create Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun RenameFolderDialog(
    currentTarget: String,
    onDismiss: () -> Unit,
    onFolderRenamed: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var renameFolderInput by remember { mutableStateOf(currentTarget) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Rename Folder", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter new name for '$currentTarget':", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = renameFolderInput,
                    onValueChange = { renameFolderInput = it },
                    placeholder = { Text("New Folder Name", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = renameFolderInput.trim()
                    if (trimmed.isNotBlank() && currentTarget.isNotBlank()) {
                        if (MyListManager.renameCustomCollection(currentTarget, trimmed)) {
                            onFolderRenamed(trimmed)
                            onDismiss()
                            ToastManager.showToast("Renamed to '$trimmed'")
                        } else {
                            ToastManager.showToast("Name already exists or is reserved")
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun DeleteFolderDialog(
    currentTarget: String,
    onDismiss: () -> Unit,
    onFolderDeleted: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Delete Folder?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Are you sure you want to delete '$currentTarget'? Saved shows in this folder will be kept and moved back to Watchlist.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (MyListManager.deleteCustomCollection(currentTarget)) {
                        onFolderDeleted()
                        onDismiss()
                        ToastManager.showToast("Folder deleted")
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun ClearCompletedDialog(
    completedCount: Int,
    onDismiss: () -> Unit,
    onConfirmClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Clear Completed Titles?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "This will remove $completedCount finished titles from your Watchlist. This cannot be undone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmClear,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
            ) {
                Text("Clear All", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderOptionsSheet(
    currentTarget: String,
    onDismiss: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Folder: $currentTarget",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRenameClick() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = primaryColor)
                Text("Rename Folder", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeleteClick() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252))
                Text("Delete Folder", color = Color(0xFFFF5252), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
