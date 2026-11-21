package com.streamhub.app.ui.screens.player

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.streamhub.app.data.SubtitleConfig

/**
 * 🔤 WHAT IS THIS FILE?
 * This is the Universal Subtitle Engine for the StreamHub Player.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * 1. transformCue: Transforms raw subtitle cues (both text-based ASS/SSA/SRT/VTT and bitmap-based PGS/VOBSUB):
 *    - For PGS bitmaps: scales bitmap size cleanly with antialiasing and applies color tints without losing sharp edges.
 *    - For ASS/SSA/SRT text: applies custom user fonts, font size, text colors, and shadows while preserving anime karaoke
 *      syllables and positioned signs (unless forceCleanTypography is enabled).
 * 2. applySubtitleStyling: Applies typography styles (bold, italic, outline edges, fractional/fixed text size) to Android SubtitleView.
 * 3. DedicatedSubtitleView: A composable that renders a dedicated `SubtitleView` layered above the video, synchronizing
 *    timing offsets (delay/advance) and vertical position drag fraction (bottom to top).
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Used by: `PlayerScreen.kt`
 * - Feeds cues from: ExoPlayer's text track renderer directly to the screen.
 */

fun transformCue(
    cue: Cue,
    config: SubtitleConfig
): Cue {
    val rawBitmap = cue.bitmap
    if (rawBitmap != null && !rawBitmap.isRecycled) {
        val isHardwareConfig = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            rawBitmap.config == Bitmap.Config.HARDWARE
        val bitmap = if (isHardwareConfig) {
            rawBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            rawBitmap
        }

        val scale = (config.fontSizeSp / 18f).coerceIn(0.6f, 2.5f)
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)

        val processedBitmap = try {
            val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            // 1. Draw custom background box if opacity is configured
            val bgColor = config.backgroundColorArgb.toInt()
            if (AndroidColor.alpha(bgColor) > 10) {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = bgColor
                    style = Paint.Style.FILL
                }
                val rect = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
                canvas.drawRoundRect(rect, 8f * scale, 8f * scale, bgPaint)
            }

            // 2. Draw scaled & color-tinted PGS text using LightingColorFilter (preserves sharp black outlines)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val textColor = config.textColorArgb.toInt()

            if (textColor != 0 && textColor != AndroidColor.WHITE) {
                paint.colorFilter = LightingColorFilter(textColor, 0)
            }

            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = Rect(0, 0, targetWidth, targetHeight)
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
            output
        } catch (e: Exception) {
            Log.e("PlayerSubtitlesEngine", "Error transforming PGS cue: ${e.message}", e)
            bitmap
        }

        val newBitmapHeight = if (cue.bitmapHeight != Cue.DIMEN_UNSET) {
            (cue.bitmapHeight * scale).coerceAtMost(0.95f)
        } else {
            Cue.DIMEN_UNSET
        }

        val newSize = if (cue.size != Cue.DIMEN_UNSET) {
            (cue.size * scale).coerceAtMost(0.98f)
        } else {
            Cue.DIMEN_UNSET
        }

        return cue.buildUpon()
            .setBitmap(processedBitmap)
            .setBitmapHeight(newBitmapHeight)
            .setSize(newSize)
            .build()
    }

    val rawText = cue.text
    if (rawText != null) {
        // Preserve existing ASS / SSA styling spans if already present
        val builder = if (rawText is Spanned) {
            SpannableStringBuilder(rawText)
        } else {
            SpannableStringBuilder(rawText.toString())
        }

        // An explicitly positioned cue (e.g. anime signs, karaoke lyrics placed at top)
        val isPositionedSignOrSong = cue.line != Cue.DIMEN_UNSET ||
                cue.position != Cue.DIMEN_UNSET

        // When forceCleanTypography is enabled: strip embedded foreground colors so user's color is enforced
        if (config.forceCleanTypography) {
            val oldColorSpans = builder.getSpans(0, builder.length, ForegroundColorSpan::class.java)
            oldColorSpans.forEach { builder.removeSpan(it) }
        }

        // Only apply user foreground color if the cue doesn't already have its own embedded color spans
        val existingColorSpans = builder.getSpans(0, builder.length, ForegroundColorSpan::class.java)
        if (existingColorSpans.isEmpty()) {
            val textColor = config.textColorArgb.toInt()
            if (textColor != 0) {
                builder.setSpan(
                    ForegroundColorSpan(textColor),
                    0,
                    builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        // Do not force solid background boxes on individual karaoke syllables or positioned signs (unless forcing clean typography)
        if (!isPositionedSignOrSong || config.forceCleanTypography) {
            val bgColor = config.backgroundColorArgb.toInt()
            if (AndroidColor.alpha(bgColor) > 10) {
                builder.setSpan(
                    BackgroundColorSpan(bgColor),
                    0,
                    builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        val styleSpan = when {
            config.bold && config.italic -> StyleSpan(Typeface.BOLD_ITALIC)
            config.bold -> StyleSpan(Typeface.BOLD)
            config.italic -> StyleSpan(Typeface.ITALIC)
            else -> null
        }
        if (styleSpan != null) {
            builder.setSpan(styleSpan, 0, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val cueBuilder = cue.buildUpon().setText(builder)
        // Only override font size and alignment for regular dialogue subtitles to avoid blowing up positioned signs/karaoke,
        // unless forceCleanTypography is active, in which case consistent uniform sizing and alignment are applied.
        if (!isPositionedSignOrSong || config.forceCleanTypography) {
            cueBuilder.setTextSize(config.fontSizeSp, Cue.TEXT_SIZE_TYPE_ABSOLUTE)
            val textAlignment = when (config.alignment) {
                "LEFT" -> Layout.Alignment.ALIGN_NORMAL
                "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_CENTER
            }
            cueBuilder.setTextAlignment(textAlignment)
        }
        return cueBuilder.build()
    }

    return cue
}

fun applySubtitleStyling(
    subtitleView: SubtitleView?,
    config: SubtitleConfig
) {
    val sv = subtitleView ?: return

    // Allow embedded styles from ASS / SSA files (karaoke, fonts, sign placement) to display natively,
    // while user-configured font sizes and caption styles take effect for plain dialogue/SRT.
    // When forceCleanTypography is active, embedded styles are suppressed for clean uniform reading.
    sv.setApplyEmbeddedStyles(!config.forceCleanTypography)
    sv.setApplyEmbeddedFontSizes(false)

    val typefaceStyle = when {
        config.bold && config.italic -> Typeface.BOLD_ITALIC
        config.bold -> Typeface.BOLD
        config.italic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }
    val customTypeface = Typeface.create(Typeface.DEFAULT, typefaceStyle)

    val effectiveEdgeType = config.edgeType

    sv.setStyle(
        CaptionStyleCompat(
            config.textColorArgb.toInt(),
            config.backgroundColorArgb.toInt(),
            AndroidColor.TRANSPARENT,
            effectiveEdgeType,
            AndroidColor.BLACK,
            customTypeface
        )
    )

    if (config.scaleByWindow) {
        sv.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * (config.fontSizeSp / 18f))
    } else {
        sv.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, config.fontSizeSp)
    }

    sv.invalidate()
    sv.requestLayout()
}

@Composable
fun DedicatedSubtitleView(
    player: Player?,
    subConfig: SubtitleConfig,
    subtitleOffsetMs: Long,
    isSubOff: Boolean,
    modifier: Modifier = Modifier
) {
    var rememberSubtitleViewRef by remember { mutableStateOf<SubtitleView?>(null) }

    // Live Subtitle Styling Engine
    LaunchedEffect(subConfig, rememberSubtitleViewRef, isSubOff) {
        val sv = rememberSubtitleViewRef ?: return@LaunchedEffect
        if (isSubOff) {
            sv.setCues(emptyList())
            sv.visibility = View.GONE
        } else {
            sv.visibility = View.VISIBLE
            applySubtitleStyling(sv, subConfig)
            val currentCues = player?.currentCues?.cues ?: emptyList()
            sv.setCues(currentCues.map { transformCue(it, subConfig) })
        }
    }

    // Real-Time PGS, ASS & Universal Subtitle Processing Pipeline
    DisposableEffect(player, subConfig, rememberSubtitleViewRef, isSubOff, subtitleOffsetMs) {
        val p = player
        val sv = rememberSubtitleViewRef
        if (p == null || sv == null) return@DisposableEffect onDispose {}

        if (isSubOff) {
            sv.setCues(emptyList())
            sv.visibility = View.GONE
            return@DisposableEffect onDispose {}
        } else {
            sv.visibility = View.VISIBLE
        }

        var pendingCueRunnable: Runnable? = null
        val cueListener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                pendingCueRunnable?.let { sv.removeCallbacks(it) }
                if (isSubOff) {
                    sv.setCues(emptyList())
                    return
                }
                val transformed = cueGroup.cues.map { transformCue(it, subConfig) }
                val offset = subtitleOffsetMs
                if (offset > 0L) {
                    val runnable = Runnable {
                        if (!isSubOff) sv.setCues(transformed)
                    }
                    pendingCueRunnable = runnable
                    sv.postDelayed(runnable, offset)
                } else {
                    sv.setCues(transformed)
                }
            }
        }
        p.addListener(cueListener)

        val currentCues = p.currentCues.cues
        if (currentCues.isNotEmpty() && !isSubOff) {
            val transformed = currentCues.map { transformCue(it, subConfig) }
            sv.setCues(transformed)
        }

        onDispose {
            pendingCueRunnable?.let { sv.removeCallbacks(it) }
            p.removeListener(cueListener)
        }
    }

    if (!isSubOff) {
        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Translates vertical subtitle position smoothly for ALL subtitle formats (ASS, PGS, SRT, VTT)
                    val basePadding = 0.08f
                    val delta = subConfig.bottomPaddingFraction - basePadding
                    translationY = -size.height * delta
                },
            factory = { ctx ->
                SubtitleView(ctx).apply {
                    applySubtitleStyling(this, subConfig)
                    rememberSubtitleViewRef = this
                }
            },
            update = { sv ->
                rememberSubtitleViewRef = sv
                applySubtitleStyling(sv, subConfig)
                val currentCues = player?.currentCues?.cues ?: emptyList()
                sv.setCues(currentCues.map { transformCue(it, subConfig) })
            }
        )
    }
}
