package com.streamhub.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App Theme Accent models for both Classic solid themes and Enhanced dual-tonal/gradient themes.
 * Matches Nuvio aesthetic parity.
 */
enum class AppThemeAccent(
    val key: String,
    val label: String,
    val color: Color,
    val gradientEnd: Color = color,
    val isEnhanced: Boolean = false
) {
    // --- Classic Themes (7 solid brand colors) ---
    WHITE("WHITE", "White", Color(0xFFFFFFFF)),
    RED("RED", "Crimson", Color(0xFFE50914)),
    OCEAN("OCEAN", "Ocean", Color(0xFF007AFF)),
    PURPLE("PURPLE", "Violet", Color(0xFF9D4EDD)),
    GREEN("GREEN", "Emerald", Color(0xFF10B981)),
    ORANGE("ORANGE", "Amber", Color(0xFFFF9500)),
    ROSE("ROSE", "Rose", Color(0xFFFF2D55)),

    // --- Enhanced Themes (6 dual-tone gradients) ---
    MESSENGER("MESSENGER", "Messenger", Color(0xFF0084FF), Color(0xFF00C6FF), isEnhanced = true),
    AMETHYST("AMETHYST", "Amethyst", Color(0xFFA855F7), Color(0xFFEC4899), isEnhanced = true),
    BLOSSOM("BLOSSOM", "Blossom", Color(0xFFFF5E7E), Color(0xFFFF99AC), isEnhanced = true),
    LAGOON("LAGOON", "Lagoon", Color(0xFF00C9FF), Color(0xFF92FE9D), isEnhanced = true),
    SUNSET("SUNSET", "Sunset", Color(0xFFFF512F), Color(0xFFF09819), isEnhanced = true),
    CUSTOM("CUSTOM", "Custom", Color(0xFFFF0080), Color(0xFF7928CA), isEnhanced = true);

    val brush: Brush
        get() = if (isEnhanced) {
            Brush.linearGradient(listOf(color, gradientEnd))
        } else {
            Brush.linearGradient(listOf(color, color))
        }

    companion object {
        val classicThemes: List<AppThemeAccent> by lazy {
            entries.filter { !it.isEnhanced }
        }

        val enhancedThemes: List<AppThemeAccent> by lazy {
            entries.filter { it.isEnhanced }
        }

        val CYAN: AppThemeAccent get() = OCEAN

        fun fromKey(key: String): AppThemeAccent {
            return when (key.uppercase()) {
                "WHITE" -> WHITE
                "RED", "CRIMSON" -> RED
                "OCEAN", "CYAN", "BLUE" -> OCEAN
                "PURPLE", "VIOLET" -> PURPLE
                "GREEN", "EMERALD" -> GREEN
                "ORANGE", "AMBER" -> ORANGE
                "ROSE" -> ROSE
                "MESSENGER" -> MESSENGER
                "AMETHYST" -> AMETHYST
                "BLOSSOM" -> BLOSSOM
                "LAGOON" -> LAGOON
                "SUNSET" -> SUNSET
                "CUSTOM" -> CUSTOM
                else -> RED
            }
        }
    }
}

/**
 * Persists the user's selected theme accent color and AMOLED black preference in SharedPreferences.
 *
 * Initialized once by StreamHubApplication.onCreate(). Callers do NOT pass
 * context to any method.
 */
object ThemeManager {

    private const val TAG = "ThemeManager"
    private const val PREFS_NAME = "streamhub_theme_prefs"
    private const val KEY_ACCENT = "theme_accent_key"
    private const val KEY_AMOLED_BLACK = "theme_amoled_black"

    @Volatile
    private var appContext: Context? = null

    private val _currentAccent = MutableStateFlow(AppThemeAccent.RED)
    val currentAccent: StateFlow<AppThemeAccent> = _currentAccent.asStateFlow()

    private val _isAmoledBlack = MutableStateFlow(false)
    val isAmoledBlack: StateFlow<Boolean> = _isAmoledBlack.asStateFlow()

    fun init(context: Context) {
        if (appContext != null) return
        synchronized(this) {
            if (appContext == null) {
                appContext = context.applicationContext
                loadFromDisk()
            }
        }
    }

    private fun loadFromDisk() {
        val prefs = getPrefs() ?: return
        val savedKey = prefs.getString(KEY_ACCENT, AppThemeAccent.RED.key) ?: AppThemeAccent.RED.key
        _currentAccent.value = AppThemeAccent.fromKey(savedKey)
        _isAmoledBlack.value = prefs.getBoolean(KEY_AMOLED_BLACK, false)
    }

    fun setAccent(accent: AppThemeAccent) {
        _currentAccent.value = accent
        getPrefs()?.edit()?.putString(KEY_ACCENT, accent.key)?.apply()
    }

    fun setAmoledBlack(enabled: Boolean) {
        _isAmoledBlack.value = enabled
        getPrefs()?.edit()?.putBoolean(KEY_AMOLED_BLACK, enabled)?.apply()
    }

    private fun getPrefs(): SharedPreferences? {
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
