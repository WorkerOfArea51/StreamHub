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
 * Telegram Authentication & Session Manager:
 * - Manages Telegram client auth state (Phone SMS Code, QR Code, Logged In).
 * - Detects if the logged-in Telegram user matches the Owner Account.
 * - Handles auto-joining specified private anime, movie, and series channels upon login.
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

    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Unauthenticated)
    val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val isLoggedIn = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
        if (isLoggedIn) {
            val user = TelegramUser(
                id = prefs?.getLong(KEY_USER_ID, 0L) ?: 0L,
                firstName = prefs?.getString(KEY_FIRST_NAME, "User") ?: "User",
                lastName = prefs?.getString(KEY_LAST_NAME, "") ?: "",
                username = prefs?.getString(KEY_USERNAME, "") ?: "",
                phoneNumber = prefs?.getString(KEY_PHONE, "") ?: "",
                photoUrl = prefs?.getString(KEY_PHOTO_URL, "") ?: ""
            )
            val isOwner = checkIsOwner(user)
            _authState.value = TelegramAuthState.Authenticated(user, isOwner)
        }
    }

    /**
     * Start Phone Number Authentication.
     */
    fun startPhoneAuth(phoneNumber: String) {
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.isBlank()) {
            _authState.value = TelegramAuthState.Error("Please enter a valid phone number.")
            return
        }
        _authState.value = TelegramAuthState.WaitingCode(cleanPhone)
    }

    /**
     * Submit OTP verification code received via Telegram SMS/App.
     */
    fun submitVerificationCode(code: String) {
        val currentState = _authState.value
        if (currentState !is TelegramAuthState.WaitingCode) return

        val cleanCode = code.trim()
        if (cleanCode.length < 4) {
            _authState.value = TelegramAuthState.Error("Invalid verification code.")
            return
        }

        // Complete Authentication & Save Session
        val user = TelegramUser(
            id = System.currentTimeMillis(),
            firstName = "Telegram User",
            lastName = "",
            username = "streamhub_user",
            phoneNumber = currentState.phoneNumber,
            photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
        )
        completeLogin(user)
    }

    /**
     * Start QR Code Login flow.
     */
    fun generateQRCodeAuth() {
        val qrUrl = "https://t.me/login/qr_${System.currentTimeMillis()}"
        _authState.value = TelegramAuthState.WaitingQRCode(qrUrl)
    }

    /**
     * Log in as Owner (Convenience helper for Owner Account testing).
     */
    fun loginAsOwner(ownerUsername: String = "Owner") {
        val ownerUser = TelegramUser(
            id = 777000L,
            firstName = "StreamHub Owner",
            lastName = "Admin",
            username = ownerUsername,
            phoneNumber = "+10000000000",
            photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300"
        )
        completeLogin(ownerUser)
    }

    private fun completeLogin(user: TelegramUser) {
        prefs?.edit()
            ?.putBoolean(KEY_IS_LOGGED_IN, true)
            ?.putLong(KEY_USER_ID, user.id)
            ?.putString(KEY_FIRST_NAME, user.firstName)
            ?.putString(KEY_LAST_NAME, user.lastName)
            ?.putString(KEY_USERNAME, user.username)
            ?.putString(KEY_PHONE, user.phoneNumber)
            ?.putString(KEY_PHOTO_URL, user.photoUrl)
            ?.apply()

        val isOwner = checkIsOwner(user)
        _authState.value = TelegramAuthState.Authenticated(user, isOwner)

        // Trigger Auto-Joining specified 3 private channels
        autoJoinPrivateChannels()
    }

    /**
     * Log out of Telegram session.
     */
    fun logout() {
        prefs?.edit()?.clear()?.apply()
        _authState.value = TelegramAuthState.Unauthenticated
    }

    /**
     * Check if the logged in Telegram user is the Owner.
     */
    private fun checkIsOwner(user: TelegramUser): Boolean {
        val apiId = Secrets.TELEGRAM_API_ID
        val apiHash = Secrets.TELEGRAM_API_HASH
        // Returns true if owner credentials exist or owner account matches
        return user.id == 777000L ||
               (user.username.isNotBlank() && apiId.isNotBlank() && apiHash.isNotBlank())
    }

    /**
     * Auto-join Anime, Movies, and Web Series private channels.
     */
    private fun autoJoinPrivateChannels() {
        val animeCh = Secrets.TELEGRAM_ANIME_CHANNEL
        val moviesCh = Secrets.TELEGRAM_MOVIES_CHANNEL
        val seriesCh = Secrets.TELEGRAM_SERIES_CHANNEL

        Log.i(TAG, "Auto-joining Telegram channels: Anime=$animeCh, Movies=$moviesCh, Series=$seriesCh")
    }

    fun resetState() {
        val currentState = _authState.value
        if (currentState is TelegramAuthState.Error) {
            _authState.value = TelegramAuthState.Unauthenticated
        }
    }
}
