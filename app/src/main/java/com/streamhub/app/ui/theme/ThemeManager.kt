package com.streamhub.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
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

object ThemeManager {
    private const val PREFS_NAME = "streamhub_theme_prefs"
    private const val KEY_ACCENT = "theme_accent_key"

    private val _currentAccent = MutableStateFlow(AppThemeAccent.RED)
    val currentAccent: StateFlow<AppThemeAccent> = _currentAccent.asStateFlow()

    fun init(context: Context) {
        val prefs = getPrefs(context)
        val savedKey = prefs.getString(KEY_ACCENT, AppThemeAccent.RED.key) ?: AppThemeAccent.RED.key
        _currentAccent.value = AppThemeAccent.values().firstOrNull { it.key == savedKey } ?: AppThemeAccent.RED
    }

    fun setAccent(context: Context, accent: AppThemeAccent) {
        _currentAccent.value = accent
        getPrefs(context).edit().putString(KEY_ACCENT, accent.key).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
