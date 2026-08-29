package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
data class SubtitleConfig(
    val fontSizeSp: Float = 18f,
    val textColorArgb: Long = 0xFFFFE066L, // Vibrant Yellow
    val backgroundColorArgb: Long = 0xAA000000L, // Semi-transparent black
    val edgeType: Int = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val alignment: String = "CENTER",
    val outlineWidth: Float = 2f,
    val shadowOffset: Float = 2f,
    val scaleByWindow: Boolean = true,
    val bottomPaddingFraction: Float = 0.08f // Vertical screen offset (0.02f = bottom, 0.85f = top)
)

/**
 * Production Media3 Subtitle Appearance Manager:
 * - Configures closed caption font size (Small 14sp, Medium 18sp, Large 24sp, XL 30sp)
 * - Configures text color (Yellow, White, Cyan, Green)
 * - Configures typography styles (Bold, Italic, Alignment, Outlines)
 * - Configures vertical screen position (bottom padding fraction)
 * - Persists preferences in SharedPreferences (streamhub_subtitle_prefs)
 */
@OptIn(UnstableApi::class)
object SubtitleSettingsManager {

    private const val PREFS_NAME = "streamhub_subtitle_prefs"
    private const val KEY_FONT_SIZE = "font_size_sp"
    private const val KEY_TEXT_COLOR = "text_color_argb"
    private const val KEY_BG_COLOR = "bg_color_argb"
    private const val KEY_EDGE_TYPE = "edge_type"
    private const val KEY_BOLD = "bold"
    private const val KEY_ITALIC = "italic"
    private const val KEY_ALIGNMENT = "alignment"
    private const val KEY_OUTLINE_WIDTH = "outline_width"
    private const val KEY_SHADOW_OFFSET = "shadow_offset"
    private const val KEY_SCALE_BY_WINDOW = "scale_by_window"
    private const val KEY_BOTTOM_PADDING = "bottom_padding_fraction"

    private var prefs: SharedPreferences? = null

    private val _subtitleConfig = MutableStateFlow(SubtitleConfig())
    val subtitleConfig: StateFlow<SubtitleConfig> = _subtitleConfig.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val p = prefs ?: return
        try {
            _subtitleConfig.value = SubtitleConfig(
                fontSizeSp = p.getFloat(KEY_FONT_SIZE, 18f),
                textColorArgb = p.getLong(KEY_TEXT_COLOR, 0xFFFFE066L),
                backgroundColorArgb = p.getLong(KEY_BG_COLOR, 0xAA000000L),
                edgeType = p.getInt(KEY_EDGE_TYPE, CaptionStyleCompat.EDGE_TYPE_OUTLINE),
                bold = p.getBoolean(KEY_BOLD, false),
                italic = p.getBoolean(KEY_ITALIC, false),
                alignment = p.getString(KEY_ALIGNMENT, "CENTER") ?: "CENTER",
                outlineWidth = p.getFloat(KEY_OUTLINE_WIDTH, 2f),
                shadowOffset = p.getFloat(KEY_SHADOW_OFFSET, 2f),
                scaleByWindow = p.getBoolean(KEY_SCALE_BY_WINDOW, true),
                bottomPaddingFraction = p.getFloat(KEY_BOTTOM_PADDING, 0.08f)
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
            putBoolean(KEY_BOLD, newConfig.bold)
            putBoolean(KEY_ITALIC, newConfig.italic)
            putString(KEY_ALIGNMENT, newConfig.alignment)
            putFloat(KEY_OUTLINE_WIDTH, newConfig.outlineWidth)
            putFloat(KEY_SHADOW_OFFSET, newConfig.shadowOffset)
            putBoolean(KEY_SCALE_BY_WINDOW, newConfig.scaleByWindow)
            putFloat(KEY_BOTTOM_PADDING, newConfig.bottomPaddingFraction)
            apply()
        }
    }
}

