package com.streamhub.app.data.api

import com.streamhub.app.BuildConfig

/**
 * Read-only accessor for build-time injected secrets.
 *
 * All values come from BuildConfig, which is generated from local.properties
 * (gitignored) or environment variables (GitHub Secrets) at build time.
 * Never hardcode secrets in source code.
 */
object Secrets {

    /** TMDB v3 API key. Used by TmdbClient for poster/backdrop/synopsis autofetch. */
    val TMDB_API_KEY: String get() = BuildConfig.TMDB_API_KEY

    /** MyAnimeList v2 client ID. Sent as X-MAL-CLIENT-ID header. */
    val MAL_CLIENT_ID: String get() = BuildConfig.MAL_CLIENT_ID

    /** MyAnimeList v2 client secret. Empty for public clients (PKCE flow). */
    val MAL_CLIENT_SECRET: String get() = BuildConfig.MAL_CLIENT_SECRET

    /** Master Admin password for Creator Studio unlock. */
    val ADMIN_MASTER_PASSWORD: String get() = BuildConfig.ADMIN_MASTER_PASSWORD

    /** Private Community Access Code for App Gate. */
    val APP_ACCESS_CODE: String get() = BuildConfig.APP_ACCESS_CODE

    /** Unity Monetization Game ID for Android. */
    val UNITY_GAME_ID: String get() = BuildConfig.UNITY_GAME_ID

    /** Unity Rewarded Video Ad Unit / Placement ID. */
    val UNITY_REWARDED_AD_UNIT_ID: String get() = BuildConfig.UNITY_REWARDED_AD_UNIT_ID

    /** Unity Interstitial Ad Unit / Placement ID. */
    val UNITY_INTERSTITIAL_AD_UNIT_ID: String get() = BuildConfig.UNITY_INTERSTITIAL_AD_UNIT_ID

    /** Unity Banner Ad Unit / Placement ID. */
    val UNITY_BANNER_AD_UNIT_ID: String get() = BuildConfig.UNITY_BANNER_AD_UNIT_ID

    /** MyAnimeList v2 REST API base URL. */
    const val MAL_BASE_URL: String = "https://api.myanimelist.net/v2/"

    /**
     * True only when built as debug. Used to gate verbose HTTP logging in TmdbClient.
     * Never use this to gate security features — only for log verbosity.
     */
    val DEBUG_LOGGING: Boolean get() = BuildConfig.DEBUG_LOGGING
}
