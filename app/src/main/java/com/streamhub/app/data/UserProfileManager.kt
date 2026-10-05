package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.streamhub.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

data class UserProfile(
    val customName: String = "",
    val customTagline: String = "",
    val avatarUri: String = "",
    val avatarPresetIndex: Int = 0,
    val memberId: String = "",
    val backgroundUri: String = ""
)

data class PresetAvatar(
    val id: Int,
    val name: String,
    val category: String,
    val drawableResId: Int
)

/**
 * Local Luxury Persona & Profile Manager (Zero login required).
 *
 * Persists:
 * - Custom display name & bio
 * - Custom gallery photo URI or selected aesthetic preset avatar
 * - Optional custom profile wallpaper/background
 * - Generated persistent VIP Member ID (#SH-XXXX)
 */
object UserProfileManager {

    private const val PREFS_NAME = "streamhub_user_profile"
    private const val KEY_CUSTOM_NAME = "profile_custom_name"
    private const val KEY_CUSTOM_TAGLINE = "profile_custom_tagline"
    private const val KEY_AVATAR_URI = "profile_avatar_uri"
    private const val KEY_AVATAR_PRESET = "profile_avatar_preset"
    private const val KEY_MEMBER_ID = "profile_member_id"
    private const val KEY_BACKGROUND_URI = "profile_background_uri"

    private lateinit var appContext: Context

    val PRESET_AVATARS = listOf(
        PresetAvatar(0, "Cyber Samurai", "Cyber", R.drawable.avatar_cyber_samurai),
        PresetAvatar(1, "Neon Phantom", "Cyber", R.drawable.avatar_neon_phantom),
        PresetAvatar(2, "Cosmic Voyager", "Sci-Fi", R.drawable.avatar_cosmic_voyager),
        PresetAvatar(3, "Mecha Titan", "Cyber", R.drawable.avatar_mecha_titan),
        PresetAvatar(4, "Noir Detective", "Cinema", R.drawable.avatar_noir_detective),
        PresetAvatar(5, "Film Director", "Cinema", R.drawable.avatar_film_director),
        PresetAvatar(6, "Retro Cinephile", "Cinema", R.drawable.avatar_retro_cinephile),
        PresetAvatar(7, "Golden Maestro", "Classic", R.drawable.avatar_golden_maestro),
        PresetAvatar(8, "Shadow Shinobi", "Mythic", R.drawable.avatar_shadow_shinobi),
        PresetAvatar(9, "Solar Valkyrie", "Mythic", R.drawable.avatar_solar_valkyrie),
        PresetAvatar(10, "Arcane Sorcerer", "Fantasy", R.drawable.avatar_arcane_sorcerer),
        PresetAvatar(11, "Frost Viking", "Mythic", R.drawable.avatar_frost_viking),
        PresetAvatar(12, "Urban Wolf", "Creatures", R.drawable.avatar_urban_wolf),
        PresetAvatar(13, "Kitsune Fox", "Creatures", R.drawable.avatar_kitsune_fox),
        PresetAvatar(14, "Midnight Owl", "Creatures", R.drawable.avatar_midnight_owl),
        PresetAvatar(15, "Mecha Panther", "Creatures", R.drawable.avatar_mecha_panther),
        PresetAvatar(16, "Chill Panda", "Creatures", R.drawable.avatar_chill_panda),
        PresetAvatar(17, "Stellar Idol", "Anime", R.drawable.avatar_stellar_idol),
        PresetAvatar(18, "Crimson Rebel", "Anime", R.drawable.avatar_crimson_rebel),
        PresetAvatar(19, "Lo-Fi Dreamer", "Chill", R.drawable.avatar_lofi_dreamer)
    )

    private val _profileFlow = MutableStateFlow(UserProfile())
    val profileFlow: StateFlow<UserProfile> = _profileFlow.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        loadProfile()
    }

    private fun loadProfile() {
        val prefs = getPrefs()
        var memberId = prefs.getString(KEY_MEMBER_ID, "") ?: ""
        if (memberId.isBlank()) {
            val randomSuffix = UUID.randomUUID().toString().take(4).uppercase()
            memberId = "SH-$randomSuffix"
            prefs.edit().putString(KEY_MEMBER_ID, memberId).apply()
        }

        _profileFlow.value = UserProfile(
            customName = prefs.getString(KEY_CUSTOM_NAME, "") ?: "",
            customTagline = prefs.getString(KEY_CUSTOM_TAGLINE, "") ?: "",
            avatarUri = prefs.getString(KEY_AVATAR_URI, "") ?: "",
            avatarPresetIndex = prefs.getInt(KEY_AVATAR_PRESET, 0).coerceIn(0, PRESET_AVATARS.size - 1),
            memberId = memberId,
            backgroundUri = prefs.getString(KEY_BACKGROUND_URI, "") ?: ""
        )
    }

    @Synchronized
    fun updateProfile(
        name: String,
        tagline: String,
        avatarUri: String,
        presetIndex: Int,
        backgroundUri: String = _profileFlow.value.backgroundUri
    ) {
        if (!::appContext.isInitialized) return
        val current = _profileFlow.value
        val updated = current.copy(
            customName = name.trim(),
            customTagline = tagline.trim(),
            avatarUri = avatarUri.trim(),
            avatarPresetIndex = presetIndex.coerceIn(0, PRESET_AVATARS.size - 1),
            backgroundUri = backgroundUri.trim()
        )

        _profileFlow.value = updated
        getPrefs().edit()
            .putString(KEY_CUSTOM_NAME, updated.customName)
            .putString(KEY_CUSTOM_TAGLINE, updated.customTagline)
            .putString(KEY_AVATAR_URI, updated.avatarUri)
            .putInt(KEY_AVATAR_PRESET, updated.avatarPresetIndex)
            .putString(KEY_BACKGROUND_URI, updated.backgroundUri)
            .apply()
    }

    fun saveCustomAvatar(context: Context, sourceUri: Uri): String {
        return try {
            val destFile = File(context.filesDir, "custom_avatar.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            "file://${destFile.absolutePath}?t=${System.currentTimeMillis()}"
        } catch (e: Exception) {
            Log.e("UserProfileManager", "Failed to save custom avatar", e)
            ""
        }
    }

    fun saveCustomBackground(context: Context, sourceUri: Uri): String {
        return try {
            val destFile = File(context.filesDir, "custom_profile_bg.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            "file://${destFile.absolutePath}?t=${System.currentTimeMillis()}"
        } catch (e: Exception) {
            Log.e("UserProfileManager", "Failed to save custom background", e)
            ""
        }
    }

    @Synchronized
    fun resetToDefault() {
        if (!::appContext.isInitialized) return
        val memberId = _profileFlow.value.memberId
        val defaultProfile = UserProfile(memberId = memberId)
        _profileFlow.value = defaultProfile

        getPrefs().edit()
            .remove(KEY_CUSTOM_NAME)
            .remove(KEY_CUSTOM_TAGLINE)
            .remove(KEY_AVATAR_URI)
            .remove(KEY_BACKGROUND_URI)
            .putInt(KEY_AVATAR_PRESET, 0)
            .apply()
    }

    /**
     * Restores user profile settings from backup payload.
     */
    @Synchronized
    fun restoreProfile(profile: UserProfile?) {
        if (!::appContext.isInitialized || profile == null) return
        val current = _profileFlow.value
        val updated = current.copy(
            customName = profile.customName.ifBlank { current.customName },
            customTagline = profile.customTagline.ifBlank { current.customTagline },
            avatarPresetIndex = profile.avatarPresetIndex.coerceIn(0, PRESET_AVATARS.size - 1),
            memberId = profile.memberId.ifBlank { current.memberId },
            backgroundUri = profile.backgroundUri.ifBlank { current.backgroundUri }
        )
        _profileFlow.value = updated
        getPrefs().edit()
            .putString(KEY_CUSTOM_NAME, updated.customName)
            .putString(KEY_CUSTOM_TAGLINE, updated.customTagline)
            .putInt(KEY_AVATAR_PRESET, updated.avatarPresetIndex)
            .putString(KEY_MEMBER_ID, updated.memberId)
            .putString(KEY_BACKGROUND_URI, updated.backgroundUri)
            .apply()
    }

    private fun getPrefs(): SharedPreferences {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
