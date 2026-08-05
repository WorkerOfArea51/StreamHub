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
 * Manages user app usage statistics and engagement metrics:
 * - All-time watch duration (in seconds/hours)
 * - Today's watch duration (in seconds/minutes)
 * - Active daily watch streak counter (🔥 1 day, 2 days, etc.)
 */
object UserStatsManager {

    private const val PREFS_NAME = "streamhub_user_stats"
    private const val KEY_TOTAL_WATCH_SECONDS = "total_watch_seconds"
    private const val KEY_DAILY_WATCH_SECONDS = "daily_watch_seconds"
    private const val KEY_LAST_WATCH_DATE = "last_watch_date"
    private const val KEY_STREAK_COUNT = "streak_count"

    private var prefs: SharedPreferences? = null

    private val _totalWatchHours = MutableStateFlow("0.0h")
    val totalWatchHours: StateFlow<String> = _totalWatchHours.asStateFlow()

    private val _dailyWatchFormatted = MutableStateFlow("0m")
    val dailyWatchFormatted: StateFlow<String> = _dailyWatchFormatted.asStateFlow()

    private val _streakDays = MutableStateFlow(1)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadStats()
    }

    private fun loadStats() {
        val p = prefs ?: return
        val totalSec = p.getLong(KEY_TOTAL_WATCH_SECONDS, 0L)
        val dailySec = p.getLong(KEY_DAILY_WATCH_SECONDS, 0L)
        val lastDate = p.getString(KEY_LAST_WATCH_DATE, "") ?: ""
        val streak = p.getInt(KEY_STREAK_COUNT, 1)

        val todayStr = getTodayDateString()

        // If today is a new day, reset daily watch seconds and update streak
        val currentDailySec = if (lastDate == todayStr) dailySec else 0L
        val currentStreak = when {
            lastDate.isBlank() -> 1
            lastDate == todayStr -> streak
            isYesterday(lastDate) -> streak
            else -> 1 // Streak broken if skipped a day
        }

        _totalWatchHours.value = String.format(Locale.US, "%.1fh", totalSec / 3600.0)
        _dailyWatchFormatted.value = formatMinutes(currentDailySec)
        _streakDays.value = currentStreak
    }

    /**
     * Record watched seconds (called periodically during playback).
     */
    fun addWatchTime(seconds: Long) {
        val p = prefs ?: return
        val todayStr = getTodayDateString()
        val lastDate = p.getString(KEY_LAST_WATCH_DATE, "") ?: ""
        var streak = p.getInt(KEY_STREAK_COUNT, 1)

        val totalSec = p.getLong(KEY_TOTAL_WATCH_SECONDS, 0L) + seconds
        val prevDailySec = if (lastDate == todayStr) p.getLong(KEY_DAILY_WATCH_SECONDS, 0L) else 0L
        val newDailySec = prevDailySec + seconds

        if (lastDate != todayStr) {
            streak = if (isYesterday(lastDate)) streak + 1 else 1
        }

        p.edit()
            .putLong(KEY_TOTAL_WATCH_SECONDS, totalSec)
            .putLong(KEY_DAILY_WATCH_SECONDS, newDailySec)
            .putString(KEY_LAST_WATCH_DATE, todayStr)
            .putInt(KEY_STREAK_COUNT, streak)
            .apply()

        _totalWatchHours.value = String.format(Locale.US, "%.1fh", totalSec / 3600.0)
        _dailyWatchFormatted.value = formatMinutes(newDailySec)
        _streakDays.value = streak
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

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun isYesterday(dateStr: String): Boolean {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateStr) ?: return false
            val diff = Date().time - date.time
            val daysDiff = diff / (1000 * 60 * 60 * 24)
            return daysDiff in 1..2
        } catch (e: Exception) {
            return false
        }
    }
}
