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
 * - All-time watch duration (in milliseconds / hours)
 * - Today's watch duration (in milliseconds / minutes / hours)
 * - Active daily watch streak counter
 * - Category Watch Breakdown (% Anime, % Movies, % Web Series)
 */
object UserStatsManager {

    private const val PREFS_NAME = "streamhub_user_stats"
    private const val KEY_TOTAL_WATCH_MS = "total_watch_millis"
    private const val KEY_DAILY_WATCH_MS = "daily_watch_millis"
    private const val KEY_LAST_WATCH_DATE = "last_watch_date"
    private const val KEY_STREAK_COUNT = "streak_count"
    private const val KEY_ANIME_SECONDS = "anime_watch_seconds"
    private const val KEY_MOVIE_SECONDS = "movie_watch_seconds"
    private const val KEY_SERIES_SECONDS = "series_watch_seconds"
    private const val KEY_CLEANED_CORRUPTED_STATS_V2 = "cleaned_corrupted_stats_v2"

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
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sanitizeCorruptedLegacyData()
        loadStats()
    }

    private fun sanitizeCorruptedLegacyData() {
        val p = prefs ?: return
        if (!p.getBoolean(KEY_CLEANED_CORRUPTED_STATS_V2, false)) {
            // Reset the old 1000x inflated values once
            p.edit()
                .putLong(KEY_TOTAL_WATCH_MS, 0L)
                .putLong(KEY_DAILY_WATCH_MS, 0L)
                .putLong(KEY_ANIME_SECONDS, 0L)
                .putLong(KEY_MOVIE_SECONDS, 0L)
                .putLong(KEY_SERIES_SECONDS, 0L)
                .putBoolean(KEY_CLEANED_CORRUPTED_STATS_V2, true)
                .apply()
        }
    }

    @Synchronized
    private fun loadStats() {
        val p = prefs ?: return
        val totalMs = p.getLong(KEY_TOTAL_WATCH_MS, 0L)
        val dailyMs = p.getLong(KEY_DAILY_WATCH_MS, 0L)
        val lastDate = p.getString(KEY_LAST_WATCH_DATE, "") ?: ""
        val streak = p.getInt(KEY_STREAK_COUNT, 0)

        val animeSec = p.getLong(KEY_ANIME_SECONDS, 0L)
        val movieSec = p.getLong(KEY_MOVIE_SECONDS, 0L)
        val seriesSec = p.getLong(KEY_SERIES_SECONDS, 0L)

        val todayStr = getTodayDateString()

        val currentDailyMs = if (lastDate == todayStr) dailyMs else 0L
        val currentStreak = when {
            lastDate.isBlank() -> 0
            lastDate == todayStr -> streak.coerceAtLeast(1)
            isYesterday(lastDate) -> streak
            else -> 0
        }

        _totalWatchHours.value = String.format(Locale.US, "%.1fh", totalMs / (3600.0 * 1000.0))
        _dailyWatchFormatted.value = formatMillis(currentDailyMs)
        _streakDays.value = currentStreak

        calculateCategoryPercentages(animeSec, movieSec, seriesSec)
    }

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

    @Synchronized
    fun addWatchTime(seconds: Long, category: String = "ANIME") {
        addWatchTimeMillis(seconds * 1000L, category)
    }

    @Synchronized
    fun addWatchTimeMillis(millis: Long, category: String = "ANIME") {
        if (millis <= 0L) return
        val safeMillis = millis.coerceAtMost(60_000L) // max 1 min step

        val p = prefs ?: return
        val todayStr = getTodayDateString()
        val lastDate = p.getString(KEY_LAST_WATCH_DATE, "") ?: ""
        var streak = p.getInt(KEY_STREAK_COUNT, 0)

        val totalMs = p.getLong(KEY_TOTAL_WATCH_MS, 0L) + safeMillis
        val prevDailyMs = if (lastDate == todayStr) p.getLong(KEY_DAILY_WATCH_MS, 0L) else 0L
        val newDailyMs = prevDailyMs + safeMillis

        var animeSec = p.getLong(KEY_ANIME_SECONDS, 0L)
        var movieSec = p.getLong(KEY_MOVIE_SECONDS, 0L)
        var seriesSec = p.getLong(KEY_SERIES_SECONDS, 0L)

        val addedSec = safeMillis / 1000L
        when (category.uppercase()) {
            "ANIME" -> animeSec += addedSec
            "MOVIE" -> movieSec += addedSec
            "WEB_SERIES", "SERIES" -> seriesSec += addedSec
            else -> { /* Ignore */ }
        }

        if (lastDate != todayStr) {
            streak = if (lastDate.isBlank()) 1
                     else if (isYesterday(lastDate)) streak + 1
                     else 1
        }

        p.edit()
            .putLong(KEY_TOTAL_WATCH_MS, totalMs)
            .putLong(KEY_DAILY_WATCH_MS, newDailyMs)
            .putString(KEY_LAST_WATCH_DATE, todayStr)
            .putInt(KEY_STREAK_COUNT, streak)
            .putLong(KEY_ANIME_SECONDS, animeSec)
            .putLong(KEY_MOVIE_SECONDS, movieSec)
            .putLong(KEY_SERIES_SECONDS, seriesSec)
            .apply()

        _totalWatchHours.value = String.format(Locale.US, "%.1fh", totalMs / (3600.0 * 1000.0))
        _dailyWatchFormatted.value = formatMillis(newDailyMs)
        _streakDays.value = streak

        calculateCategoryPercentages(animeSec, movieSec, seriesSec)
    }

    private fun formatMillis(millis: Long): String {
        val totalSec = millis / 1000
        val mins = totalSec / 60
        return if (mins >= 60) {
            val hrs = mins / 60
            val remMins = mins % 60
            "${hrs}h ${remMins}m"
        } else {
            "${mins}m"
        }
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun isYesterday(dateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calYesterday = java.util.Calendar.getInstance()
            calYesterday.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(calYesterday.time)
            dateStr == yesterdayStr
        } catch (e: Exception) {
            false
        }
    }
}
