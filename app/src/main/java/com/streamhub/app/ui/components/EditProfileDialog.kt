package com.streamhub.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.UserProfileManager
import com.streamhub.app.ui.theme.bouncyClickable

@Composable
fun EditProfileDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentProfile by UserProfileManager.profileFlow.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    var nameInput by remember { mutableStateOf(currentProfile.customName) }
    var taglineInput by remember { mutableStateOf(currentProfile.customTagline) }
    var avatarUriInput by remember { mutableStateOf(currentProfile.avatarUri) }
    var selectedPresetIdx by remember { mutableIntStateOf(currentProfile.avatarPresetIndex) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = UserProfileManager.saveCustomAvatar(context, uri)
            if (savedPath.isNotBlank()) {
                avatarUriInput = savedPath
                ToastManager.showToast("Photo selected successfully", Icons.Default.Check)
            } else {
                ToastManager.showToast("Failed to load photo", Icons.Default.Close)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Customize Profile Persona",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ── Live Avatar Preview ──
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(
                                    if (avatarUriInput.isBlank()) {
                                        val preset = UserProfileManager.PRESET_AVATARS.getOrElse(selectedPresetIdx) { UserProfileManager.PRESET_AVATARS[0] }
                                        Brush.linearGradient(preset.gradientColors.map { Color(it) })
                                    } else {
                                        Brush.linearGradient(listOf(Color(0xFF1E1E2E), Color(0xFF2D2D44)))
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUriInput.isNotBlank()) {
                                AsyncImage(
                                    model = avatarUriInput,
                                    contentDescription = "Avatar Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val preset = UserProfileManager.PRESET_AVATARS.getOrElse(selectedPresetIdx) { UserProfileManager.PRESET_AVATARS[0] }
                                Text(preset.emoji, fontSize = 38.sp)
                            }
                        }

                        if (avatarUriInput.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                    .bouncyClickable { avatarUriInput = "" }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Remove Custom Photo",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // ── Avatar Picker (Presets & Gallery) ──
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CHOOSE AVATAR OR PHOTO",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Gallery Upload Button
                        item {
                            val isCustomActive = avatarUriInput.isNotBlank()
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isCustomActive) primaryColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .size(64.dp)
                                    .bouncyClickable { photoPickerLauncher.launch("image/*") }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Gallery",
                                        tint = if (isCustomActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Gallery",
                                        color = if (isCustomActive) primaryColor else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Presets List
                        items(UserProfileManager.PRESET_AVATARS) { preset ->
                            val isSelected = avatarUriInput.isBlank() && selectedPresetIdx == preset.id
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) primaryColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .size(64.dp)
                                    .bouncyClickable {
                                        avatarUriInput = ""
                                        selectedPresetIdx = preset.id
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(preset.gradientColors.map { Color(it).copy(alpha = 0.25f) })),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(preset.emoji, fontSize = 26.sp)
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Display Name Input ──
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DISPLAY NAME",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    TextField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 30) nameInput = it },
                        placeholder = {
                            Text(
                                text = "e.g. Shadow Walker, OtakuKing",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${nameInput.length}/30 characters",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                // ── Bio / Custom Tagline Input ──
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "STATUS / BIO TAGLINE",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    TextField(
                        value = taglineInput,
                        onValueChange = { if (it.length <= 60) taglineInput = it },
                        placeholder = {
                            Text(
                                text = "e.g. Streaming anime into the night",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${taglineInput.length}/60 characters",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    UserProfileManager.updateProfile(
                        name = nameInput,
                        tagline = taglineInput,
                        avatarUri = avatarUriInput,
                        presetIndex = selectedPresetIdx
                    )
                    ToastManager.showToast("Profile updated successfully", Icons.Default.Check)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    UserProfileManager.resetToDefault()
                    nameInput = ""
                    taglineInput = ""
                    avatarUriInput = ""
                    selectedPresetIdx = 0
                    ToastManager.showToast("Profile reset to default", Icons.Default.Refresh)
                },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reset",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    )
}
