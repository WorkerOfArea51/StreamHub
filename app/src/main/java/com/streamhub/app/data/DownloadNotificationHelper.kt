package com.streamhub.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.streamhub.app.MainActivity
import com.streamhub.app.R

/**
 * Background Download Notification Manager:
 * - Shows ongoing progress notifications with Pause, Resume, and Cancel actions in the notification drawer.
 * - Displays active download percentage, remaining MB, and completion alerts.
 */
object DownloadNotificationHelper {

    private const val CHANNEL_ID = "streamhub_downloads_channel"
    private const val CHANNEL_NAME = "Offline Downloads"

    const val ACTION_PAUSE_DOWNLOAD = "com.streamhub.app.ACTION_PAUSE_DOWNLOAD"
    const val ACTION_RESUME_DOWNLOAD = "com.streamhub.app.ACTION_RESUME_DOWNLOAD"
    const val ACTION_CANCEL_DOWNLOAD = "com.streamhub.app.ACTION_CANCEL_DOWNLOAD"

    const val EXTRA_MEDIA_ID = "extra_media_id"
    const val EXTRA_EPISODE_INDEX = "extra_episode_index"
    const val EXTRA_DOWNLOAD_ID = "extra_download_id"

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress and completion status for offline streaming"
                setShowBadge(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun getSafeNotificationId(downloadId: Long): Int {
        // Mask to 27 bits (max ~134M) so multiplying by 10 stays well under Int.MAX_VALUE (2.14B)
        return (downloadId.hashCode() and 0x07FFFFFF).coerceAtLeast(1)
    }

    fun showProgress(
        context: Context,
        downloadId: Long,
        mediaId: String = "",
        episodeIndex: Int = 0,
        mediaTitle: String,
        episodeTitle: String,
        progressPercent: Int,
        downloadedMb: Double = 0.0,
        totalMb: Double = 0.0
    ) {
        initChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notifId = getSafeNotificationId(downloadId)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId * 10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause PendingIntent
        val pauseIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = ACTION_PAUSE_DOWNLOAD
            putExtra(EXTRA_MEDIA_ID, mediaId)
            putExtra(EXTRA_EPISODE_INDEX, episodeIndex)
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            context,
            (notifId * 10) + 1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel PendingIntent
        val cancelIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            putExtra(EXTRA_MEDIA_ID, mediaId)
            putExtra(EXTRA_EPISODE_INDEX, episodeIndex)
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            (notifId * 10) + 2,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sizeInfo = if (totalMb > 0) {
            " • %.1f / %.1f MB".format(downloadedMb, totalMb)
        } else ""

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Downloading: $mediaTitle")
            .setContentText("$episodeTitle ($progressPercent%)$sizeInfo")
            .setProgress(100, progressPercent.coerceIn(0, 100), progressPercent <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)

        try {
            nm.notify(notifId, builder.build())
        } catch (_: SecurityException) {}
    }

    fun showPaused(
        context: Context,
        downloadId: Long,
        mediaId: String = "",
        episodeIndex: Int = 0,
        mediaTitle: String,
        episodeTitle: String,
        progressPercent: Int
    ) {
        initChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notifId = getSafeNotificationId(downloadId)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId * 10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Resume PendingIntent
        val resumeIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = ACTION_RESUME_DOWNLOAD
            putExtra(EXTRA_MEDIA_ID, mediaId)
            putExtra(EXTRA_EPISODE_INDEX, episodeIndex)
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val resumePendingIntent = PendingIntent.getBroadcast(
            context,
            (notifId * 10) + 3,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel PendingIntent
        val cancelIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            putExtra(EXTRA_MEDIA_ID, mediaId)
            putExtra(EXTRA_EPISODE_INDEX, episodeIndex)
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            (notifId * 10) + 2,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Paused: $mediaTitle")
            .setContentText("$episodeTitle (Paused • $progressPercent%)")
            .setProgress(100, progressPercent.coerceIn(0, 100), false)
            .setOngoing(false)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)

        try {
            nm.notify(notifId, builder.build())
        } catch (_: SecurityException) {}
    }

    fun showCompleted(
        context: Context,
        downloadId: Long,
        mediaTitle: String,
        episodeTitle: String
    ) {
        initChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notifId = getSafeNotificationId(downloadId)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId * 10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Download Complete: $mediaTitle")
            .setContentText("$episodeTitle is ready for offline streaming")
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        try {
            nm.notify(notifId, builder.build())
        } catch (_: SecurityException) {}
    }

    fun cancel(context: Context, downloadId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(getSafeNotificationId(downloadId))
    }
}
