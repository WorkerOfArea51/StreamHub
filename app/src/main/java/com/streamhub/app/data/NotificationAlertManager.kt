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

    const val CHANNEL_EPISODE_ID = "streamhub_episode_alerts"
    const val CHANNEL_EPISODE_NAME = "New Episode Alerts"

    const val CHANNEL_ADMIN_ID = "streamhub_admin_announcements"
    const val CHANNEL_ADMIN_NAME = "Admin & Community Announcements"

    private var prefs: SharedPreferences? = null

    private val _alertsEnabled = MutableStateFlow(true)
    val alertsEnabled: StateFlow<Boolean> = _alertsEnabled.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _alertsEnabled.value = prefs?.getBoolean(KEY_ALERTS_ENABLED, true) ?: true

        createNotificationChannels(appContext)
    }

    fun setAlertsEnabled(context: Context, enabled: Boolean) {
        _alertsEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_ALERTS_ENABLED, enabled)?.apply()
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Episode Alerts Channel
            val episodeChannel = NotificationChannel(
                CHANNEL_EPISODE_ID,
                CHANNEL_EPISODE_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when new episodes release for your bookmarked shows"
                enableVibration(true)
            }
            nm.createNotificationChannel(episodeChannel)

            // 2. Admin Announcements Channel (Guaranteed High Priority Heads-up)
            val adminChannel = NotificationChannel(
                CHANNEL_ADMIN_ID,
                CHANNEL_ADMIN_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important announcements, broadcasts, and direct messages from Admin"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            nm.createNotificationChannel(adminChannel)
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

    private val lastNotificationTime = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val NOTIFICATION_COOLDOWN_MS = 60 * 60 * 1000L

    private fun sendEpisodeNotification(
        context: Context,
        mediaTitle: String,
        episodeTitle: String,
        mediaId: String,
        notificationId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted — skipping notification")
                return
            }
        }

        val lastTime = lastNotificationTime[mediaId] ?: 0L
        if (System.currentTimeMillis() - lastTime < NOTIFICATION_COOLDOWN_MS) {
            Log.d(TAG, "Notification rate limited for mediaId: $mediaId")
            return
        }
        lastNotificationTime[mediaId] = System.currentTimeMillis()

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

            val builder = NotificationCompat.Builder(context, CHANNEL_EPISODE_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New Episode Available!")
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

    /**
     * Dispatches an instant remote Admin or Global Announcement notification to the Android Notification Shade.
     */
    fun sendAdminAlertNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        createNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted — skipping admin notification")
                return
            }
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ADMIN_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(longArrayOf(0, 300, 200, 300))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(notificationId, builder.build())
            Log.i(TAG, "Triggered admin alert notification: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send admin alert notification: ${e.message}", e)
        }
    }
}
