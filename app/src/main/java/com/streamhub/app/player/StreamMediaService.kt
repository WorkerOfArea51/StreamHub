package com.streamhub.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.streamhub.app.MainActivity
import com.streamhub.app.R

class StreamMediaService : MediaSessionService() {

    companion object {
        private const val TAG = "StreamMediaService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "streamhub_media_playback"
        private const val WAKELOCK_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
    }

    private var mediaSession: MediaSession? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var fallbackPlayer: ExoPlayer? = null
    private var playerListener: Player.Listener? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val sharedPlayer = StreamPlayerViewModel.currentPlayer
        val player = sharedPlayer ?: createFallbackPlayer().also { fallbackPlayer = it }

        // FIX: Register fallback player in PlayerHolder so the ViewModel can pick it up
        // if it (re)initializes after the service has started.
        if (sharedPlayer == null) {
            PlayerHolder.setPlayer(player)
        }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(StreamSessionCallback())
            .build()

        // FIX: Attach a listener to update the foreground notification when media item changes.
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateForegroundNotification()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateForegroundNotification()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateForegroundNotification()
            }
        }
        playerListener = listener
        player.addListener(listener)

        // FIX: Start foreground AFTER session is built, so notification title reflects actual media.
        startForegroundNotification()
        acquireWakeLock()
    }

    private fun createFallbackPlayer(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun startForegroundNotification() {
        // FIX: Pull title from mediaSession.player (now guaranteed non-null).
        val player = mediaSession?.player
        val title = player?.currentMediaItem?.mediaMetadata?.title?.toString()
            ?.ifBlank { "StreamHub" } ?: "StreamHub"
        val subtitle = if (player?.isPlaying == true) "Playing media" else "Paused"
        val notification = buildPlaybackNotification(title, subtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * FIX: Update the existing foreground notification without re-calling startForeground
     * (which can crash on Android 12+ if called too frequently).
     */
    private fun updateForegroundNotification() {
        val player = mediaSession?.player ?: return
        val title = player.currentMediaItem?.mediaMetadata?.title?.toString()
            ?.ifBlank { "StreamHub" } ?: "StreamHub"
        val subtitle = when {
            player.isPlaying -> "Playing media"
            player.playbackState == Player.STATE_BUFFERING -> "Buffering…"
            player.playbackState == Player.STATE_ENDED -> "Playback ended"
            else -> "Paused"
        }
        val notification = buildPlaybackNotification(title, subtitle)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            nm?.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
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
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            wakeLock = pm?.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "StreamHub::PlaybackWakeLock"
            )
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(WAKELOCK_TIMEOUT_MS)
        }
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
        releaseWakeLock()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        releaseWakeLock()
        // FIX: Remove listener before releasing session.
        mediaSession?.player?.let { p ->
            playerListener?.let { p.removeListener(it) }
        }
        playerListener = null

        mediaSession?.run {
            val sharedPlayer = StreamPlayerViewModel.currentPlayer
            val sessionPlayer = player
            // FIX: Release the fallback player (owned by this service).
            // Never release the shared player here — the ViewModel owns its lifecycle.
            if (sessionPlayer !== sharedPlayer) {
                sessionPlayer.release()
            }
            release()
        }
        mediaSession = null
        fallbackPlayer = null
        super.onDestroy()
    }

    private inner class StreamSessionCallback : MediaSession.Callback
}
