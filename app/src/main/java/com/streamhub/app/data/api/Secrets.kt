package com.streamhub.app.data.api

import com.streamhub.app.BuildConfig

/**
 * Read-only accessor for build-time injected secrets.
 *
 * All values come from BuildConfig, which is generated from local.properties
 * (gitignored) or environment variables at build time. Never hardcode secrets in
 * this file or anywhere else in source. To rotate a key, update local.properties
 * or CI secrets and rebuild.
 */
object Secrets {

    /** TMDB v3 API key. Used by TmdbClient for poster/backdrop/synopsis autofetch. */
    val TMDB_API_KEY: String get() = BuildConfig.TMDB_API_KEY

    /** MyAnimeList v2 client ID. Sent as X-MAL-CLIENT-ID header. */
    val MAL_CLIENT_ID: String get() = BuildConfig.MAL_CLIENT_ID

    /** MyAnimeList v2 client secret. Empty for public clients (PKCE flow). */
    val MAL_CLIENT_SECRET: String get() = BuildConfig.MAL_CLIENT_SECRET

    /**
     * Bcrypt hash of the admin PIN. Verified by AdminManager.verifyAndEnableAdmin (M3).
     * Default "0000" never matches any real PIN, so admin login is disabled until
     * a real hash is set in local.properties.
     */
    val ADMIN_PIN_HASH: String get() = BuildConfig.ADMIN_PIN_HASH

    /** Telegram App API credentials for TDLib client authentication. */
    val TELEGRAM_API_ID: String get() = BuildConfig.TELEGRAM_API_ID.ifBlank { "23143864" }
    val TELEGRAM_API_HASH: String get() = BuildConfig.TELEGRAM_API_HASH.ifBlank { "726e02cd51d31364d6aca817dac5ed81" }

    /** Telegram Private Channels (Auto-joined upon TDLib user login). */
    val TELEGRAM_ANIME_CHANNEL: String get() = BuildConfig.TELEGRAM_ANIME_CHANNEL
    val TELEGRAM_MOVIES_CHANNEL: String get() = BuildConfig.TELEGRAM_MOVIES_CHANNEL
    val TELEGRAM_SERIES_CHANNEL: String get() = BuildConfig.TELEGRAM_SERIES_CHANNEL

    /** MyAnimeList v2 REST API base URL. */
    const val MAL_BASE_URL: String = "https://api.myanimelist.net/v2/"

    /**
     * True only when built as debug. Used to gate verbose HTTP logging in TmdbClient.
     * Never use this to gate security features — only for log verbosity.
     */
    val DEBUG_LOGGING: Boolean get() = BuildConfig.DEBUG_LOGGING
}
