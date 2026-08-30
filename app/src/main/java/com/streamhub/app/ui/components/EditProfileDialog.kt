package com.streamhub.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.streamhub.app.data.PresetAvatar
import com.streamhub.app.data.UserProfileManager
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

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
            avatarUriInput = uri.toString()
            Toast.makeText(context, "Photo selected!", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                    Text("Customize VIP Persona", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Live Avatar Preview ──
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(
                                    if (avatarUriInput.isBlank()) {
                                        val preset = UserProfileManager.PRESET_AVATARS.getOrElse(selectedPresetIdx) { UserProfileManager.PRESET_AVATARS[0] }
                                        Brush.linearGradient(preset.gradientColors.map { Color(it) })
                                    } else {
                                        Brush.linearGradient(listOf(Color(0xFF1E1E2E), Color(0xFF2D2D44)))
                                    }
                                )
                                .border(2.dp, Brush.linearGradient(listOf(primaryColor, Color(0xFF00E5FF))), CircleShape),
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

                        Spacer(modifier = Modifier.height(6.dp))

                        if (avatarUriInput.isNotBlank()) {
                            TextButton(
                                onClick = { avatarUriInput = "" },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Remove Custom Photo", color = Color(0xFFFF5252), fontSize = 11.sp)
                            }
                        }
                    }
                }

                // ── Avatar Picker (Presets & Gallery) ──
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CHOOSE AVATAR OR PHOTO", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Gallery Upload Button
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E1E2E),
                                border = BorderStroke(1.dp, if (avatarUriInput.isNotBlank()) primaryColor else CardBorderDark),
                                modifier = Modifier
                                    .size(62.dp)
                                    .clickable { photoPickerLauncher.launch("image/*") }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = "Gallery", tint = primaryColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Gallery", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Presets List
                        items(UserProfileManager.PRESET_AVATARS) { preset ->
                            val isSelected = avatarUriInput.isBlank() && selectedPresetIdx == preset.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF161626),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) primaryColor else CardBorderDark
                                ),
                                modifier = Modifier
                                    .size(62.dp)
                                    .clickable {
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
                                                .padding(3.dp)
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Display Name Input ──
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DISPLAY NAME", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 30) nameInput = it },
                        placeholder = { Text("e.g. Shadow Walker, OtakuKing", color = TextSecondary.copy(alpha = 0.6f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = CardBorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${nameInput.length}/30 characters", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
                }

                // ── Bio / Custom Tagline Input ──
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("STATUS / BIO TAGLINE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    OutlinedTextField(
                        value = taglineInput,
                        onValueChange = { if (it.length <= 60) taglineInput = it },
                        placeholder = { Text("e.g. Streaming 4K anime into the night 🌙", color = TextSecondary.copy(alpha = 0.6f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = CardBorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${taglineInput.length}/60 characters", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
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
                    Toast.makeText(context, "VIP Profile updated! ✨", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(12.dp)
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
                    Toast.makeText(context, "Profile reset to default", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset", color = TextSecondary, fontSize = 12.sp)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp)
    )
}
