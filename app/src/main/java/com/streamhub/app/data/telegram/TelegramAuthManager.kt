package com.streamhub.app.data.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.streamhub.app.data.api.Secrets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    val photoUrl: String = ""
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Unauthenticated)
    val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private lateinit var prefs: SharedPreferences

    private val isTdLibAvailable: Boolean
        get() = Secrets.TELEGRAM_API_ID.isNotBlank() && Secrets.TELEGRAM_API_HASH.isNotBlank()

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
                photoUrl = prefs.getString(KEY_PHOTO_URL, "") ?: ""
            )
            val isOwner = prefs.getBoolean(KEY_IS_OWNER, false)
            _authState.value = TelegramAuthState.Authenticated(user, isOwner)
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
                val user = TelegramUser(
                    id = tdUser.id.toLong(),
                    firstName = tdUser.firstName,
                    lastName = tdUser.lastName,
                    username = tdUser.usernames?.activeUsernames?.firstOrNull() ?: tdUser.usernames?.editableUsername ?: "",
                    phoneNumber = tdUser.phoneNumber,
                    photoUrl = getProfilePhotoUrl(tdUser)
                )
                completeLogin(user)
            } else {
                // Fetch user from TDLib
                val result = TdLibManager.send(org.drinkless.tdlib.TdApi.GetMe())
                if (result is TdApi.User) {
                    val user = TelegramUser(
                        id = result.id.toLong(),
                        firstName = result.firstName,
                        lastName = result.lastName,
                        username = result.usernames?.activeUsernames?.firstOrNull() ?: result.usernames?.editableUsername ?: "",
                        phoneNumber = result.phoneNumber,
                        photoUrl = getProfilePhotoUrl(result)
                    )
                    completeLogin(user)
                } else {
                    Log.e(TAG, "Failed to get current user after TDLib auth")
                }
            }
        }
    }

    /**
     * Downloads user profile photo from TDLib and updates the authenticated state once downloaded.
     */
    private fun getProfilePhotoUrl(user: TdApi.User): String {
        val photo = user.profilePhoto
        if (photo != null) {
            val small = photo.small
            if (small != null) {
                if (small.local.isDownloadingCompleted && small.local.path.isNotBlank()) {
                    return small.local.path
                }
                // Trigger download and update state when completed
                scope.launch {
                    try {
                        val downloaded = TdLibManager.send(org.drinkless.tdlib.TdApi.DownloadFile(small.id, 1, 0, 0, true))
                        if (downloaded is TdApi.File && downloaded.local.isDownloadingCompleted && downloaded.local.path.isNotBlank()) {
                            val currentAuth = _authState.value
                            if (currentAuth is TelegramAuthState.Authenticated) {
                                val updatedUser = currentAuth.user.copy(photoUrl = downloaded.local.path)
                                _authState.value = currentAuth.copy(user = updatedUser)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to download profile photo for user ${user.id}", e)
                    }
                }
                if (small.local.path.isNotBlank()) {
                    return small.local.path
                }
            }
        }
        return ""
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
            val result = TdLibManager.setPhoneNumber(cleanPhone)
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _authState.value = TelegramAuthState.Error("Phone auth failed: $error")
            }
            // Success → TDLib will emit WaitCode state, which we observe
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

    private fun completeLogin(user: TelegramUser) {
        val isOwner = checkIsOwner(user)

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
            .apply()

        _authState.value = TelegramAuthState.Authenticated(user, isOwner)

        // Auto-join configured private channels
        autoJoinPrivateChannels()
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

        val ownerUsernames = com.streamhub.app.BuildConfig.OWNER_USERNAMES
            .split(",")
            .map { it.trim().lowercase().removePrefix("@") }
            .filter { it.isNotBlank() }

        val cleanUsername = user.username.lowercase().removePrefix("@")
        if (cleanUsername.isNotBlank() && cleanUsername in ownerUsernames) {
            com.streamhub.app.data.AdminManager.enableAdminModeFromOwner()
            return true
        }

        // Trigger async TDLib channel membership check
        verifyChannelAdminStatus(user.id)

        return false
    }

    private fun verifyChannelAdminStatus(userId: Long) {
        scope.launch {
            try {
                val isChannelAdmin = TdLibMediaProvider.checkIfUserIsChannelAdmin(userId)
                if (isChannelAdmin) {
                    com.streamhub.app.data.AdminManager.enableAdminModeFromOwner()
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
    private fun autoJoinPrivateChannels() {
        scope.launch {
            try {
                TdLibMediaProvider.autoJoinConfiguredChannels()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-join channels", e)
            }
        }
    }

    /**
     * Reset error state back to Unauthenticated so the user can retry.
     */
    fun resetState() {
        val currentState = _authState.value
        if (currentState is TelegramAuthState.Error) {
            _authState.value = TelegramAuthState.Unauthenticated
        }
    }

    /**
     * Check if TDLib is available (API credentials configured).
     */
    fun isTelegramAvailable(): Boolean = isTdLibAvailable
}
