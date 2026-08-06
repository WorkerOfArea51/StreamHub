package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * User Engagement & Analytics Manager:
 * - All-time watch duration (in seconds/hours)
 * - Today's watch duration (in seconds/minutes)
 * - Active daily watch streak counter
 * - Category Watch Breakdown (% Anime, % Movies, % Web Series)
 *
 * FIX: All default values are now HONEST — a fresh install shows 0.0h, 0m, 0-day streak.
 * No fabricated engagement metrics.
 */
object UserStatsManager {

    private const val PREFS_NAME = "streamhub_user_stats"
    private const val KEY_TOTAL_WATCH_SECONDS = "total_watch_seconds"
    private const val KEY_DAILY_WATCH_SECONDS = "daily_watch_seconds"
    private const val KEY_LAST_WATCH_DATE = "last_watch_date"
    private const val KEY_STREAK_COUNT = "streak_count"
    private const val KEY_ANIME_SECONDS = "anime_watch_seconds"
    private const val KEY_MOVIE_SECONDS = "movie_watch_seconds"
    private const val KEY_SERIES_SECONDS = "series_watch_seconds"

    private var prefs: SharedPreferences? = null

    private val _totalWatchHours = MutableStateFlow("0.0h")
    val totalWatchHours: StateFlow<String> = _totalWatchHours.asStateFlow()

    private val _dailyWatchFormatted = MutableStateFlow("0m")
    val dailyWatchFormatted: StateFlow<String> = _dailyWatchFormatted.asStateFlow()

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private val _animePercent = MutableStateFlow(0)
    val animePercent: StateFlow<Int> = _animePercent.asStateFlow()

    private val _moviePercent = MutableStateFlow(0)
    val moviePercent: StateFlow<Int> = _moviePercent.asStateFlow()

    private val _seriesPercent = MutableStateFlow(0)
    val seriesPercent: StateFlow<Int> = _seriesPercent.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadStats()
    }

    private fun loadStats() {
        val p = prefs ?: return
        // FIX #1: All defaults are 0 — no fabricated engagement on fresh install
        val totalSec = p.getLong(KEY_TOTAL_WATCH_SECONDS, 0L)
        val dailySec = p.getLong(KEY_DAILY_WATCH_SECONDS, 0L)
        val lastDate = p.getString(KEY_LAST_WATCH_DATE, "") ?: ""
        val streak = p.getInt(KEY_STREAK_COUNT, 0)

        val animeSec = p.getLong(KEY_ANIME_SECONDS, 0L)
        val movieSec = p.getLong(KEY_MOVIE_SECONDS, 0L)
        val seriesSec = p.getLong(KEY_SERIES_SECONDS, 0L)

        val todayStr = getTodayDateString()

        val currentDailySec = if (lastDate == todayStr) dailySec else 0L
        // FIX #4: Blank lastDate = 0 streak (never watched), not 3
        val currentStreak = when {
            lastDate.isBlank() -> 0
            lastDate == todayStr -> streak.coerceAtLeast(1)
            isYesterday(lastDate) -> streak
            else -> 0
        }

        _totalWatchHours.value = String.format(Locale.US, "%.1fh", totalSec / 3600.0)
        _dailyWatchFormatted.value = formatMinutes(currentDailySec)
        _streakDays.value = currentStreak

        calculateCategoryPercentages(animeSec, movieSec, seriesSec)
    }

    /**
     * FIX #2: Removed coerceIn(10, 80) — percentages now reflect actual data honestly.
     * If a user only watches movies, anime shows 0%. No artificial floors or ceilings.
     */
    private fun calculateCategoryPercentages(animeSec: Long, movieSec: Long, seriesSec: Long) {
        val sum = animeSec + movieSec + seriesSec
        if (sum == 0L) {
            _animePercent.value = 0
            _moviePercent.value = 0
            _seriesPercent.value = 0
            return
        }
        _animePercent.value = ((animeSec * 100) / sum).toInt()
        _moviePercent.value = ((movieSec * 100) / sum).toInt()
        _seriesPercent.value = (100 - _animePercent.value - _moviePercent.value).coerceAtLeast(0)
    }

    fun addWatchTime(seconds: Long, category: String = "ANIME") {
        val p = prefs ?: return
        val todayStr = getTodayDateString()
        val lastDate = p.getString(KEY_LAST_WATCH_DATE, "") ?: ""
        var streak = p.getInt(KEY_STREAK_COUNT, 0)

        // FIX #1: Read with honest defaults (0L, 0)
        val totalSec = p.getLong(KEY_TOTAL_WATCH_SECONDS, 0L) + seconds
        val prevDailySec = if (lastDate == todayStr) p.getLong(KEY_DAILY_WATCH_SECONDS, 0L) else 0L
        val newDailySec = prevDailySec + seconds

        var animeSec = p.getLong(KEY_ANIME_SECONDS, 0L)
        var movieSec = p.getLong(KEY_MOVIE_SECONDS, 0L)
        var seriesSec = p.getLong(KEY_SERIES_SECONDS, 0L)

        when (category.uppercase()) {
            "ANIME" -> animeSec += seconds
            "MOVIE" -> movieSec += seconds
            "WEB_SERIES", "SERIES" -> seriesSec += seconds
            else -> animeSec += seconds
        }

        if (lastDate != todayStr) {
            // FIX #4: Blank lastDate with new watch = streak of 1, not inherited from fabricated default
            streak = if (lastDate.isBlank()) 1
                     else if (isYesterday(lastDate)) streak + 1
                     else 1
        }

        p.edit()
            .putLong(KEY_TOTAL_WATCH_SECONDS, totalSec)
            .putLong(KEY_DAILY_WATCH_SECONDS, newDailySec)
            .putString(KEY_LAST_WATCH_DATE, todayStr)
            .putInt(KEY_STREAK_COUNT, streak)
            .putLong(KEY_ANIME_SECONDS, animeSec)
            .putLong(KEY_MOVIE_SECONDS, movieSec)
            .putLong(KEY_SERIES_SECONDS, seriesSec)
            .apply()

        _totalWatchHours.value = String.format(Locale.US, "%.1fh", totalSec / 3600.0)
        _dailyWatchFormatted.value = formatMinutes(newDailySec)
        _streakDays.value = streak

        calculateCategoryPercentages(animeSec, movieSec, seriesSec)
    }

    private fun formatMinutes(seconds: Long): String {
        val mins = seconds / 60
        return if (mins >= 60) {
            val hrs = mins / 60
            val remMins = mins % 60
            "${hrs}h ${remMins}m"
        } else {
            "${mins}m"
        }
    }

    private val dateFormatter by lazy {
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.US)
    }

    private fun getTodayDateString(): String {
        return java.time.LocalDate.now().format(dateFormatter)
    }

    /**
     * FIX #19 & #20: Thread-safe java.time date comparison.
     * Only exactly 1 day ago counts as "yesterday".
     */
    private fun isYesterday(dateStr: String): Boolean {
        return try {
            val date = java.time.LocalDate.parse(dateStr, dateFormatter)
            date == java.time.LocalDate.now().minusDays(1)
        } catch (e: Exception) {
            false
        }
    }
}
