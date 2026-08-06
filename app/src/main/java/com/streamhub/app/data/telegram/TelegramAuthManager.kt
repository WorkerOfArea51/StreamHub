package com.streamhub.app.data.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.streamhub.app.BuildConfig
import com.streamhub.app.data.api.Secrets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

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

    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Unauthenticated)
    val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private lateinit var prefs: SharedPreferences

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
     *
     * FIX #2: Gated mock login behind DEBUG_LOGGING and require 5+ digit code.
     * FIX #24: Generates robust mock UUID long ID instead of predictable timestamp/0L.
     */
    fun submitVerificationCode(code: String) {
        val currentState = _authState.value
        if (currentState !is TelegramAuthState.WaitingCode) return

        val cleanCode = code.trim()
        if (cleanCode.length < 5) {
            _authState.value = TelegramAuthState.Error("Invalid verification code. Must be 5+ digits.")
            return
        }

        // TODO: Replace this placeholder with real TDLib/Telegram API verification.
        // Currently this is a MOCK that auto-authenticates in debug builds only.
        if (!BuildConfig.DEBUG_LOGGING) {
            _authState.value = TelegramAuthState.Error(
                "Telegram login is not yet available in this build. Please wait for a future update."
            )
            return
        }

        Log.w(TAG, "WARNING: Using mock Telegram authentication. Not secure for production!")
        val user = TelegramUser(
            id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
            firstName = "Debug User",
            lastName = "",
            username = "debug_user",
            phoneNumber = currentState.phoneNumber,
            photoUrl = ""
        )
        completeLogin(user)
    }

    /**
     * FIX #17: Generate QR Code Auth STUB notification.
     */
    fun generateQRCodeAuth() {
        // TODO: Implement real Telegram QR login via TDLib.
        _authState.value = TelegramAuthState.Error(
            "QR code login is not yet available. Please use phone number login."
        )
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
        autoJoinPrivateChannels()
    }

    fun logout() {
        prefs.edit().clear().apply()
        _authState.value = TelegramAuthState.Unauthenticated
    }

    /**
     * FIX #5: Hardened owner username check.
     */
    private fun checkIsOwner(user: TelegramUser): Boolean {
        val apiId = Secrets.TELEGRAM_API_ID
        val apiHash = Secrets.TELEGRAM_API_HASH
        if (apiId.isBlank() || apiHash.isBlank()) return false

        return user.username.equals("WorkerOfArea51", ignoreCase = true) ||
               user.username.equals("StreamHubOwner", ignoreCase = true)
    }

    /**
     * FIX #18: Log pending TDLib integration notice for auto-join.
     */
    private fun autoJoinPrivateChannels() {
        val animeCh = Secrets.TELEGRAM_ANIME_CHANNEL
        val moviesCh = Secrets.TELEGRAM_MOVIES_CHANNEL
        val seriesCh = Secrets.TELEGRAM_SERIES_CHANNEL

        if (animeCh.isNotBlank() || moviesCh.isNotBlank() || seriesCh.isNotBlank()) {
            Log.i(TAG, "Auto-join channels pending TDLib integration: Anime=$animeCh, Movies=$moviesCh, Series=$seriesCh")
        }
    }

    fun resetState() {
        val currentState = _authState.value
        if (currentState is TelegramAuthState.Error) {
            _authState.value = TelegramAuthState.Unauthenticated
        }
    }
}
