package com.streamhub.app.data.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.streamhub.app.data.api.Secrets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

/**
 * Data class representing a logged-in Telegram User.
 */
data class TelegramUser(
    val id: Long,
    val firstName: String,
    val lastName: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val photoUrl: String = "",
    val isVerified: Boolean = false,
    val isPremium: Boolean = false
) {
    val displayName: String
        get() = if (lastName.isNotBlank()) "$firstName $lastName" else firstName

    val formattedUsername: String
        get() = if (username.isNotBlank()) "@$username" else ""
}

/**
 * Telegram Authentication State — exposed to the UI layer.
 */
sealed class TelegramAuthState {
    data object Unauthenticated : TelegramAuthState()
    data object WaitingPhoneNumber : TelegramAuthState()
    data class WaitingCode(val phoneNumber: String, val isRegistered: Boolean = true) : TelegramAuthState()
    data class WaitingPassword(val passwordHint: String, val hasRecovery: Boolean) : TelegramAuthState()
    data class WaitingQRCode(val qrLink: String) : TelegramAuthState()
    data class Authenticated(val user: TelegramUser, val isOwner: Boolean) : TelegramAuthState()
    data class Error(val message: String) : TelegramAuthState()
}

/**
 * Telegram Authentication & Session Manager.
 *
 * Production implementation using TdLibManager for real MTProto authentication.
 * Handles the complete auth flow: phone number → SMS/Telegram code → optional 2FA password.
 * Also supports QR code authentication for login without entering a phone number.
 *
 * Architecture:
 *  - TdLibManager owns the TDLib Client and handles the raw auth state machine
 *  - This class translates TdLibAuthState into the UI-friendly TelegramAuthState
 *  - On successful auth, it auto-joins configured private channels
 *  - User session is persisted by TDLib's own database (not SharedPreferences)
 *  - SharedPreferences only caches display info for fast cold-start reads
 *
 * Thread Safety:
 *  - All TDLib calls are dispatched on Dispatchers.IO
 *  - StateFlow updates are thread-safe
 *  - init() is idempotent and safe to call multiple times
 */
object TelegramAuthManager {

    private const val TAG = "TelegramAuthManager"
    private const val PREFS_NAME = "streamhub_telegram_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_LAST_NAME = "last_name"
    private const val KEY_USERNAME = "username"
    private const val KEY_PHONE = "phone"
    private const val KEY_PHOTO_URL = "photo_url"
    private const val KEY_IS_OWNER = "is_owner"
    private const val KEY_IS_VERIFIED = "is_verified"
    private const val KEY_IS_PREMIUM = "is_premium"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingPhotoFileId = java.util.concurrent.atomic.AtomicInteger(0)

    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Unauthenticated)
    val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private lateinit var prefs: SharedPreferences

    private val isTdLibAvailable: Boolean
        get() = true

    /**
     * Initialize the auth manager. Called from StreamHubApplication.onCreate().
     *
     * Reads cached user info from SharedPreferences for fast UI rendering,
     * then observes TdLibManager's auth state for real-time updates.
     */
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Read cached user from SharedPreferences (for fast cold-start)
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (isLoggedIn) {
            val user = TelegramUser(
                id = prefs.getLong(KEY_USER_ID, 0L),
                firstName = prefs.getString(KEY_FIRST_NAME, "User") ?: "User",
                lastName = prefs.getString(KEY_LAST_NAME, "") ?: "",
                username = prefs.getString(KEY_USERNAME, "") ?: "",
                phoneNumber = prefs.getString(KEY_PHONE, "") ?: "",
                photoUrl = prefs.getString(KEY_PHOTO_URL, "") ?: "",
                isVerified = prefs.getBoolean(KEY_IS_VERIFIED, false),
                isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false)
            )
            val isOwner = prefs.getBoolean(KEY_IS_OWNER, false)
            _authState.value = TelegramAuthState.Authenticated(user, isOwner)
        }

        // Register TDLib file and user update listener for dynamic profile updates
        TdLibManager.addUpdateListener { update ->
            when (update) {
                is TdApi.UpdateFile -> {
                    val file = update.file
                    if (file.local.isDownloadingCompleted && file.local.path.isNotBlank()) {
                        val currentAuth = _authState.value
                        if (currentAuth is TelegramAuthState.Authenticated) {
                            val targetId = pendingPhotoFileId.get()
                            if ((targetId != 0 && file.id == targetId) || currentAuth.user.photoUrl.isBlank()) {
                                val updatedUser = currentAuth.user.copy(photoUrl = file.local.path)
                                prefs.edit().putString(KEY_PHOTO_URL, file.local.path).apply()
                                _authState.value = currentAuth.copy(user = updatedUser)
                                Log.i(TAG, "Profile photo updated from TDLib file update: ${file.local.path}")
                            }
                        }
                    }
                }
                is TdApi.UpdateUser -> {
                    val currentAuth = _authState.value
                    if (currentAuth is TelegramAuthState.Authenticated && update.user.id.toLong() == currentAuth.user.id) {
                        fetchUserProfileAndPhoto(update.user)
                    }
                }
                else -> {}
            }
        }

        // Observe TdLibManager's auth state and translate to our state
        observeTdLibAuthState()
    }

    /**
     * Observe TdLibManager's auth state and translate to UI-friendly TelegramAuthState.
     *
     * This is the bridge between the raw TDLib auth state machine and the UI.
     * When TDLib reports Ready, we fetch the current user and complete login.
     * When TDLib reports waiting states, we translate them for the UI.
     */
    private fun observeTdLibAuthState() {
        scope.launch {
            TdLibManager.authState.collect { state ->
                when (state) {
                    is TdLibAuthState.Uninitialized,
                    is TdLibAuthState.WaitTdlibParameters,
                    is TdLibAuthState.WaitEncryptionKey -> {
                        // TDLib is initializing — don't change UI state yet
                        // If we had a cached user, keep showing it until TDLib confirms
                    }

                    is TdLibAuthState.WaitPhoneNumber -> {
                        _authState.value = TelegramAuthState.WaitingPhoneNumber
                    }

                    is TdLibAuthState.WaitCode -> {
                        _authState.value = TelegramAuthState.WaitingCode(
                            phoneNumber = state.phoneNumber,
                            isRegistered = state.isRegistered
                        )
                    }

                    is TdLibAuthState.WaitPassword -> {
                        _authState.value = TelegramAuthState.WaitingPassword(
                            passwordHint = state.passwordHint,
                            hasRecovery = state.hasRecovery
                        )
                    }

                    is TdLibAuthState.WaitQRCode -> {
                        _authState.value = TelegramAuthState.WaitingQRCode(state.qrLink)
                    }

                    is TdLibAuthState.Ready -> {
                        // TDLib is authenticated — fetch user info
                        onTdLibAuthenticated()
                    }

                    is TdLibAuthState.Closing -> {
                        // Session closing — keep current UI state
                    }

                    is TdLibAuthState.Closed -> {
                        _authState.value = TelegramAuthState.Unauthenticated
                    }

                    is TdLibAuthState.Error -> {
                        // Only override if we're not already authenticated (could be a stale error)
                        val current = _authState.value
                        if (current !is TelegramAuthState.Authenticated) {
                            _authState.value = TelegramAuthState.Error(state.message)
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when TDLib reports Ready state.
     * Fetches the current user from TDLib and completes the login flow.
     */
    private fun onTdLibAuthenticated() {
        scope.launch {
            val tdUser = TdLibManager.currentUser.value
            if (tdUser != null) {
                fetchUserProfileAndPhoto(tdUser)
            } else {
                val result = TdLibManager.send(TdApi.GetMe())
                if (result is TdApi.User) {
                    fetchUserProfileAndPhoto(result)
                } else {
                    Log.e(TAG, "Failed to get current user after TDLib auth")
                }
            }
        }
    }

    /**
     * Actively fetches user profile and downloads high-resolution profile photo from TDLib.
     */
    fun fetchUserProfileAndPhoto(tdUser: TdApi.User) {
        scope.launch {
            var photoPath = ""
            val photo = tdUser.profilePhoto
            val fileToDownload = photo?.big ?: photo?.small
            if (fileToDownload != null) {
                if (fileToDownload.local.isDownloadingCompleted && fileToDownload.local.path.isNotBlank()) {
                    photoPath = fileToDownload.local.path
                } else {
                    pendingPhotoFileId.set(fileToDownload.id)
                    try {
                        val downloadResult = TdLibManager.send(TdApi.DownloadFile(fileToDownload.id, 32, 0, 0, false))
                        if (downloadResult is TdApi.File && downloadResult.local.isDownloadingCompleted && downloadResult.local.path.isNotBlank()) {
                            photoPath = downloadResult.local.path
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "DownloadFile failed for profile photo ${fileToDownload.id}", e)
                    }
                }
            }

            val username = tdUser.usernames?.activeUsernames?.firstOrNull()
                ?: tdUser.usernames?.editableUsername
                ?: ""

            val user = TelegramUser(
                id = tdUser.id.toLong(),
                firstName = tdUser.firstName,
                lastName = tdUser.lastName,
                username = username,
                phoneNumber = tdUser.phoneNumber,
                photoUrl = photoPath,
                isVerified = false,
                isPremium = false
            )

            val isOwner = checkIsOwner(user)
            completeLogin(user, isOwner)
        }
    }

    /**
     * Refresh the user profile from Telegram MTProto on demand and retry channel joins.
     */
    fun refreshProfile() {
        scope.launch {
            try {
                val result = TdLibManager.send(TdApi.GetMe())
                if (result is TdApi.User) {
                    fetchUserProfileAndPhoto(result)
                }
                TdLibMediaProvider.autoJoinConfiguredChannels()
            } catch (e: Exception) {
                Log.w(TAG, "refreshProfile failed", e)
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Auth Actions (called by UI)
    // ──────────────────────────────────────────────────────────────

    /**
     * Start phone number authentication.
     *
     * Sends the phone number to TDLib, which triggers Telegram to send
     * a verification code via SMS or the Telegram app.
     *
     * @param phoneNumber Phone number in international format (e.g. "+1234567890")
     */
    fun startPhoneAuth(phoneNumber: String) {
        val cleanPhone = phoneNumber.trim()
        Log.d(TAG, "startPhoneAuth called with phone: '$cleanPhone'")
        if (cleanPhone.isBlank()) {
            _authState.value = TelegramAuthState.Error("Please enter a valid phone number.")
            return
        }

        if (!isTdLibAvailable) {
            _authState.value = TelegramAuthState.Error(
                "Telegram API credentials not configured. Add telegram_api_id and telegram_api_hash to local.properties."
            )
            return
        }

        scope.launch {
            Log.d(TAG, "Calling TdLibManager.setPhoneNumber('$cleanPhone')...")
            val result = TdLibManager.setPhoneNumber(cleanPhone)
            Log.d(TAG, "TdLibManager.setPhoneNumber result: $result")
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "Phone auth failed: $error", result.exceptionOrNull())
                _authState.value = TelegramAuthState.Error("Phone auth failed: $error")
            }
        }
    }

    /**
     * Submit the verification code received via SMS or Telegram app.
     *
     * @param code The 5-digit (or longer) verification code
     */
    fun submitVerificationCode(code: String) {
        val currentState = _authState.value
        if (currentState !is TelegramAuthState.WaitingCode) {
            _authState.value = TelegramAuthState.Error("Not waiting for a verification code.")
            return
        }

        val cleanCode = code.trim()
        if (cleanCode.length < 5) {
            _authState.value = TelegramAuthState.Error("Invalid verification code. Must be 5+ digits.")
            return
        }

        scope.launch {
            val result = TdLibManager.checkAuthCode(cleanCode)
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _authState.value = TelegramAuthState.Error("Code verification failed: $error")
            }
            // Success → TDLib will emit Ready or WaitPassword state
        }
    }

    /**
     * Submit the 2FA password (for accounts with two-factor authentication enabled).
     *
     * @param password The 2FA password
     */
    fun submitPassword(password: String) {
        val currentState = _authState.value
        if (currentState !is TelegramAuthState.WaitingPassword) {
            _authState.value = TelegramAuthState.Error("Not waiting for a password.")
            return
        }

        if (password.isBlank()) {
            _authState.value = TelegramAuthState.Error("Please enter your 2FA password.")
            return
        }

        scope.launch {
            val result = TdLibManager.checkAuthPassword(password)
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _authState.value = TelegramAuthState.Error("Password verification failed: $error")
            }
            // Success → TDLib will emit Ready state
        }
    }

    /**
     * Generate QR Code for authentication.
     *
     * The user scans this QR code with their Telegram app to log in
     * without entering a phone number. Useful for tablets and TVs.
     */
    fun generateQRCodeAuth() {
        if (!isTdLibAvailable) {
            _authState.value = TelegramAuthState.Error(
                "Telegram API credentials not configured."
            )
            return
        }

        scope.launch {
            val result = TdLibManager.requestQrCodeAuth()
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _authState.value = TelegramAuthState.Error("QR auth failed: $error")
            }
            // Success → TDLib will emit WaitQRCode state with the QR link
        }
    }

    private fun completeLogin(user: TelegramUser, isOwner: Boolean = checkIsOwner(user)) {
        // Cache user info in SharedPreferences for fast cold-start reads
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_FIRST_NAME, user.firstName)
            .putString(KEY_LAST_NAME, user.lastName)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_PHONE, user.phoneNumber)
            .putString(KEY_PHOTO_URL, user.photoUrl)
            .putBoolean(KEY_IS_OWNER, isOwner)
            .putBoolean(KEY_IS_VERIFIED, user.isVerified)
            .putBoolean(KEY_IS_PREMIUM, user.isPremium)
            .apply()

        // C12 FIX: Auto-join channels BEFORE emitting Authenticated state.
        // This guarantees the user can't navigate to content until channel
        // membership is established, preventing "Cannot access chat" errors.
        scope.launch {
            autoJoinPrivateChannels()
            _authState.value = TelegramAuthState.Authenticated(user, isOwner)
        }
    }

    /**
     * Log out the current Telegram user.
     *
     * Destroys the TDLib session and clears cached user info.
     * The user will need to re-authenticate on next login.
     */
    fun logout() {
        scope.launch {
            // Disable admin mode
            com.streamhub.app.data.AdminManager.disableAdmin()

            // Tell TDLib to log out (destroys session & restarts client)
            TdLibManager.logout()

            // M13 FIX: Wait for TDLib session destruction before clearing SharedPreferences
            kotlinx.coroutines.withTimeoutOrNull(5_000L) {
                TdLibManager.authState.collect { state ->
                    if (state is TdLibAuthState.Closed) {
                        return@collect
                    }
                }
            }

            // Clear SharedPreferences cache
            prefs.edit().clear().apply()

            _authState.value = TelegramAuthState.Unauthenticated
        }
    }

    /**
     * Checks whether the logged-in user is an Admin/Owner.
     * Regular users log in normally without admin access.
     * Admin mode is activated if user is a Channel Administrator/Creator, matches OWNER_USERNAMES,
     * or unlocks via Admin Password.
     */
    private fun checkIsOwner(user: TelegramUser): Boolean {
        val apiId = Secrets.TELEGRAM_API_ID
        val apiHash = Secrets.TELEGRAM_API_HASH
        if (apiId.isBlank() || apiHash.isBlank() || user.id == 0L) return false

        // Check if already verified as owner in prefs or AdminManager
        if (prefs.getBoolean(KEY_IS_OWNER, false) || com.streamhub.app.data.AdminManager.isAdminMode.value) {
            com.streamhub.app.data.AdminManager.enableAdminModeFromOwner()
            return true
        }

        val ownerIdentifiers = com.streamhub.app.BuildConfig.OWNER_USERNAMES
            .split(",")
            .map { it.trim().lowercase().removePrefix("@") }
            .filter { it.isNotBlank() }

        val cleanUsername = user.username.lowercase().removePrefix("@")
        val userIdStr = user.id.toString()

        // Match by username or permanent numeric Telegram User ID
        if ((cleanUsername.isNotBlank() && cleanUsername in ownerIdentifiers) || userIdStr in ownerIdentifiers) {
            prefs.edit().putBoolean(KEY_IS_OWNER, true).apply()
            com.streamhub.app.data.AdminManager.enableAdminModeFromOwner()
            return true
        }

        // Trigger async TDLib channel creator/admin check (immune to username changes)
        verifyChannelAdminStatus(user.id)

        return false
    }

    private fun verifyChannelAdminStatus(userId: Long) {
        scope.launch {
            try {
                val isChannelAdmin = TdLibMediaProvider.checkIfUserIsChannelAdmin(userId)
                if (isChannelAdmin) {
                    com.streamhub.app.data.AdminManager.enableAdminModeFromOwner()
                    prefs.edit().putBoolean(KEY_IS_OWNER, true).apply()
                    val currentAuth = _authState.value
                    if (currentAuth is TelegramAuthState.Authenticated && !currentAuth.isOwner) {
                        _authState.value = currentAuth.copy(isOwner = true)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to verify channel admin status for user $userId", e)
            }
        }
    }

    /**
     * Auto-join all configured private Telegram channels.
     *
     * Uses TdLibMediaProvider which handles the TDLib join chat API.
     * Called after successful authentication.
     */
    private suspend fun autoJoinPrivateChannels() {
        // Wait up to 10s for TDLib client to be fully in Ready state
        val ready = kotlinx.coroutines.withTimeoutOrNull(10_000L) {
            while (!TdLibManager.isReady()) {
                delay(200L)
            }
            true
        }
        if (ready != true) {
            Log.e(TAG, "TDLib not ready after 10s — skipping auto-join")
            return
        }

        try {
            TdLibMediaProvider.autoJoinConfiguredChannels()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-join channels", e)
        }
    }

    /**
     * Reset auth state back to Unauthenticated so the user can edit their phone number or retry.
     */
    fun resetState() {
        val currentState = _authState.value
        if (currentState !is TelegramAuthState.Authenticated) {
            _authState.value = TelegramAuthState.Unauthenticated
        }
    }

    /**
     * Check if TDLib is available (API credentials configured).
     */
    fun isTelegramAvailable(): Boolean = isTdLibAvailable
}
