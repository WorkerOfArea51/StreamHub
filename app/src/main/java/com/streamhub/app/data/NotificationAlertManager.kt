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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.streamhub.app.MainActivity
import com.streamhub.app.R
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.workers.CatalogSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * Production Episode & Franchise Notification Alerts Manager:
 * - Monitors catalog updates for shows added to user's "My List"
 * - Posts Android system notifications when new episodes release
 * - Detects brand new installments, seasons, prequels, and movies in bookmarked franchise universes
 * - Periodic background sync via Android Jetpack WorkManager
 * - Configurable notification preference (toggle on/off in Settings)
 */
object NotificationAlertManager {

    private const val TAG = "NotificationAlertManager"
    private const val PREFS_NAME = "streamhub_notification_prefs"
    private const val KEY_ALERTS_ENABLED = "alerts_enabled"
    private const val KEY_SEEN_EPISODE_COUNTS = "seen_episode_counts_"
    private const val KEY_SEEN_FRANCHISE_MEDIA_IDS = "seen_franchise_media_ids_"

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

        if (_alertsEnabled.value) {
            schedulePeriodicSync(appContext)
        }
    }

    fun setAlertsEnabled(context: Context, enabled: Boolean) {
        _alertsEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_ALERTS_ENABLED, enabled)?.apply()
        if (enabled) {
            schedulePeriodicSync(context)
        } else {
            cancelPeriodicSync(context)
        }
    }

    fun schedulePeriodicSync(context: Context) {
        if (!_alertsEnabled.value) return
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<CatalogSyncWorker>(8, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                CatalogSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.d(TAG, "Scheduled periodic background catalog sync worker (every 8h)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic sync", e)
        }
    }

    fun cancelPeriodicSync(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(CatalogSyncWorker.WORK_NAME)
            Log.d(TAG, "Cancelled periodic background catalog sync worker")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel periodic sync", e)
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Episode & Franchise Alerts Channel
            val episodeChannel = NotificationChannel(
                CHANNEL_EPISODE_ID,
                CHANNEL_EPISODE_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when new episodes or franchise releases drop for your bookmarked shows"
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
     * Comprehensive alert check:
     * 1. Checks if existing bookmarked shows have received new episodes.
     * 2. Checks if any new franchise installments (seasons, sequels, movies, prequels) have released
     *    in the franchise universes of the user's My List shows.
     */
    fun checkAndNotifyUpdates(context: Context, catalog: List<MediaItem>, myListIds: Set<String>) {
        if (!_alertsEnabled.value || catalog.isEmpty() || myListIds.isEmpty()) return

        val p = prefs ?: return
        val editor = p.edit()

        // 1. Check existing My List titles for new episodes
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

        // 2. Check for new Franchise releases (Seasons, Sequels, Prequels, Movies, Arcs)
        val bookmarkedItems = catalog.filter { myListIds.contains(it.id) }
        val franchiseMap = mutableMapOf<String, Pair<String, MediaItem>>() // franchiseId -> (universeTitle, referenceItem)
        for (b in bookmarkedItems) {
            val fId = FranchiseManager.getFranchiseId(b)
            if (fId.isNotBlank() && !franchiseMap.containsKey(fId)) {
                val universeTitle = FranchiseManager.getFranchiseTitle(b)
                franchiseMap[fId] = Pair(universeTitle, b)
            }
        }

        for ((fId, pair) in franchiseMap) {
            val (universeTitle, referenceItem) = pair
            val seenKey = "$KEY_SEEN_FRANCHISE_MEDIA_IDS$fId"
            val seenSet = p.getStringSet(seenKey, null)
            val franchiseCatalogItems = catalog.filter { FranchiseManager.getFranchiseId(it) == fId }
            val currentItemIds = franchiseCatalogItems.map { it.id }.toSet()

            if (seenSet == null) {
                // Baseline seeding: franchise newly indexed, record all existing titles without alerting
                editor.putStringSet(seenKey, currentItemIds)
            } else {
                // Check if any brand-new installment has been added to this franchise
                val brandNewItems = franchiseCatalogItems.filter { !seenSet.contains(it.id) }
                for (newItem in brandNewItems) {
                    val tag = FranchiseManager.getFranchiseTag(newItem, referenceItem).ifBlank { "New Installment" }
                    sendFranchiseNotification(
                        context = context,
                        franchiseTitle = universeTitle,
                        newShowTitle = newItem.title,
                        relationTag = tag,
                        mediaId = newItem.id,
                        notificationId = newItem.id.hashCode() and 0x7FFFFFFF
                    )
                }
                editor.putStringSet(seenKey, seenSet + currentItemIds)
            }
        }

        editor.apply()
    }

    /**
     * Backward-compatible alias for existing call sites.
     */
    fun checkAndNotifyNewEpisodes(context: Context, catalog: List<MediaItem>, myListIds: Set<String>) {
        checkAndNotifyUpdates(context, catalog, myListIds)
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

            val appIcon = NotificationIconHelper.getAppIconBitmap(context)
            val builder = NotificationCompat.Builder(context, CHANNEL_EPISODE_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFFE50914.toInt())
                .apply {
                    if (appIcon != null) setLargeIcon(appIcon)
                }
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

    private fun sendFranchiseNotification(
        context: Context,
        franchiseTitle: String,
        newShowTitle: String,
        relationTag: String,
        mediaId: String,
        notificationId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted — skipping franchise notification")
                return
            }
        }

        val lastTime = lastNotificationTime[mediaId] ?: 0L
        if (System.currentTimeMillis() - lastTime < NOTIFICATION_COOLDOWN_MS) {
            Log.d(TAG, "Notification rate limited for franchise mediaId: $mediaId")
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

            val appIcon = NotificationIconHelper.getAppIconBitmap(context)
            val subText = if (relationTag.isNotBlank() && !relationTag.equals("CURRENT", true)) relationTag else "New Release"
            val builder = NotificationCompat.Builder(context, CHANNEL_EPISODE_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFFE50914.toInt())
                .apply {
                    if (appIcon != null) setLargeIcon(appIcon)
                }
                .setContentTitle("🎬 New Franchise Release: $newShowTitle")
                .setContentText("A new $subText in the $franchiseTitle universe is now available!")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("🎬 $newShowTitle\n\nA new $subText in the \"$franchiseTitle\" universe is now streaming on StreamHub! Tap to watch now.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(notificationId, builder.build())

            Log.i(TAG, "Triggered franchise notification for $franchiseTitle: $newShowTitle ($relationTag)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send franchise notification: ${e.message}", e)
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

            val appIcon = NotificationIconHelper.getAppIconBitmap(context)
            val builder = NotificationCompat.Builder(context, CHANNEL_ADMIN_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFFE50914.toInt())
                .apply {
                    if (appIcon != null) setLargeIcon(appIcon)
                }
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
