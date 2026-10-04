package com.streamhub.app.player

import android.media.audiofx.Equalizer
import android.util.Log
import androidx.media3.common.C
import com.streamhub.app.data.AudioProfile

/**
 * Hardware-accelerated Dialogue Clarity & Night Cinema Equalizer.
 *
 * Attaches Android's [Equalizer] to ExoPlayer's active audio session ID.
 * - [AudioProfile.STANDARD]: Flat response (disabled / bypassed).
 * - [AudioProfile.CLEAR_DIALOGUE]: Amplifies human vocal presence frequencies (1.0 kHz - 3.5 kHz)
 *   by up to +5 dB, lifting dialogues and whispers directly above background music and FX.
 * - [AudioProfile.NIGHT_CINEMA]: Attenuates low-frequency explosion rumble (< 250 Hz) by -6 dB
 *   while boosting vocal clarity, preventing wall vibration and loud dynamic spikes at night.
 */
class DialogueEnhancerManager {

    companion object {
        private const val TAG = "DialogueEnhancer"
    }

    private var equalizer: Equalizer? = null
    private var currentProfile: AudioProfile = AudioProfile.STANDARD

    /**
     * Attaches the Equalizer to ExoPlayer's active audio session.
     */
    fun attachToAudioSession(audioSessionId: Int) {
        release()
        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
            try {
                // Priority 0, audioSessionId
                val eq = Equalizer(0, audioSessionId)
                equalizer = eq
                applyProfile(currentProfile)
                Log.i(TAG, "Attached Equalizer (bands=${eq.numberOfBands}) to session: $audioSessionId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to attach Equalizer to session $audioSessionId: ${e.message}")
            }
        }
    }

    /**
     * Sets the active audio enhancement profile.
     */
    fun setProfile(profile: AudioProfile) {
        currentProfile = profile
        applyProfile(profile)
    }

    fun getProfile(): AudioProfile = currentProfile

    private fun applyProfile(profile: AudioProfile) {
        val eq = equalizer ?: return
        try {
            when (profile) {
                AudioProfile.STANDARD -> {
                    // Reset all bands to neutral 0 mB (flat) then disable to save DSP cycles
                    val numBands = eq.numberOfBands.toInt()
                    for (i in 0 until numBands) {
                        eq.setBandLevel(i.toShort(), 0)
                    }
                    eq.enabled = false
                }
                AudioProfile.CLEAR_DIALOGUE -> {
                    eq.enabled = true
                    val numBands = eq.numberOfBands.toInt()
                    val range = eq.bandLevelRange // e.g. [-1500, 1500] in mB
                    val maxGain = if (range.size >= 2) range[1].toInt() else 1500
                    val targetBoostMb = (700).coerceAtMost(maxGain).toShort() // +7 dB vocal boost
                    val presenceBoostMb = (350).coerceAtMost(maxGain).toShort() // +3.5 dB presence
                    val bassTrimMb = (-250).toShort() // -2.5 dB trim on bass to reduce masking

                    for (i in 0 until numBands) {
                        val centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000 // mHz to Hz
                        when {
                            // Sub-bass trim to eliminate voice masking
                            centerFreqHz < 250 -> {
                                eq.setBandLevel(i.toShort(), bassTrimMb)
                            }
                            // Fundamental human vocal spectrum: 250 Hz to 4500 Hz
                            centerFreqHz in 250..4500 -> {
                                eq.setBandLevel(i.toShort(), targetBoostMb)
                            }
                            // Sibilance & consonant articulation: 4501 Hz to 9000 Hz
                            centerFreqHz in 4501..9000 -> {
                                eq.setBandLevel(i.toShort(), presenceBoostMb)
                            }
                            // Extreme highs neutral
                            else -> {
                                eq.setBandLevel(i.toShort(), 0)
                            }
                        }
                    }
                }
                AudioProfile.NIGHT_CINEMA -> {
                    eq.enabled = true
                    val numBands = eq.numberOfBands.toInt()
                    val range = eq.bandLevelRange
                    val minCut = if (range.size >= 2) range[0].toInt() else -1500
                    val maxGain = if (range.size >= 2) range[1].toInt() else 1500
                    val bassCutMb = (-600).coerceAtLeast(minCut).toShort() // -6 dB cut on explosions
                    val dialogueBoostMb = (400).coerceAtMost(maxGain).toShort() // +4 dB boost on speech

                    for (i in 0 until numBands) {
                        val centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000 // mHz to Hz
                        when {
                            // Low-end rumble / explosions: < 300 Hz
                            centerFreqHz < 300 -> {
                                eq.setBandLevel(i.toShort(), bassCutMb)
                            }
                            // Mid-range vocal frequencies: 900 Hz to 3500 Hz
                            centerFreqHz in 900..3500 -> {
                                eq.setBandLevel(i.toShort(), dialogueBoostMb)
                            }
                            // High frequencies: subtle attenuation to reduce sharp action clatter
                            centerFreqHz > 6000 -> {
                                eq.setBandLevel(i.toShort(), (-200).toShort())
                            }
                            else -> {
                                eq.setBandLevel(i.toShort(), 0)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying audio profile $profile: ${e.message}")
        }
    }

    /**
     * Releases Equalizer resources on player teardown.
     */
    fun release() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing Equalizer: ${e.message}")
        }
        equalizer = null
    }
}
