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

    /** MyAnimeList v2 REST API base URL. */
    const val MAL_BASE_URL: String = "https://api.myanimelist.net/v2/"

    /**
     * True only when built as debug. Used to gate verbose HTTP logging in TmdbClient.
     * Never use this to gate security features — only for log verbosity.
     */
    val DEBUG_LOGGING: Boolean get() = BuildConfig.DEBUG_LOGGING
}
