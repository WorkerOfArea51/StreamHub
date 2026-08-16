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
 * - Shows ongoing progress notifications in the Android notification drawer.
 * - Displays active download percentage, remaining MB, and completion alerts.
 */
object DownloadNotificationHelper {

    private const val CHANNEL_ID = "streamhub_downloads_channel"
    private const val CHANNEL_NAME = "Offline Downloads"

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

    fun showProgress(
        context: Context,
        downloadId: Long,
        mediaTitle: String,
        episodeTitle: String,
        progressPercent: Int,
        downloadedMb: Double = 0.0,
        totalMb: Double = 0.0
    ) {
        initChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            downloadId.toInt(),
            intent,
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

        try {
            nm.notify(downloadId.toInt(), builder.build())
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

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            downloadId.toInt(),
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
            nm.notify(downloadId.toInt(), builder.build())
        } catch (_: SecurityException) {}
    }

    fun cancel(context: Context, downloadId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(downloadId.toInt())
    }
}
