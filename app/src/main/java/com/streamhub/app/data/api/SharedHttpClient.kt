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
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
