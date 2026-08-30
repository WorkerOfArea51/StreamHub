package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class UserProfile(
    val customName: String = "",
    val customTagline: String = "",
    val avatarUri: String = "",
    val avatarPresetIndex: Int = 0,
    val memberId: String = ""
)

data class PresetAvatar(
    val id: Int,
    val name: String,
    val emoji: String,
    val gradientColors: List<Long>
)

/**
 * Local Luxury Persona & Profile Manager (Zero login required).
 *
 * Persists:
 * - Custom display name & bio
 * - Custom gallery photo URI or selected aesthetic preset avatar
 * - Generated persistent VIP Member ID (#SH-XXXX)
 */
object UserProfileManager {

    private const val PREFS_NAME = "streamhub_user_profile"
    private const val KEY_CUSTOM_NAME = "profile_custom_name"
    private const val KEY_CUSTOM_TAGLINE = "profile_custom_tagline"
    private const val KEY_AVATAR_URI = "profile_avatar_uri"
    private const val KEY_AVATAR_PRESET = "profile_avatar_preset"
    private const val KEY_MEMBER_ID = "profile_member_id"

    private lateinit var appContext: Context

    val PRESET_AVATARS = listOf(
        PresetAvatar(0, "Cyber Samurai", "⚡", listOf(0xFFE50914, 0xFFFF5722)),
        PresetAvatar(1, "Neon Sovereign", "👑", listOf(0xFFFFD700, 0xFFFFA000)),
        PresetAvatar(2, "Cosmic Voyaguer", "🌌", listOf(0xFF7C4DFF, 0xFF00E5FF)),
        PresetAvatar(3, "Dragon Knight", "🐉", listOf(0xFF00E676, 0xFF00B0FF)),
        PresetAvatar(4, "Cinema Director", "🎬", listOf(0xFFFF3366, 0xFF7928CA)),
        PresetAvatar(5, "Popcorn King", "🍿", listOf(0xFFFF9800, 0xFFFF5722)),
        PresetAvatar(6, "Phantom Shadow", "🎭", listOf(0xFF6366F1, 0xFFEC4899)),
        PresetAvatar(7, "Lo-Fi Otaku", "🎧", listOf(0xFF38BDF8, 0xFF818CF8)),
        PresetAvatar(8, "Cyber Kitsune", "🦊", listOf(0xFFF43F5E, 0xFFFB923C)),
        PresetAvatar(9, "Valkyrie Blade", "⚔️", listOf(0xFF10B981, 0xFF06B6D4)),
        PresetAvatar(10, "Diamond VIP", "💎", listOf(0xFF00E5FF, 0xFF3B82F6)),
        PresetAvatar(11, "Astro Scout", "🚀", listOf(0xFFA855F7, 0xFFEC4899))
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
            memberId = memberId
        )
    }

    @Synchronized
    fun updateProfile(
        name: String,
        tagline: String,
        avatarUri: String,
        presetIndex: Int
    ) {
        if (!::appContext.isInitialized) return
        val current = _profileFlow.value
        val updated = current.copy(
            customName = name.trim(),
            customTagline = tagline.trim(),
            avatarUri = avatarUri.trim(),
            avatarPresetIndex = presetIndex.coerceIn(0, PRESET_AVATARS.size - 1)
        )

        _profileFlow.value = updated
        getPrefs().edit()
            .putString(KEY_CUSTOM_NAME, updated.customName)
            .putString(KEY_CUSTOM_TAGLINE, updated.customTagline)
            .putString(KEY_AVATAR_URI, updated.avatarUri)
            .putInt(KEY_AVATAR_PRESET, updated.avatarPresetIndex)
            .apply()
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
            .putInt(KEY_AVATAR_PRESET, 0)
            .apply()
    }

    private fun getPrefs(): SharedPreferences {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
