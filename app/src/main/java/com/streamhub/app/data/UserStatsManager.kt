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

    private var cachedTotalMs = 0L
    private var cachedDailyMs = 0L
    private var cachedLastDate = ""
    private var cachedStreak = 0
    private var cachedAnimeMs = 0L
    private var cachedMovieMs = 0L
    private var cachedSeriesMs = 0L
    private var lastDiskSaveTimestamp = 0L
    private const val DISK_SAVE_INTERVAL_MS = 5_000L // 5s debounce for flash storage protection

    @Synchronized
    private fun loadStats() {
        val p = prefs ?: return
        cachedTotalMs = p.getLong(KEY_TOTAL_WATCH_MS, 0L)
        cachedDailyMs = p.getLong(KEY_DAILY_WATCH_MS, 0L)
        cachedLastDate = p.getString(KEY_LAST_WATCH_DATE, "") ?: ""
        cachedStreak = p.getInt(KEY_STREAK_COUNT, 0)

        cachedAnimeMs = p.getLong(KEY_ANIME_SECONDS, 0L) * 1000L
        cachedMovieMs = p.getLong(KEY_MOVIE_SECONDS, 0L) * 1000L
        cachedSeriesMs = p.getLong(KEY_SERIES_SECONDS, 0L) * 1000L

        val todayStr = getTodayDateString()

        val currentDailyMs = if (cachedLastDate == todayStr) cachedDailyMs else 0L
        val currentStreak = when {
            cachedLastDate.isBlank() -> 0
            cachedLastDate == todayStr -> cachedStreak.coerceAtLeast(1)
            isYesterday(cachedLastDate) -> cachedStreak
            else -> 0
        }

        _totalWatchHours.value = String.format(Locale.US, "%.1fh", cachedTotalMs / (3600.0 * 1000.0))
        _dailyWatchFormatted.value = formatMillis(currentDailyMs)
        _streakDays.value = currentStreak

        calculateCategoryPercentages(cachedAnimeMs / 1000L, cachedMovieMs / 1000L, cachedSeriesMs / 1000L)
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
    fun addWatchTimeMillis(millis: Long, category: String = "ANIME", forceSave: Boolean = false) {
        if (millis <= 0L) return
        val safeMillis = millis.coerceAtMost(60_000L) // max 1 min step

        val todayStr = getTodayDateString()
        if (cachedLastDate != todayStr) {
            cachedStreak = if (cachedLastDate.isBlank()) 1
            else if (isYesterday(cachedLastDate)) cachedStreak + 1
            else 1
            cachedDailyMs = safeMillis
            cachedLastDate = todayStr
        } else {
            cachedDailyMs += safeMillis
        }
        cachedTotalMs += safeMillis

        when (category.uppercase(Locale.ROOT)) {
            "ANIME" -> cachedAnimeMs += safeMillis
            "MOVIE", "MOVIES" -> cachedMovieMs += safeMillis
            "WEB_SERIES", "SERIES" -> cachedSeriesMs += safeMillis
            else -> { /* Ignore */ }
        }

        val animeSec = cachedAnimeMs / 1000L
        val movieSec = cachedMovieMs / 1000L
        val seriesSec = cachedSeriesMs / 1000L

        _totalWatchHours.value = String.format(Locale.US, "%.1fh", cachedTotalMs / (3600.0 * 1000.0))
        _dailyWatchFormatted.value = formatMillis(cachedDailyMs)
        _streakDays.value = cachedStreak

        calculateCategoryPercentages(animeSec, movieSec, seriesSec)

        val now = System.currentTimeMillis()
        if (forceSave || (now - lastDiskSaveTimestamp >= DISK_SAVE_INTERVAL_MS)) {
            flushToDiskInternal(now, animeSec, movieSec, seriesSec)
        }
    }

    @Synchronized
    fun flushToDisk() {
        val now = System.currentTimeMillis()
        val animeSec = cachedAnimeMs / 1000L
        val movieSec = cachedMovieMs / 1000L
        val seriesSec = cachedSeriesMs / 1000L
        flushToDiskInternal(now, animeSec, movieSec, seriesSec)
    }

    private fun flushToDiskInternal(now: Long, animeSec: Long, movieSec: Long, seriesSec: Long) {
        val p = prefs ?: return
        p.edit()
            .putLong(KEY_TOTAL_WATCH_MS, cachedTotalMs)
            .putLong(KEY_DAILY_WATCH_MS, cachedDailyMs)
            .putString(KEY_LAST_WATCH_DATE, cachedLastDate)
            .putInt(KEY_STREAK_COUNT, cachedStreak)
            .putLong(KEY_ANIME_SECONDS, animeSec)
            .putLong(KEY_MOVIE_SECONDS, movieSec)
            .putLong(KEY_SERIES_SECONDS, seriesSec)
            .apply()
        lastDiskSaveTimestamp = now
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
