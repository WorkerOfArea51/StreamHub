package com.streamhub.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State of the media catalog fetch.
 */
sealed class CatalogState {
    data object Loading : CatalogState()
    data object Ready : CatalogState()
    data class Error(val message: String) : CatalogState()
}

/**
 * State of an admin write operation (save or delete).
 */
sealed class AdminOperationState {
    data object Idle : AdminOperationState()
    data object Loading : AdminOperationState()
    data class Success(val timestamp: Long = System.currentTimeMillis()) : AdminOperationState()
    data class Error(val message: String) : AdminOperationState()
}

class FirebaseRepository private constructor() {

    companion object {
        private const val TAG = "FirebaseRepository"
        private const val COLLECTION_NAME = "media_content"

        @Volatile
        private var INSTANCE: FirebaseRepository? = null

        fun getInstance(): FirebaseRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseRepository().also { INSTANCE = it }
            }
    }

    private val firestore by lazy {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            attachFirestoreListener()
        }
    }

    fun retry() {
        Log.d(TAG, "Manual retry requested")
        _catalogState.value = CatalogState.Loading
        removeFirestoreListener()
        attachFirestoreListener()
    }

    fun resetAdminOperationState() {
        _adminOperationState.value = AdminOperationState.Idle
    }

    fun cleanup() {
        removeFirestoreListener()
    }

    private fun attachFirestoreListener() {
        val db = firestore
        if (db == null) {
            Log.e(TAG, "Firestore instance not available")
            _catalogState.value = CatalogState.Ready
            return
        }
        try {
            listenerRegistration = db.collection(COLLECTION_NAME)
                .orderBy("title", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Firestore listener error", error)
                        _catalogState.value = CatalogState.Ready
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        _catalogState.value = CatalogState.Ready
                        return@addSnapshotListener
                    }

                    scope.launch(Dispatchers.IO) {
                        val remoteItems = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(MediaItem::class.java)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse document ${doc.id}", e)
                                null
                            }
                        }

                        if (remoteItems.isNotEmpty()) {
                            val merged = (_mediaCatalog.value + remoteItems).distinctBy { it.id }
                            _mediaCatalog.value = merged
                        }
                        _catalogState.value = CatalogState.Ready
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach Firestore listener", e)
            _catalogState.value = CatalogState.Ready
        }
    }

    private fun removeFirestoreListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    /**
     * Save/Publish a media item into catalog from App UI.
     */
    fun saveMediaItem(item: MediaItem) {
        val current = _mediaCatalog.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        _mediaCatalog.value = current
        _catalogState.value = CatalogState.Ready
        _adminOperationState.value = AdminOperationState.Success()

        val db = firestore ?: return
        db.collection(COLLECTION_NAME)
            .document(item.id)
            .set(item)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully synced media item to Firestore: ${item.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync media item to Firestore: ${item.id}", e)
            }
    }

    /**
     * Delete a media item from catalog.
     */
    fun deleteMediaItem(itemId: String) {
        val current = _mediaCatalog.value.toMutableList()
        current.removeAll { it.id == itemId }
        _mediaCatalog.value = current
        _adminOperationState.value = AdminOperationState.Success()

        val db = firestore ?: return
        db.collection(COLLECTION_NAME)
            .document(itemId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Successfully deleted media item from Firestore: $itemId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete media item from Firestore: $itemId", e)
            }
    }

    private fun loadInitialCatalog() {
        _mediaCatalog.value = emptyList()
        _catalogState.value = CatalogState.Ready
    }
}
