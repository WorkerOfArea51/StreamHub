package com.streamhub.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import java.util.concurrent.atomic.AtomicLong

/**
 * Real-Time Network Bandwidth & Transfer Speed Tracker for Media3 ExoPlayer.
 *
 * Tracks actual HTTP bytes transferred over the network via [TransferListener]
 * to provide accurate, real-time throughput metrics (KB/s and MB/s) for Stats for Nerds
 * and adaptive quality selection, without confusing playback timeline advancement with network bytes.
 */
@OptIn(UnstableApi::class)
class StreamBandwidthTracker(context: Context) : TransferListener {

    val bandwidthMeter: DefaultBandwidthMeter = DefaultBandwidthMeter.Builder(context.applicationContext).build()

    private val bytesInWindow = AtomicLong(0L)
    private var lastSampleTimeMs = System.currentTimeMillis()

    /** Current instantaneous transfer speed in Kilobytes per second (KB/s). */
    @Volatile
    var currentSpeedKBps: Long = 0L
        private set

    /** Peak transfer speed achieved during the active playback session in KB/s. */
    @Volatile
    var peakSpeedKBps: Long = 0L
        private set

    override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
        bandwidthMeter.onTransferInitializing(source, dataSpec, isNetwork)
    }

    override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
        bandwidthMeter.onTransferStart(source, dataSpec, isNetwork)
    }

    override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
        bandwidthMeter.onBytesTransferred(source, dataSpec, isNetwork, bytesTransferred)
        if (isNetwork && bytesTransferred > 0) {
            bytesInWindow.addAndGet(bytesTransferred.toLong())
        }
    }

    override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
        bandwidthMeter.onTransferEnd(source, dataSpec, isNetwork)
    }

    /**
     * Samples the transferred bytes over the elapsed window (min 500ms)
     * and calculates the real-time download throughput in KB/s.
     */
    fun sampleSpeedKBps(): Long {
        val now = System.currentTimeMillis()
        val elapsed = now - lastSampleTimeMs
        if (elapsed >= 500L) {
            val bytes = bytesInWindow.getAndSet(0L)
            currentSpeedKBps = if (bytes > 0L) {
                (bytes * 1000L) / (elapsed * 1024L)
            } else {
                0L
            }
            if (currentSpeedKBps > peakSpeedKBps) {
                peakSpeedKBps = currentSpeedKBps
            }
            lastSampleTimeMs = now
        }
        return currentSpeedKBps
    }

    /**
     * Estimated connection bitrate from Media3's internal sliding window in bits per second.
     */
    fun getEstimatedBitrate(): Long = bandwidthMeter.bitrateEstimate
}
