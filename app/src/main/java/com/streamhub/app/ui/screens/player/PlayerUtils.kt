package com.streamhub.app.ui.screens.player

import java.util.Locale

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt().coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Robustly parses various duration string formats into milliseconds:
 * - "1h 52m", "1 hr 52 min", "24 min. per ep.", "24m", "112m"
 * - "01:52:00", "24:00"
 * - Plain milliseconds (> 100_000) or plain minutes
 */
fun parseMediaDurationMs(durationStr: String?): Long {
    if (durationStr.isNullOrBlank()) return 0L
    val s = durationStr.trim()

    // 1. Plain numeric check
    s.toLongOrNull()?.let { num ->
        return if (num > 100_000L) num else (num * 60_000L)
    }

    // 2. Colon-separated format ("HH:MM:SS" or "MM:SS")
    if (s.contains(":")) {
        val parts = s.split(":").mapNotNull { it.trim().toLongOrNull() }
        if (parts.size == 3) {
            return (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
        } else if (parts.size == 2) {
            return (parts[0] * 60 + parts[1]) * 1000L
        }
    }

    // 3. Natural language strings (e.g. "1h 52m", "1 hr 52 min", "112 min. per ep.", "24m")
    val hours = Regex("""(\d+)\s*(?:h|hr|hour)""", RegexOption.IGNORE_CASE).find(s)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    val mins = Regex("""(\d+)\s*(?:m|min|minute)""", RegexOption.IGNORE_CASE).find(s)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    val secs = Regex("""(\d+)\s*(?:s|sec|second)""", RegexOption.IGNORE_CASE).find(s)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    val totalMs = (hours * 3600 + mins * 60 + secs) * 1000L
    if (totalMs > 0L) return totalMs

    // 4. Fallback: extract any isolated first number and interpret as minutes if in [1..600]
    val firstNumber = Regex("""\d+""").find(s)?.value?.toLongOrNull() ?: 0L
    return if (firstNumber in 1..600) firstNumber * 60_000L else 0L
}

