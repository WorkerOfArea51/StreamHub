package com.streamhub.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.streamhub.app.MainActivity
import com.streamhub.app.R

class StreamMediaService : MediaSessionService() {

    companion object {
        private const val TAG = "StreamMediaService"
        private const val CHANNEL_ID = "streamhub_playback"
        private const val NOTIFICATION_ID = 1001
    }

    private var mediaSession: MediaSession? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val sharedPlayer = StreamPlayerViewModel.currentPlayer
        if (sharedPlayer != null) {
            Log.d(TAG, "Using shared ExoPlayer from ViewModel")
            mediaSession = MediaSession.Builder(this, sharedPlayer)
                .setCallback(StreamSessionCallback())
                .build()
        } else {
            Log.w(TAG, "ViewModel player not available, creating standalone player")
            val player = ExoPlayer.Builder(this)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build()
            mediaSession = MediaSession.Builder(this, player)
                .setCallback(StreamSessionCallback())
                .build()
        }

        startForegroundNotification()
        acquireWakeLock()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun startForegroundNotification() {
        val notification = buildPlaybackNotification("StreamHub", "Playing media")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildPlaybackNotification(title: String, subtitle: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when media is playing in the background"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = pm.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "StreamHub::PlaybackWakeLock"
            )
        }
        wakeLock?.acquire(3 * 60 * 60 * 1000L)
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && player.isPlaying) {
            player.stop()
            player.clearMediaItems()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        releaseWakeLock()
        mediaSession?.run {
            val sharedPlayer = StreamPlayerViewModel.currentPlayer
            if (player !== sharedPlayer) {
                player.release()
            }
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private inner class StreamSessionCallback : MediaSession.Callback
}
