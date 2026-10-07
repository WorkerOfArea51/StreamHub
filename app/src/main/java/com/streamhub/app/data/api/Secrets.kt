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

    /** Master Admin password for Creator Studio unlock. */
    val ADMIN_MASTER_PASSWORD: String get() = BuildConfig.ADMIN_MASTER_PASSWORD

    /** Private Community Access Code for App Gate. */
    val APP_ACCESS_CODE: String get() = BuildConfig.APP_ACCESS_CODE

    /** AniList v2 public GraphQL API base endpoint (zero authentication required). */
    const val ANILIST_GRAPHQL_URL: String = "https://graphql.anilist.co"

    /**
     * True only when built as debug. Used to gate verbose HTTP logging in TmdbClient.
     * Never use this to gate security features — only for log verbosity.
     */
    val DEBUG_LOGGING: Boolean get() = BuildConfig.DEBUG_LOGGING
}
