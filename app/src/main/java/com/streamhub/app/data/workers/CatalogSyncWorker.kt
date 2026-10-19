package com.streamhub.app.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.NotificationAlertManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.FirebaseRepository
import kotlinx.coroutines.tasks.await

/**
 * Lightweight periodic background worker:
 * - Wakes up periodically (every 8 hours) with CONNECTED network constraint.
 * - Checks Firestore catalog for new episodes or franchise installments belonging to user's My List.
 * - Posts high-priority Android notifications if new content is detected.
 * - 0 bytes written to Firebase, 100% read-only, shuts down in < 2 seconds.
 */
class CatalogSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "CatalogSyncWorker"
        const val WORK_NAME = "StreamHubCatalogSync"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Executing periodic background catalog sync check...")

        NotificationAlertManager.init(applicationContext)
        if (!NotificationAlertManager.alertsEnabled.value) {
            Log.d(TAG, "New episode & franchise alerts are disabled in settings. Skipping sync.")
            return Result.success()
        }

        return try {
            MyListManager.init(applicationContext)
            val myListIds = MyListManager.myListFlow.value
            if (myListIds.isEmpty()) {
                Log.d(TAG, "My List is empty. No notifications to evaluate.")
                return Result.success()
            }

            // 1. Fetch catalog via Cloudflare Edge (0 Firebase reads)
            val edgeCatalog = runCatching {
                com.streamhub.app.data.api.CatalogEdgeClient.fetchCatalog().getOrNull()
            }.getOrNull()

            val catalog = if (!edgeCatalog.isNullOrEmpty()) {
                Log.d(TAG, "Background sync using Cloudflare Edge (0 Firebase reads) for ${edgeCatalog.size} items.")
                com.streamhub.app.data.CatalogCacheManager.saveCatalogSnapshot(applicationContext, edgeCatalog)
                edgeCatalog
            } else {
                // Read from local disk cache if Cloudflare Edge is unreachable
                val cached = com.streamhub.app.data.CatalogCacheManager.loadCachedCatalog(applicationContext)
                Log.d(TAG, "Background sync using local disk cache (${cached.size} items, 0 Firebase reads)")
                cached
            }

            if (catalog.isNotEmpty()) {
                NotificationAlertManager.checkAndNotifyUpdates(applicationContext, catalog, myListIds)
                Log.i(TAG, "Background sync completed successfully for ${catalog.size} catalog items.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "CatalogSyncWorker encountered an error", e)
            Result.retry()
        }
    }
}
