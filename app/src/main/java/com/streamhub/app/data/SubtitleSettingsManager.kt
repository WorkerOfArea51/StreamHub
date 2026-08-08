package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.ui.CaptionStyleCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SubtitleConfig(
    val fontSizeSp: Float = 18f,
    val textColorArgb: Long = 0xFFFFE066L, // Vibrant Yellow
    val backgroundColorArgb: Long = 0xAA000000L, // Semi-transparent black
    val edgeType: Int = CaptionStyleCompat.EDGE_TYPE_OUTLINE
)

/**
 * Production Media3 Subtitle Appearance Manager:
 * - Configures closed caption font size (Small 14sp, Medium 18sp, Large 24sp, XL 30sp)
 * - Configures text color (Yellow, White, Cyan, Green)
 * - Configures background box contrast
 * - Persists preferences in SharedPreferences (streamhub_subtitle_prefs)
 */
object SubtitleSettingsManager {

    private const val PREFS_NAME = "streamhub_subtitle_prefs"
    private const val KEY_FONT_SIZE = "font_size_sp"
    private const val KEY_TEXT_COLOR = "text_color_argb"
    private const val KEY_BG_COLOR = "bg_color_argb"
    private const val KEY_EDGE_TYPE = "edge_type"

    private var prefs: SharedPreferences? = null

    private val _subtitleConfig = MutableStateFlow(SubtitleConfig())
    val subtitleConfig: StateFlow<SubtitleConfig> = _subtitleConfig.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val p = prefs ?: return
        try {
            _subtitleConfig.value = SubtitleConfig(
                fontSizeSp = p.getFloat(KEY_FONT_SIZE, 18f),
                textColorArgb = p.getLong(KEY_TEXT_COLOR, 0xFFFFE066L),
                backgroundColorArgb = p.getLong(KEY_BG_COLOR, 0xAA000000L),
                edgeType = p.getInt(KEY_EDGE_TYPE, CaptionStyleCompat.EDGE_TYPE_OUTLINE)
            )
        } catch (e: Exception) {
            p.edit().clear().apply()
            _subtitleConfig.value = SubtitleConfig()
        }
    }

    fun updateConfig(newConfig: SubtitleConfig) {
        _subtitleConfig.value = newConfig
        prefs?.edit()?.apply {
            putFloat(KEY_FONT_SIZE, newConfig.fontSizeSp)
            putLong(KEY_TEXT_COLOR, newConfig.textColorArgb)
            putLong(KEY_BG_COLOR, newConfig.backgroundColorArgb)
            putInt(KEY_EDGE_TYPE, newConfig.edgeType)
            apply()
        }
    }
}
