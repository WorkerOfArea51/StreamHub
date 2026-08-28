package com.streamhub.app.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource

import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.streamhub.app.data.api.SharedHttpClient

/**
 * High-performance Direct HTTP & Local File DataSource Factory for Media3 ExoPlayer.
 *
 * Supports:
 * - Direct HTTP/HTTPS progressive streaming (MP4, MKV, WebM, HLS) via robust OkHttp
 * - Multi-gigabyte sparse chunk caching via [StreamCacheManager]
 * - Local offline media playback via [FileDataSource]
 * - Byte-range seeking (Accept-Ranges: bytes)
 */
@OptIn(UnstableApi::class)
class StreamDataSourceFactory(
    context: Context
) : DataSource.Factory {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "StreamDataSourceFactory"
        const val LOCAL_FILE_SCHEME = "file"
    }

    private val okHttpDataSourceFactory = OkHttpDataSource.Factory(SharedHttpClient.baseClient)
        .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

    private val cacheDataSourceFactory: CacheDataSource.Factory by lazy {
        val cache = StreamCacheManager.getCache(appContext)
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(okHttpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    override fun createDataSource(): DataSource {
        val cachedHttpSource = cacheDataSourceFactory.createDataSource()
        val directHttpSource = okHttpDataSourceFactory.createDataSource()
        val fileSource = FileDataSource()

        return object : DataSource {
            private var currentSource: DataSource? = null
            private var transferListener: TransferListener? = null

            override fun addTransferListener(transferListener: TransferListener) {
                this.transferListener = transferListener
                cachedHttpSource.addTransferListener(transferListener)
                directHttpSource.addTransferListener(transferListener)
                fileSource.addTransferListener(transferListener)
            }

            override fun open(dataSpec: DataSpec): Long {
                try { currentSource?.close() } catch (_: Exception) {}
                val uri = dataSpec.uri
                val scheme = uri.scheme?.lowercase()

                val isLocalFile = scheme == LOCAL_FILE_SCHEME || scheme == null || uri.path?.startsWith("/") == true

                currentSource = when {
                    isLocalFile -> fileSource
                    else -> cachedHttpSource
                }
                transferListener?.let { currentSource?.addTransferListener(it) }
                return try {
                    currentSource!!.open(dataSpec)
                } catch (e: Exception) {
                    if (!isLocalFile && currentSource !== directHttpSource) {
                        Log.w(TAG, "Cache stream open failed, falling back to direct OkHttp stream: ${e.message}")
                        currentSource = directHttpSource
                        transferListener?.let { currentSource?.addTransferListener(it) }
                        currentSource!!.open(dataSpec)
                    } else {
                        throw e
                    }
                }
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                return currentSource?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
            }

            override fun getUri(): Uri? = currentSource?.uri

            override fun close() {
                try {
                    currentSource?.close()
                } finally {
                    currentSource = null
                }
            }
        }
    }
}
