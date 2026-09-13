package com.streamhub.app.data.api

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * M10 FIX: Shared OkHttpClient singleton — all HTTP clients in the app
 * derive from this base instance to share connection pools, socket pools,
 * and thread dispatchers efficiently.
 */
object SharedHttpClient {
    val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Dedicated OkHttpClient instance tailored for progressive video streaming.
     * Uses zero read timeout (no socket drops when player pauses or buffers ahead),
     * keep-alive connection pooling, and connection retry.
     */
    val streamingClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS) // Resilient 15s read timeout to detect hung backend sockets promptly
            .writeTimeout(0, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionPool(okhttp3.ConnectionPool(5, 15, TimeUnit.SECONDS)) // 15s keep-alive: prevents stale/poisoned sockets from being reused
            .build()
    }
}

