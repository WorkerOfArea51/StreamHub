package com.streamhub.app.player.extractor

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint

/**
 * Resilient Linear Interpolated [SeekMap] fallback (Tier 3 Universal Seek).
 *
 * Used when an MKV stream contains no Cues keyframe index anywhere (or Cues could not be
 * fetched from the network), preventing ExoPlayer from defaulting to [SeekMap.Unseekable]
 * and rewinding seeking/scrubbing to 00:00.
 *
 * Maps target playback timestamp proportionally across the data payload:
 * byteOffset = cluster0Offset + (timeUs / durationUs) * (fileLength - cluster0Offset)
 *
 * When ExoPlayer seeks to this byte offset, [MatroskaExtractor] synchronizes to the nearest
 * Cluster sync code (0x1F43B675) and resumes smooth playback rather than aborting to 0s.
 */
@OptIn(UnstableApi::class)
class LinearInterpolatedSeekMap(
    private val durationUs: Long,
    private val fileLength: Long,
    private val cluster0Offset: Long
) : SeekMap {

    override fun isSeekable(): Boolean = durationUs > 0L && fileLength > cluster0Offset

    override fun getDurationUs(): Long = durationUs

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
        if (!isSeekable) {
            return SeekMap.SeekPoints(SeekPoint(0L, cluster0Offset))
        }
        val clampedTimeUs = timeUs.coerceIn(0L, durationUs)
        val fraction = clampedTimeUs.toDouble() / durationUs.toDouble()
        val dataSpan = (fileLength - cluster0Offset).coerceAtLeast(1L)
        val byteOffset = cluster0Offset + (fraction * dataSpan).toLong()
        val clampedOffset = byteOffset.coerceIn(cluster0Offset, fileLength - 1L)
        val point = SeekPoint(clampedTimeUs, clampedOffset)
        return SeekMap.SeekPoints(point)
    }
}
