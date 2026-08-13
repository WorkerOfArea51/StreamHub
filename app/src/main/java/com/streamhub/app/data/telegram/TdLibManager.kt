package com.streamhub.app.data.telegram

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.streamhub.app.BuildConfig
import com.streamhub.app.data.api.Secrets
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * TDLib Authorization State exposed to the app layer.
 *
 * Mirrors TdApi.AuthorizationState but is serializable and UI-friendly.
 */
sealed class TdLibAuthState {
    data object Uninitialized : TdLibAuthState()
    data object WaitTdlibParameters : TdLibAuthState()
    data object WaitEncryptionKey : TdLibAuthState()
    data object WaitPhoneNumber : TdLibAuthState()
    data class WaitCode(val phoneNumber: String, val isRegistered: Boolean = true) : TdLibAuthState()
    data class WaitPassword(val passwordHint: String, val hasRecovery: Boolean) : TdLibAuthState()
    data class WaitQRCode(val qrLink: String) : TdLibAuthState()
    data object Ready : TdLibAuthState()
    data object Closing : TdLibAuthState()
    data object Closed : TdLibAuthState()
    data class Error(val message: String) : TdLibAuthState()
}

/**
 * Core TDLib Client Wrapper — the single source of truth for all Telegram operations.
 *
 * Responsibilities:
 *  - Creates and manages the TDLib [Client] instance
 *  - Handles the full authorization state machine (phone → code → password → QR)
 *  - Provides [send] suspend function for request/response with timeout
 *  - Exposes [authState] StateFlow for UI observation
 *  - Manages TDLib session database (auto-persisted by TDLib)
 *  - Configures MTProto/SOCKS5 proxy on the TDLib client
 *
 * Thread Safety:
 *  - All TDLib calls are dispatched on the TDLib internal handler thread
 *  - [send] is safe to call from any coroutine context
 *  - StateFlow updates are posted to the main thread for Compose observation
 *
 * Lifecycle:
 *  - Call [initialize] once from Application.onCreate() on a background thread
 *  - Call [destroy] on app termination (rarely needed — TDLib persists sessions)
 *  - Do NOT recreate on configuration changes — the singleton survives
 */
object TdLibManager {

    private const val TAG = "TdLibManager"
    private const val REQUEST_TIMEOUT_MS = 30_000L

    /** Coroutine scope for internal async work. SupervisorJob so one failure doesn't cancel all. */
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Main-thread handler for posting StateFlow updates. */
    private val mainHandler = Handler(Looper.getMainLooper())

    /** The TDLib JNI client. Null before [initialize] and after [destroy]. */
    @Volatile
    private var client: Client? = null

    /** Monotonically increasing request ID for correlating responses. */
    private val requestIdCounter = AtomicLong(0)

    /** Pending requests waiting for their TDLib response. Key = requestId. */
    private val pendingRequests = ConcurrentHashMap<Long, CancellableContinuation<TdApi.Object>>()

    /** Mutex to serialize TDLib initialization (prevent double-init race). */
    private val initMutex = Mutex()

    /** Whether [initialize] has been called and completed successfully. */
    @Volatile
    private var isInitialized = false

    /** Path to TDLib session database directory. */
    @Volatile
    private var databaseDirectory: String = ""

    // ──────────────────────────────────────────────────────────────
    // Auth State
    // ──────────────────────────────────────────────────────────────

    private val _authState = MutableStateFlow<TdLibAuthState>(TdLibAuthState.Uninitialized)
    val authState: StateFlow<TdLibAuthState> = _authState.asStateFlow()

    /** The currently logged-in user, or null. */
    private val _currentUser = MutableStateFlow<TdApi.User?>(null)
    val currentUser: StateFlow<TdApi.User?> = _currentUser.asStateFlow()

    /** Phone number being authenticated (stored during the auth flow). */
    @Volatile
    private var pendingPhoneNumber: String = ""

    // ──────────────────────────────────────────────────────────────
    // Update Listeners
    // ──────────────────────────────────────────────────────────────

    /** External listeners for TDLib updates (e.g. new messages, file download progress). */
    private val updateListeners = java.util.concurrent.CopyOnWriteArrayList<(TdApi.Object) -> Unit>()

    // ──────────────────────────────────────────────────────────────
    // Initialization
    // ──────────────────────────────────────────────────────────────

    /**
     * Initialize the TDLib client. Safe to call multiple times — subsequent calls are no-ops.
     *
     * Must be called from a background thread (Dispatchers.IO) because TDLib
     * performs JNI work during Client.create().
     *
     * @param context Application context (used for TDLib database path)
     * @return true if initialization succeeded, false if credentials are missing
     */
    suspend fun initialize(context: Context): Boolean {
        initMutex.withLock {
            if (isInitialized) return true

            val rawApiId = Secrets.TELEGRAM_API_ID.trim()
            val rawApiHash = Secrets.TELEGRAM_API_HASH.trim()

            if (rawApiId.isBlank() || rawApiId == "0" || rawApiHash.isBlank()) {
                Log.e(TAG, "Telegram API credentials not configured in Secrets")
                _authState.value = TdLibAuthState.Error("Telegram API credentials not configured")
                return false
            }

            val apiIdInt = rawApiId.toIntOrNull()
                ?: run {
                    Log.e(TAG, "Invalid TELEGRAM_API_ID: not an integer")
                    _authState.value = TdLibAuthState.Error("Invalid TELEGRAM_API_ID")
                    return false
                }

            val apiHash = rawApiHash

            appContext = context.applicationContext
            // TDLib session database stored in app's internal storage
            databaseDirectory = "${context.filesDir.absolutePath}/tdlib"

            try {
                client = Client.create(
                    Client.ResultHandler { obj -> handleUpdate(obj) },
                    Client.ExceptionHandler { err -> Log.e(TAG, "TDLib exception", err) },
                    Client.ExceptionHandler { err -> Log.e(TAG, "TDLib default exception", err) }
                )
                isInitialized = true
                Log.i(TAG, "TDLib client created. Database: $databaseDirectory")
                return true
            } catch (t: Throwable) {
                Log.e(TAG, "TDLib native library failed to load or initialize", t)
                _authState.value = TdLibAuthState.Error("TDLib native library failed to load: ${t.message}")
                return false
            }
        }
    }

    /**
     * Returns true if the TDLib client is initialized and ready for requests.
     */
    fun isReady(): Boolean = isInitialized && client != null && _authState.value == TdLibAuthState.Ready

    /**
     * Returns true if the client is initialized (even if not yet authenticated).
     */
    fun isInitialized(): Boolean = isInitialized

    // ──────────────────────────────────────────────────────────────
    // Request / Response
    // ──────────────────────────────────────────────────────────────

    /**
     * Send a TDLib request and suspend until the response is received or timeout.
     *
     * This is the primary way to interact with TDLib. It wraps the callback-based
     * Client.send() into a coroutine-friendly suspend function.
     *
     * @param function TdApi request object (e.g. TdApi.SendMessage)
     * @param timeoutMs Maximum wait time in milliseconds (default 30s)
     * @return TdApi.Object response, or TdApi.Error on failure/timeout
     */
    suspend fun send(
        function: TdApi.Function<*>,
        timeoutMs: Long = REQUEST_TIMEOUT_MS
    ): TdApi.Object {
        val c = client
        if (c == null) {
            return TdApi.Error(400, "TDLib client not initialized")
        }

        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val id = requestIdCounter.incrementAndGet()
                pendingRequests[id] = cont

                cont.invokeOnCancellation {
                    pendingRequests.remove(id)
                }

                try {
                    c.send(function) { result ->
                        val pending = pendingRequests.remove(id)
                        pending?.resumeWith(
                            if (result is TdApi.Error) {
                                Result.failure(TdLibException(result.code, result.message))
                            } else {
                                Result.success(result)
                            }
                        )
                    }
                } catch (e: Exception) {
                    pendingRequests.remove(id)
                    cont.resumeWith(Result.failure(e))
                }
            }
        } ?: TdApi.Error(408, "Request timed out after ${timeoutMs}ms")
    }

    /**
     * Send a TDLib request that is expected to return [TdApi.Ok].
     * Throws [TdLibException] on error.
     */
    suspend fun sendOk(function: TdApi.Function<*>, timeoutMs: Long = REQUEST_TIMEOUT_MS) {
        val result = send(function, timeoutMs)
        if (result is TdApi.Error) {
            throw TdLibException(result.code, result.message)
        }
    }

    /**
     * Execute a TDLib request synchronously (blocking). Use only for fast queries.
     * Must NOT be called on the main thread.
     */
    fun execute(function: TdApi.Function<*>): TdApi.Object {
        if (!isInitialized) return TdApi.Error(400, "TDLib client not initialized")
        return try {
            Client.execute(function)
        } catch (e: Exception) {
            TdApi.Error(500, "Execute failed: ${e.message}")
        }
    }

    private fun handleUpdate(obj: TdApi.Object) {
        when (obj) {
            is TdApi.UpdateAuthorizationState -> handleAuthState(obj.authorizationState)
            is TdApi.UpdateUser -> {
                if (obj.user.id == (_currentUser.value?.id ?: 0)) {
                    mainHandler.post { _currentUser.value = obj.user }
                }
            }
            else -> {
                for (listener in updateListeners) {
                    try { listener(obj) } catch (e: Exception) {
                        Log.w(TAG, "Update listener threw", e)
                    }
                }
            }
        }
    }

    private fun handleAuthState(state: TdApi.AuthorizationState) {
        Log.i(TAG, "Auth state: ${state::class.simpleName}")

        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                mainHandler.post { _authState.value = TdLibAuthState.WaitTdlibParameters }
                autoSetTdlibParameters()
            }

            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                mainHandler.post { _authState.value = TdLibAuthState.WaitPhoneNumber }
            }

            is TdApi.AuthorizationStateWaitCode -> {
                val info = state.codeInfo
                mainHandler.post {
                    _authState.value = TdLibAuthState.WaitCode(
                        phoneNumber = pendingPhoneNumber,
                        isRegistered = true
                    )
                }
                Log.i(TAG, "Waiting for auth code. Type: ${info.type::class.simpleName}")
            }

            is TdApi.AuthorizationStateWaitPassword -> {
                mainHandler.post {
                    _authState.value = TdLibAuthState.WaitPassword(
                        passwordHint = state.passwordHint ?: "",
                        hasRecovery = state.hasRecoveryEmailAddress
                    )
                }
            }

            is TdApi.AuthorizationStateReady -> {
                mainHandler.post { _authState.value = TdLibAuthState.Ready }
                fetchCurrentUser()
                Log.i(TAG, "TDLib authenticated successfully!")
            }

            is TdApi.AuthorizationStateClosing -> {
                mainHandler.post { _authState.value = TdLibAuthState.Closing }
            }

            is TdApi.AuthorizationStateClosed -> {
                mainHandler.post { _authState.value = TdLibAuthState.Closed }
            }

            else -> {
                Log.d(TAG, "Other auth state: ${state::class.simpleName}")
            }
        }
    }

    @Volatile
    private var appContext: Context? = null

    private fun getOrCreateDatabaseEncryptionKey(): ByteArray {
        val ctx = appContext ?: return ByteArray(0)
        return try {
            val keyAlias = "streamhub_tdlib_db_key_alias"
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore")
                keyGenerator.init(
                    android.security.keystore.KeyGenParameterSpec.Builder(
                        keyAlias,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                )
                keyGenerator.generateKey()
            }

            val prefs = ctx.getSharedPreferences("streamhub_tdlib_sec", Context.MODE_PRIVATE)
            val encryptedKeyBase64 = prefs.getString("encrypted_db_key_gcm", null)

            if (encryptedKeyBase64 != null) {
                val secretKey = keyStore.getKey(keyAlias, null) as javax.crypto.SecretKey
                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                val encryptedBytes = android.util.Base64.decode(encryptedKeyBase64, android.util.Base64.DEFAULT)
                val iv = encryptedBytes.copyOfRange(0, 12)
                val ciphertext = encryptedBytes.copyOfRange(12, encryptedBytes.size)
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, javax.crypto.spec.GCMParameterSpec(128, iv))
                cipher.doFinal(ciphertext)
            } else {
                val randomKey = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
                val secretKey = keyStore.getKey(keyAlias, null) as javax.crypto.SecretKey
                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encrypted = cipher.doFinal(randomKey)
                val combined = iv + encrypted
                prefs.edit()
                    .putString("encrypted_db_key_gcm", android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT))
                    .apply()
                randomKey
            }
        } catch (e: Exception) {
            Log.e(TAG, "Android Keystore unavailable — cannot securely store TDLib database encryption key", e)
            ByteArray(0)
        }
    }

    private fun autoSetTdlibParameters() {
        val c = client ?: return
        val apiId = Secrets.TELEGRAM_API_ID.trim().toIntOrNull() ?: return
        val apiHash = Secrets.TELEGRAM_API_HASH.trim()

        try {
            c.send(TdApi.SetTdlibParameters(
                false, // useTestDc
                databaseDirectory, // databaseDirectory
                "$databaseDirectory/files", // filesDirectory
                getOrCreateDatabaseEncryptionKey(), // databaseEncryptionKey
                true, // useFileDatabase
                true, // useChatInfoDatabase
                true, // useMessageDatabase
                true, // useSecretChatDatabase
                apiId, // apiId
                apiHash, // apiHash
                java.util.Locale.getDefault().toLanguageTag(), // systemLanguageCode
                android.os.Build.MODEL, // deviceModel
                "${android.os.Build.VERSION.SDK_INT}", // systemVersion
                BuildConfig.VERSION_NAME // applicationVersion
            )) { result ->
                if (result is TdApi.Error) {
                    Log.e(TAG, "SetTdlibParameters failed: ${result.code} — ${result.message}")
                    mainHandler.post {
                        _authState.value = TdLibAuthState.Error("TDLib params failed: ${result.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SetTdlibParameters", e)
        }
    }

    private fun fetchCurrentUser() {
        scope.launch {
            val result = send(TdApi.GetMe())
            if (result is TdApi.User) {
                mainHandler.post { _currentUser.value = result }
                val uName = result.usernames?.activeUsernames?.firstOrNull() ?: ""
                Log.i(TAG, "Current user: ${result.firstName} ${result.lastName} (@$uName)")
            } else if (result is TdApi.Error) {
                Log.e(TAG, "GetMe failed: ${result.message}")
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Auth Actions (called by TelegramAuthManager / UI)
    // ──────────────────────────────────────────────────────────────

    /**
     * Completely resets and re-creates the TDLib client JNI instance.
     * Called automatically after logout or on session errors so users can log in cleanly.
     */
    suspend fun restartClient() {
        initMutex.withLock {
            try {
                client?.send(TdApi.Close()) {}
            } catch (_: Exception) {}
            client = null
            isInitialized = false
            _currentUser.value = null
            _authState.value = TdLibAuthState.Uninitialized

            val apiId = Secrets.TELEGRAM_API_ID.trim()
            val apiHash = Secrets.TELEGRAM_API_HASH.trim()
            if (apiId.isBlank() || apiHash.isBlank()) return

            val apiIdInt = apiId.toIntOrNull() ?: return
            val handler = Client.ResultHandler { update -> handleUpdate(update) }
            val exHandler = Client.ExceptionHandler { ex -> Log.e(TAG, "TDLib unhandled exception", ex) }

            try {
                client = Client.create(handler, exHandler, exHandler)
                isInitialized = true
                Log.i(TAG, "TDLib client successfully restarted.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to recreate TDLib client", e)
            }
        }
    }

    /**
     * Start phone number authentication.
     * TDLib will send an SMS/Telegram code to the phone number.
     * Includes automatic smart recovery if the client was closed or uninitialized.
     */
    suspend fun setPhoneNumber(phoneNumber: String): Result<Unit> {
        pendingPhoneNumber = phoneNumber

        // SMART RECOVERY: If client is null or auth state is closed/uninitialized, restart client first
        val currentState = _authState.value
        if (client == null || !isInitialized || currentState is TdLibAuthState.Closed || currentState is TdLibAuthState.Uninitialized) {
            Log.i(TAG, "TDLib client closed or uninitialized. Restarting TDLib client before phone auth...")
            restartClient()
            // Wait briefly for TDLib parameters initialization
            withTimeoutOrNull(5000L) {
                while (_authState.value !is TdLibAuthState.WaitPhoneNumber && _authState.value !is TdLibAuthState.Ready) {
                    kotlinx.coroutines.delay(100)
                }
                true
            }
        }

        return try {
            val settings = TdApi.PhoneNumberAuthenticationSettings(
                false, // allowFlashCall
                false, // allowMissedCall
                false, // isCurrentPhoneNumber
                false, // allowSmsRetrieverApi
                false, // isUnknown
                null,  // firebaseAuthenticationSettings
                null   // authenticationTokens
            )
            sendOk(TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings))
            Result.success(Unit)
        } catch (e: TdLibException) {
            // Smart Recovery: If request aborted or client closed, restart client ONCE and retry setPhoneNumber
            if (e.message.contains("aborted", ignoreCase = true) || e.code == 400) {
                Log.w(TAG, "Phone auth failed with '${e.message}', performing automatic smart client reset...")
                restartClient()
                kotlinx.coroutines.delay(800)
                try {
                    val settings = TdApi.PhoneNumberAuthenticationSettings(
                        false, false, false, false, false, null, null
                    )
                    sendOk(TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings))
                    return Result.success(Unit)
                } catch (retryEx: TdLibException) {
                    return Result.failure(retryEx)
                }
            }
            Result.failure(e)
        }
    }

    /**
     * Submit the authentication code received via SMS or Telegram app.
     */
    suspend fun checkAuthCode(code: String): Result<Unit> {
        return try {
            sendOk(TdApi.CheckAuthenticationCode(code))
            Result.success(Unit)
        } catch (e: TdLibException) {
            Result.failure(e)
        }
    }

    /**
     * Submit the 2FA password (for accounts with two-factor authentication).
     */
    suspend fun checkAuthPassword(password: String): Result<Unit> {
        return try {
            sendOk(TdApi.CheckAuthenticationPassword(password))
            Result.success(Unit)
        } catch (e: TdLibException) {
            Result.failure(e)
        }
    }

    /**
     * Request QR code authentication. TDLib will generate a QR link
     * that the user scans with their Telegram app.
     */
    suspend fun requestQrCodeAuth(): Result<Unit> {
        return Result.failure(UnsupportedOperationException("QR code login not supported in this version"))
    }

    /**
     * Register a new user (when the phone number is not yet registered on Telegram).
     * @param firstName User's first name
     * @param lastName User's last name
     */
    suspend fun registerUser(firstName: String, lastName: String = ""): Result<Unit> {
        return try {
            sendOk(TdApi.RegisterUser(firstName, lastName, false))
            Result.success(Unit)
        } catch (e: TdLibException) {
            Result.failure(e)
        }
    }

    /**
     * Log out the current user. TDLib will destroy the session.
     * Automatically restarts the client afterwards so a new user can log in immediately.
     */
    suspend fun logout() {
        try {
            sendOk(TdApi.LogOut())
        } catch (e: Exception) {
            Log.w(TAG, "Logout error", e)
        }
        pendingPhoneNumber = ""
        mainHandler.post {
            _currentUser.value = null
            _authState.value = TdLibAuthState.Uninitialized
        }
        // Auto-restart client so TDLib is immediately clean and ready for next login!
        scope.launch {
            kotlinx.coroutines.delay(500)
            restartClient()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Proxy Configuration
    // ──────────────────────────────────────────────────────────────

    /**
     * Configure a proxy on the TDLib client.
     *
     * MTProto proxies are TDLib's native proxy type and work directly.
     * SOCKS5 proxies are also supported.
     *
     * @param server Proxy server hostname or IP
     * @param port Proxy port
     * @param type ProxyType.MTPROTO or ProxyType.SOCKS5
     * @param secret MTProto secret (hex string) — only for MTPROTO
     * @param username SOCKS5 username — only for SOCKS5
     * @param password SOCKS5 password — only for SOCKS5
     */
    suspend fun setProxy(
        server: String,
        port: Int,
        type: ProxyType,
        secret: String = "",
        username: String = "",
        password: String = ""
    ): Result<Unit> {
        return try {
            val proxyType = when (type) {
                ProxyType.MTPROTO -> TdApi.ProxyTypeMtproto(secret)
                ProxyType.SOCKS5 -> TdApi.ProxyTypeSocks5(username, password)
                ProxyType.SOCKS4 -> {
                    Log.w(TAG, "SOCKS4 proxies are not supported by TDLib. Please use SOCKS5 or MTProto.")
                    return Result.failure(IllegalArgumentException("TDLib does not support SOCKS4 proxies"))
                }
                ProxyType.HTTP -> {
                    // TDLib doesn't support HTTP proxies natively.
                    // HTTP proxies are handled by OkHttpClient, not TDLib.
                    Log.w(TAG, "HTTP proxy not supported by TDLib. Configure OkHttpClient instead.")
                    return Result.failure(IllegalArgumentException("TDLib does not support HTTP proxies"))
                }
            }

            // TDLib 1.8.x uses AddProxy instead of the removed SetProxy
            val result = send(TdApi.AddProxy(server, port, true, proxyType))
            if (result is TdApi.Error) {
                Result.failure(TdLibException(result.code, result.message))
            } else {
                Log.i(TAG, "Proxy set: $server:$port ($type)")
                Result.success(Unit)
            }
        } catch (e: TdLibException) {
            Result.failure(e)
        }
    }

    /**
     * Remove the currently configured proxy (direct connection).
     */
    suspend fun removeProxy(): Result<Unit> {
        return try {
            // TDLib 1.8.x uses DisableProxy instead of SetProxy with enable=false
            sendOk(TdApi.DisableProxy())
            Result.success(Unit)
        } catch (e: TdLibException) {
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Update Listener Registration
    // ──────────────────────────────────────────────────────────────

    /**
     * Register a listener for TDLib updates.
     * Use this for file download progress, new messages, etc.
     */
    fun addUpdateListener(listener: (TdApi.Object) -> Unit) {
        updateListeners.add(listener)
    }

    fun removeUpdateListener(listener: (TdApi.Object) -> Unit) {
        updateListeners.remove(listener)
    }

    // ──────────────────────────────────────────────────────────────
    // Destroy
    // ──────────────────────────────────────────────────────────────

    /**
     * M11 FIX: Send a request and throw TdLibException on Error response.
     */
    suspend fun sendOrThrow(function: TdApi.Function<*>): TdApi.Object {
        val result = send(function)
        if (result is TdApi.Error) {
            throw TdLibException(result.code, result.message)
        }
        return result
    }

    /**
     * Gracefully close the TDLib client.
     * Called on app termination. TDLib persists its database, so
     * the session is restored on next [initialize].
     */
    suspend fun destroy() {
        val c = client ?: return
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        try {
            sendOk(TdApi.Close())
        } catch (e: Exception) {
            Log.w(TAG, "Error closing TDLib", e)
        }
        for ((_, cont) in pendingRequests) {
            try { cont.resumeWith(Result.failure(TdLibException(499, "TDLib client destroyed"))) } catch (_: Exception) {}
        }
        pendingRequests.clear()
        isInitialized = false
        client = null
    }
}

/**
 * Exception thrown when a TDLib request returns an error.
 */
class TdLibException(val code: Int, override val message: String) : Exception("TDLib error $code: $message")
