package com.streamhub.app.player

import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import androidx.media3.common.C

/**
 * Hardware-accelerated Volume Boost Manager.
 *
 * Uses Android's [LoudnessEnhancer] attached to ExoPlayer's audio session.
 * Supports up to +19 dB (+1900 milliBels) of audio gain, effectively doubling (200%)
 * the perceived output volume without audible distortion or clipping.
 */
class VolumeBoostManager {

    companion object {
        private const val TAG = "VolumeBoostManager"
        private const val MAX_GAIN_MB = 1900 // +19 dB
    }

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentBoostPercent: Int = 0

    /**
     * Attach the LoudnessEnhancer to the player's active audio session ID.
     */
    fun attachToAudioSession(audioSessionId: Int) {
        release()
        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
            try {
                val enhancer = LoudnessEnhancer(audioSessionId)
                loudnessEnhancer = enhancer
                applyGain(currentBoostPercent)
                Log.i(TAG, "Attached LoudnessEnhancer to audioSessionId: $audioSessionId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize LoudnessEnhancer: ${e.message}")
            }
        }
    }

    /**
     * Set the volume boost percentage (0% to 100% boost above max system volume).
     * 0% = normal (disabled), 100% = +19 dB maximum volume boost (200% total volume).
     */
    fun setBoostPercent(percent: Int) {
        currentBoostPercent = percent.coerceIn(0, 100)
        applyGain(currentBoostPercent)
    }

    fun getBoostPercent(): Int = currentBoostPercent

    private fun applyGain(percent: Int) {
        try {
            loudnessEnhancer?.let { enhancer ->
                if (percent > 0) {
                    val gainMb = (percent.toFloat() / 100f * MAX_GAIN_MB).toInt()
                    enhancer.setTargetGain(gainMb)
                    enhancer.enabled = true
                } else {
                    enhancer.enabled = false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply target gain: ${e.message}")
        }
    }

    /**
     * Release audio effect resources on player teardown.
     */
    fun release() {
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing LoudnessEnhancer: ${e.message}")
        }
        loudnessEnhancer = null
    }
}
