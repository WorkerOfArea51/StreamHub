package com.streamhub.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles action clicks (Pause, Resume, Cancel) from download notifications.
 */
class DownloadActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val mediaId = intent.getStringExtra(DownloadNotificationHelper.EXTRA_MEDIA_ID) ?: ""
        val episodeIndex = intent.getIntExtra(DownloadNotificationHelper.EXTRA_EPISODE_INDEX, 0)
        val downloadId = intent.getLongExtra(DownloadNotificationHelper.EXTRA_DOWNLOAD_ID, -1L)

        Log.i("DownloadActionReceiver", "Received download action: $action (mediaId=$mediaId, epIndex=$episodeIndex, downloadId=$downloadId)")

        if (mediaId.isBlank()) {
            if (downloadId != -1L) {
                DownloadNotificationHelper.cancel(context, downloadId)
            }
            return
        }

        when (action) {
            DownloadNotificationHelper.ACTION_PAUSE_DOWNLOAD -> {
                DownloadManager.pauseDownloadByKeys(mediaId, episodeIndex)
            }
            DownloadNotificationHelper.ACTION_RESUME_DOWNLOAD -> {
                DownloadManager.resumeDownloadByKeys(mediaId, episodeIndex, context)
            }
            DownloadNotificationHelper.ACTION_CANCEL_DOWNLOAD -> {
                DownloadManager.cancelDownloadByKeys(mediaId, episodeIndex)
            }
        }
    }
}
