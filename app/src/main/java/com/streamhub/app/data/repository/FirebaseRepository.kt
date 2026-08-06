package com.streamhub.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State of the media catalog fetch.
 * - Loading: initial fetch has not completed yet
 * - Ready: at least one successful Firestore snapshot has been received
 *   (catalog may still be empty if Firestore has no documents)
 * - Error: Firestore returned an error AND no cached data is available
 */
sealed class CatalogState {
    data object Loading : CatalogState()
    data object Ready : CatalogState()
    data class Error(val message: String) : CatalogState()
}

/**
 * State of an admin write operation (save or delete).
 * - Idle: no operation in progress
 * - Loading: operation in flight
 * - Success: operation completed (with timestamp for "dismiss after N seconds" UI)
 * - Error: operation failed (with message)
 */
sealed class AdminOperationState {
    data object Idle : AdminOperationState()
    data object Loading : AdminOperationState()
    data class Success(val timestamp: Long = System.currentTimeMillis()) : AdminOperationState()
    data class Error(val message: String) : AdminOperationState()
}

class FirebaseRepository {

    companion object {
        private const val TAG = "FirebaseRepository"
        private const val COLLECTION_NAME = "media_content"
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    /** Active Firestore snapshot listener registration. Only one at a time. */
    private var listenerRegistration: ListenerRegistration? = null

    private val _mediaCatalog = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaCatalog: StateFlow<List<MediaItem>> = _mediaCatalog.asStateFlow()

    private val _catalogState = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val catalogState: StateFlow<CatalogState> = _catalogState.asStateFlow()

    private val _adminOperationState = MutableStateFlow<AdminOperationState>(AdminOperationState.Idle)
    val adminOperationState: StateFlow<AdminOperationState> = _adminOperationState.asStateFlow()

    init {
        loadInitialCatalog()
    }

    suspend fun connect() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            attachFirestoreListener()
        }
    }

    /**
     * Re-attach the Firestore listener. Call from UI when user taps "Retry"
     * after a CatalogState.Error.
     *
     * The previous listener is removed before attaching a new one, preventing
     * listener stacking and duplicate snapshot callbacks.
     */
    fun retry() {
        Log.d(TAG, "Manual retry requested")
        _catalogState.value = CatalogState.Loading
        removeFirestoreListener()
        attachFirestoreListener()
    }

    /**
     * Reset admin operation state back to Idle. Call from UI after showing
     * a Success/Error snackbar to dismiss it.
     */
    fun resetAdminOperationState() {
        _adminOperationState.value = AdminOperationState.Idle
    }

    /**
     * Remove the active Firestore snapshot listener.
     *
     * Call this when the repository is no longer needed (e.g. Activity destroy)
     * to prevent leaking the listener and receiving snapshots for a dead UI.
     */
    fun cleanup() {
        removeFirestoreListener()
    }

    private fun attachFirestoreListener() {
        try {
            listenerRegistration = firestore.collection(COLLECTION_NAME)
                .orderBy("title", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Firestore listener error", error)
                        // Only emit Error if we have no cached data.
                        // If we have cached data, keep showing it — degraded mode
                        // is better than blanking the catalog.
                        if (_mediaCatalog.value.isEmpty()) {
                            _catalogState.value = CatalogState.Error(error.message ?: "Unknown Firestore error")
                        } else {
                            // We have cached data — keep showing it, but mark as Ready
                            // so SplashScreen doesn't hang.
                            _catalogState.value = CatalogState.Ready
                        }
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        // No error, but no data. This is a valid state — empty catalog.
                        _catalogState.value = CatalogState.Ready
                        return@addSnapshotListener
                    }

                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(MediaItem::class.java)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse document ${doc.id}", e)
                            null
                        }
                    }

                    _mediaCatalog.value = items
                    _catalogState.value = CatalogState.Ready
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach Firestore listener", e)
            // Mark Ready with empty catalog — do not hang on splash forever
            _catalogState.value = CatalogState.Ready
        }
    }

    private fun removeFirestoreListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    /**
     * Save (or update) a media item locally and remotely.
     *
     * Updates the local catalog immediately (optimistic UI), then pushes to
     * Firestore. The result is surfaced via [adminOperationState] — admin UI
     * should observe it to show "Saved" or "Save failed: <message>".
     *
     * On save failure, the optimistic update is reverted by restoring the
     * previous item version (for updates) or removing the item (for new items).
     */
    fun saveMediaItem(item: MediaItem) {
        // Snapshot the current catalog state before optimistic update
        // so we can revert precisely on failure.
        val catalogBeforeUpdate = _mediaCatalog.value.toList()
        val existingItem = catalogBeforeUpdate.firstOrNull { it.id == item.id }

        // Optimistic local update — UI reflects the change immediately
        val current = _mediaCatalog.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        _mediaCatalog.value = current

        // Remote sync with proper success/failure callbacks
        _adminOperationState.value = AdminOperationState.Loading
        firestore.collection(COLLECTION_NAME)
            .document(item.id)
            .set(item)
            .addOnSuccessListener {
                Log.d(TAG, "Saved media item: ${item.id}")
                _adminOperationState.value = AdminOperationState.Success()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save media item: ${item.id}", e)
                _adminOperationState.value = AdminOperationState.Error(e.message ?: "Unknown error")
                // Revert the optimistic local update precisely:
                // - If this was an update, restore the previous item version
                // - If this was a new item, remove it from the catalog
                // Either way, restoring the snapshot we took before the
                // optimistic update gives the correct result.
                _mediaCatalog.value = catalogBeforeUpdate
            }
    }

    /**
     * Delete a media item locally and remotely.
     *
     * Same optimistic pattern as [saveMediaItem].
     */
    fun deleteMediaItem(itemId: String) {
        // Snapshot before optimistic update for precise revert
        val catalogBeforeDelete = _mediaCatalog.value.toList()

        // Optimistic local update — remove the item immediately
        val current = _mediaCatalog.value.toMutableList()
        current.removeAll { it.id == itemId }
        _mediaCatalog.value = current

        _adminOperationState.value = AdminOperationState.Loading
        firestore.collection(COLLECTION_NAME)
            .document(itemId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Deleted media item: $itemId")
                _adminOperationState.value = AdminOperationState.Success()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete media item: $itemId", e)
                _adminOperationState.value = AdminOperationState.Error(e.message ?: "Unknown error")
                // Restore the entire catalog snapshot from before the delete
                _mediaCatalog.value = catalogBeforeDelete
            }
    }

    private fun loadInitialCatalog() {
        // Catalog starts empty. Firestore listener will populate it
        // when the first snapshot arrives. If Firestore is empty
        // or unreachable, the catalog stays empty and UI shows empty states.
        _mediaCatalog.value = emptyList()
    }
}
