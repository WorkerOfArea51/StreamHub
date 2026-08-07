package com.streamhub.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeAccent(val key: String, val label: String, val color: Color) {
    RED("RED", "Netflix Red", Color(0xFFE50914)),
    ORANGE("ORANGE", "Crunchyroll Orange", Color(0xFFFF6B00)),
    CYAN("CYAN", "Cyberpunk Cyan", Color(0xFF00E5FF)),
    GREEN("GREEN", "Emerald Green", Color(0xFF10B981)),
    PURPLE("PURPLE", "Neon Purple", Color(0xFF9D4EDD))
}

/**
 * Persists the user's selected theme accent color in SharedPreferences.
 *
 * Initialized once by StreamHubApplication.onCreate(). Callers do NOT pass
 * context to any method.
 *
 * The selected accent is applied to the Material3 color scheme via Theme.kt
 * (primary = accent.color). The SettingsScreen picker UI collects
 * currentAccent to show the selection.
 */
object ThemeManager {

    private const val TAG = "ThemeManager"
    private const val PREFS_NAME = "streamhub_theme_prefs"
    private const val KEY_ACCENT = "theme_accent_key"

    private lateinit var appContext: Context

    private val _currentAccent = MutableStateFlow(AppThemeAccent.RED)
    val currentAccent: StateFlow<AppThemeAccent> = _currentAccent.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val prefs = getPrefs()
        val savedKey = prefs.getString(KEY_ACCENT, AppThemeAccent.RED.key) ?: AppThemeAccent.RED.key
        _currentAccent.value = AppThemeAccent.entries.firstOrNull { it.key == savedKey } ?: AppThemeAccent.RED
    }

    fun setAccent(accent: AppThemeAccent) {
        if (!::appContext.isInitialized) {
            Log.w(TAG, "setAccent called before init — no-op")
            return
        }
        _currentAccent.value = accent
        getPrefs().edit().putString(KEY_ACCENT, accent.key).apply()
    }

    private fun getPrefs(): SharedPreferences {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
