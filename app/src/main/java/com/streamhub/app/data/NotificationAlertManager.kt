package com.streamhub.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.streamhub.app.MainActivity
import com.streamhub.app.R
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production Episode Notification Alerts Manager:
 * - Monitors catalog updates for shows added to user's "My List"
 * - Posts Android system notifications when new episodes release
 * - Configurable notification preference (toggle on/off in Settings)
 */
object NotificationAlertManager {

    private const val TAG = "NotificationAlertManager"
    private const val PREFS_NAME = "streamhub_notification_prefs"
    private const val KEY_ALERTS_ENABLED = "alerts_enabled"
    private const val KEY_SEEN_EPISODE_COUNTS = "seen_episode_counts_"

    private const val CHANNEL_ID = "streamhub_episode_alerts"
    private const val CHANNEL_NAME = "New Episode Alerts"

    private var prefs: SharedPreferences? = null

    private val _alertsEnabled = MutableStateFlow(true)
    val alertsEnabled: StateFlow<Boolean> = _alertsEnabled.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _alertsEnabled.value = prefs?.getBoolean(KEY_ALERTS_ENABLED, true) ?: true

        createNotificationChannel(context)
    }

    fun setAlertsEnabled(context: Context, enabled: Boolean) {
        _alertsEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_ALERTS_ENABLED, enabled)?.apply()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when new episodes release for your bookmarked shows"
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if any show in user's My List has received a new episode, and triggers an Android notification.
     */
    fun checkAndNotifyNewEpisodes(context: Context, catalog: List<MediaItem>, myListIds: Set<String>) {
        if (!_alertsEnabled.value || catalog.isEmpty() || myListIds.isEmpty()) return

        val p = prefs ?: return
        val editor = p.edit()

        for (item in catalog) {
            if (!myListIds.contains(item.id)) continue

            val currentEpCount = item.episodes.size
            val lastSeenCount = p.getInt("$KEY_SEEN_EPISODE_COUNTS${item.id}", -1)

            if (lastSeenCount != -1 && currentEpCount > lastSeenCount) {
                val newEpIndex = currentEpCount
                val latestEp = item.episodes.lastOrNull()
                val epTitle = latestEp?.title?.ifEmpty { "Episode $newEpIndex" } ?: "Episode $newEpIndex"

                sendEpisodeNotification(
                    context = context,
                    mediaTitle = item.title,
                    episodeTitle = epTitle,
                    mediaId = item.id,
                    notificationId = item.id.hashCode() and 0x7FFFFFFF
                )
            }

            editor.putInt("$KEY_SEEN_EPISODE_COUNTS${item.id}", currentEpCount)
        }
        editor.apply()
    }

    private fun sendEpisodeNotification(
        context: Context,
        mediaTitle: String,
        episodeTitle: String,
        mediaId: String,
        notificationId: Int
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("mediaId", mediaId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🍿 New Episode Available!")
                .setContentText("$mediaTitle - $episodeTitle is now ready to stream!")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$mediaTitle\n$episodeTitle is now available on StreamHub! Tap to watch now.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(notificationId, builder.build())

            Log.i(TAG, "Triggered episode notification for $mediaTitle - $episodeTitle")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}", e)
        }
    }
}
