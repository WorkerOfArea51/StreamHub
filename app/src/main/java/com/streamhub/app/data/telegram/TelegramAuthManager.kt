package com.streamhub.app.data.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.streamhub.app.data.api.Secrets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class representing a logged in Telegram User.
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
 * Telegram Authentication State.
 */
sealed class TelegramAuthState {
    data object Unauthenticated : TelegramAuthState()
    data object WaitingPhoneNumber : TelegramAuthState()
    data class WaitingCode(val phoneNumber: String) : TelegramAuthState()
    data class WaitingPassword(val phoneNumber: String) : TelegramAuthState()
    data class WaitingQRCode(val qrLink: String) : TelegramAuthState()
    data class Authenticated(val user: TelegramUser, val isOwner: Boolean) : TelegramAuthState()
    data class Error(val message: String) : TelegramAuthState()
}

/**
 * Telegram Authentication & Session Manager.
 *
 * ⚠️ STUB IMPLEMENTATION — NOT PRODUCTION READY
 *
 * This manager simulates the Telegram authentication flow without real TDLib
 * integration. All login methods create mock sessions with hardcoded user data.
 * Auto-join and owner detection are also simulated.
 *
 * To make this production-ready, you must:
 * 1. Integrate TDLib (Telegram Database Library) for real authentication.
 * 2. Replace `submitVerificationCode()` with TDLib's `verifyCode()` call.
 * 3. Replace `generateQRCodeAuth()` with TDLib's QR login request.
 * 4. Implement `autoJoinPrivateChannels()` using TDLib's `joinChannel()`.
 * 5. Replace `checkIsOwner()` with a server-side owner claim (e.g. Firestore
 *    document mapping Telegram user ID → owner status) instead of client-side
 *    username matching which is trivially spoofable.
 *
 * Until TDLib is integrated, users will see a "logged in" state that does not
 * connect to real Telegram servers and cannot access private channels.
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

    /**
     * Known owner Telegram user IDs. These are the only IDs that grant
     * owner privileges. Username-based matching is removed because any
     * user can set their display username to anything.
     */
    private val OWNER_USER_IDS: Set<Long> = setOf(
        777000L  // Replace with real owner Telegram user ID(s) from TDLib
    )

    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Unauthenticated)
    val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private lateinit var prefs: SharedPreferences

    /** Whether TDLib integration is available for real authentication. */
    private val isTdLibAvailable: Boolean
        get() = Secrets.TELEGRAM_API_ID.isNotBlank() && Secrets.TELEGRAM_API_HASH.isNotBlank()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
    }

    /**
     * Start Phone Number Authentication.
     *
     * ⚠️ STUB: In production, this should call TDLib's `sendPhoneNumber()`
     * which triggers Telegram to send an SMS/authorization code.
     */
    fun startPhoneAuth(phoneNumber: String) {
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.isBlank()) {
            _authState.value = TelegramAuthState.Error("Please enter a valid phone number.")
            return
        }

        if (!isTdLibAvailable) {
            // STUB: Simulate waiting for code without real TDLib call
            Log.w(TAG, "STUB: startPhoneAuth called without TDLib — simulating auth flow")
        }

        _authState.value = TelegramAuthState.WaitingCode(cleanPhone)
    }

    /**
     * Submit OTP verification code received via Telegram SMS/App.
     *
     * ⚠️ STUB: This does NOT verify with Telegram servers. It creates a
     * mock session with a fake user. In production, replace with:
     *   tdClient.send(TdApi.CheckAuthenticationCode(code)) { result -> ... }
     *
     * @param code The verification code entered by the user
     */
    fun submitVerificationCode(code: String) {
        val currentState = _authState.value
        if (currentState !is TelegramAuthState.WaitingCode) return

        val cleanCode = code.trim()
        if (cleanCode.length < 4) {
            _authState.value = TelegramAuthState.Error("Invalid verification code.")
            return
        }

        if (!isTdLibAvailable) {
            // STUB: Create a mock user instead of real TDLib authentication
            Log.w(TAG, "STUB: submitVerificationCode creating mock session (no TDLib)")
            val stubUser = TelegramUser(
                id = 0L,
                firstName = "Demo User",
                lastName = "",
                username = "demo_user",
                phoneNumber = currentState.phoneNumber,
                photoUrl = ""
            )
            completeLogin(stubUser)
            return
        }

        // TODO: Real TDLib integration:
        //   TdClient.send(TdApi.CheckAuthenticationCode(code)) { result ->
        //       if (result is TdApi.User) {
        //           val user = TelegramUser(id = result.id, firstName = result.firstName, ...)
        //           completeLogin(user)
        //       } else {
        //           _authState.value = TelegramAuthState.Error("Verification failed")
        //       }
        //   }
        Log.e(TAG, "TDLib integration not yet implemented")
        _authState.value = TelegramAuthState.Error("Real Telegram login is not yet implemented.")
    }

    /**
     * Start QR Code Login flow.
     *
     * ⚠️ STUB: Generates a fake QR URL. In production, replace with:
     *   tdClient.send(TdApi.RequestQrCodeAuthentication()) { result -> ... }
     */
    fun generateQRCodeAuth() {
        if (!isTdLibAvailable) {
            // STUB: Generate a placeholder QR code that is not a real Telegram link
            Log.w(TAG, "STUB: generateQRCodeAuth producing non-functional QR code (no TDLib)")
            val qrUrl = "https://streamhub.app/auth/pending?session=stub_${System.currentTimeMillis()}"
            _authState.value = TelegramAuthState.WaitingQRCode(qrUrl)
            return
        }

        // TODO: Real TDLib integration:
        //   TdClient.send(TdApi.RequestQrCodeAuthentication()) { result ->
        //       if (result is TdApi.UpdateQrCode) {
        //           _authState.value = TelegramAuthState.WaitingQRCode(result.url)
        //       }
        //   }
        Log.e(TAG, "TDLib QR auth not yet implemented")
        _authState.value = TelegramAuthState.Error("QR login is not yet implemented.")
    }

    private fun completeLogin(user: TelegramUser) {
        val isOwner = checkIsOwner(user)

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

        // Trigger Auto-Joining specified private channels
        autoJoinPrivateChannels()
    }

    /**
     * Log out of Telegram session.
     */
    fun logout() {
        prefs.edit().clear().apply()
        _authState.value = TelegramAuthState.Unauthenticated
    }

    /**
     * Check if the logged in Telegram user is the Owner.
     *
     * Uses only server-authoritative user IDs for owner detection.
     * Username-based matching is intentionally excluded because any
     * Telegram user can set their display username to any value —
     * matching on username is trivially spoofable.
     *
     * When TDLib is integrated, the user ID comes from Telegram's
     * server response and is immutable, making it a trustworthy
     * owner claim.
     */
    private fun checkIsOwner(user: TelegramUser): Boolean {
        if (!isTdLibAvailable) return false
        return user.id in OWNER_USER_IDS
    }

    /**
     * Auto-join Anime, Movies, and Web Series private channels.
     *
     * ⚠️ STUB: Currently logs channel names without actually joining.
     * In production, replace with TDLib joinChannel calls:
     *   TdClient.send(TdApi.JoinChannel(channelId)) { ... }
     */
    private fun autoJoinPrivateChannels() {
        val animeCh = Secrets.TELEGRAM_ANIME_CHANNEL
        val moviesCh = Secrets.TELEGRAM_MOVIES_CHANNEL
        val seriesCh = Secrets.TELEGRAM_SERIES_CHANNEL

        if (!isTdLibAvailable) {
            Log.w(TAG, "STUB: autoJoinPrivateChannels is a no-op without TDLib")
            return
        }

        // TODO: Real TDLib integration:
        //   listOf(animeCh, moviesCh, seriesCh).filter { it.isNotBlank() }.forEach { channel ->
        //       TdClient.send(TdApi.SearchPublicChat(channel)) { chat ->
        //           TdClient.send(TdApi.JoinChat(chat.id)) { result -> ... }
        //       }
        //   }
        Log.i(TAG, "TDLib auto-join not yet implemented for: Anime=$animeCh, Movies=$moviesCh, Series=$seriesCh")
    }

    fun resetState() {
        val currentState = _authState.value
        if (currentState is TelegramAuthState.Error) {
            _authState.value = TelegramAuthState.Unauthenticated
        }
    }
}
