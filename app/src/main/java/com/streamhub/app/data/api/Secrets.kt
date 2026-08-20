package com.streamhub.app.data.api

import com.streamhub.app.BuildConfig

/**
 * Read-only accessor for build-time injected secrets and built-in fallbacks.
 *
 * All values come from BuildConfig, which is generated from local.properties
 * or environment variables at build time, with default fallback credentials
 * to ensure release builds from GitHub Actions or forks always function properly.
 */
object Secrets {

    private const val DEFAULT_TMDB_API_KEY = "ec562d9f2a8a07ffb7fa3308fb5bec9c"
    private const val DEFAULT_MAL_CLIENT_ID = "4f7167fe0e6ff0b5832d117657a1aefb"
    private const val DEFAULT_MAL_CLIENT_SECRET = "c721d0b2400eeb7893c2e958514be9279736d7f202b6734e4eef913e098b71df"
    private const val DEFAULT_TELEGRAM_API_ID = "23143864"
    private const val DEFAULT_TELEGRAM_API_HASH = "726e02cd51d31364d6aca817dac5ed81"
    private const val DEFAULT_TELEGRAM_ANIME_CHANNEL = "https://t.me/+AkdK7gDbYWRmZTc1"
    private const val DEFAULT_TELEGRAM_MOVIES_CHANNEL = "https://t.me/+dx2YihneVQczNTM1"
    private const val DEFAULT_TELEGRAM_SERIES_CHANNEL = "https://t.me/+FiQ7kG8Ofh5jMDU1"

    /** TMDB v3 API key. Used by TmdbClient for poster/backdrop/synopsis autofetch. */
    val TMDB_API_KEY: String get() = BuildConfig.TMDB_API_KEY.ifBlank { DEFAULT_TMDB_API_KEY }

    /** MyAnimeList v2 client ID. Sent as X-MAL-CLIENT-ID header. */
    val MAL_CLIENT_ID: String get() = BuildConfig.MAL_CLIENT_ID.ifBlank { DEFAULT_MAL_CLIENT_ID }

    /** MyAnimeList v2 client secret. Empty for public clients (PKCE flow). */
    val MAL_CLIENT_SECRET: String get() = BuildConfig.MAL_CLIENT_SECRET.ifBlank { DEFAULT_MAL_CLIENT_SECRET }

    /** Telegram App API credentials for TDLib client authentication. */
    val TELEGRAM_API_ID: String get() = BuildConfig.TELEGRAM_API_ID.ifBlank { DEFAULT_TELEGRAM_API_ID }
    val TELEGRAM_API_HASH: String get() = BuildConfig.TELEGRAM_API_HASH.ifBlank { DEFAULT_TELEGRAM_API_HASH }

    /** Telegram Private Channels (Auto-joined upon TDLib user login). */
    val TELEGRAM_ANIME_CHANNEL: String get() = BuildConfig.TELEGRAM_ANIME_CHANNEL.ifBlank { DEFAULT_TELEGRAM_ANIME_CHANNEL }
    val TELEGRAM_MOVIES_CHANNEL: String get() = BuildConfig.TELEGRAM_MOVIES_CHANNEL.ifBlank { DEFAULT_TELEGRAM_MOVIES_CHANNEL }
    val TELEGRAM_SERIES_CHANNEL: String get() = BuildConfig.TELEGRAM_SERIES_CHANNEL.ifBlank { DEFAULT_TELEGRAM_SERIES_CHANNEL }

    /** MyAnimeList v2 REST API base URL. */
    const val MAL_BASE_URL: String = "https://api.myanimelist.net/v2/"

    /**
     * True only when built as debug. Used to gate verbose HTTP logging in TmdbClient.
     * Never use this to gate security features — only for log verbosity.
     */
    val DEBUG_LOGGING: Boolean get() = BuildConfig.DEBUG_LOGGING
}
