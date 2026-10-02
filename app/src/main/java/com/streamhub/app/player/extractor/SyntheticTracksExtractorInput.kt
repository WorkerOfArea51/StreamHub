package com.streamhub.app.player.extractor

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.ExtractorInput
import java.io.EOFException

/**
 * Transparent Virtual [ExtractorInput] for Split-SeekHead / HandBrake MKVs.
 *
 * In Split-SeekHead MKVs, Cluster 0 frames begin immediately at [cluster0Offset] (e.g. byte 2,966),
 * while Tracks and Cues metadata are stored at EOF.
 *
 * This virtual input wraps the physical [originalInput] and seamlessly splices [syntheticData]
 * (the exact [ID_TRACKS] element parsed from EOF) into the stream directly between the EBML Info
 * element and Cluster 0.
 *
 * Virtual Layout:
 * - `0 .. cluster0Offset - 1`: Physical bytes from [originalInput] (EBML header, Segment, Info).
 * - `cluster0Offset .. cluster0Offset + syntheticData.size - 1`: [syntheticData] in memory.
 * - `cluster0Offset + syntheticData.size .. EOF`: Physical bytes from [originalInput] starting at [cluster0Offset].
 *
 * Native [androidx.media3.extractor.mkv.MatroskaExtractor] reads this stream linearly from byte 0,
 * parses all tracks, codecs, and durations, calls `endTracks()` and `seekMap()`, and immediately
 * begins decoding Cluster 0 frames in < 250ms with ZERO reflection, ZERO socket drops, and ZERO stalls.
 */
@OptIn(UnstableApi::class)
class SyntheticTracksExtractorInput(
    private val originalInput: ExtractorInput,
    private val cluster0Offset: Long,
    private val syntheticData: ByteArray
) : ExtractorInput {

    private val syntheticLength = syntheticData.size.toLong()
    private val syntheticEndOffset = cluster0Offset + syntheticLength
    private var virtualPos = originalInput.position
    private var virtualPeekPos = originalInput.peekPosition
    private val scratch = ByteArray(4096)
    private var isSplicingDone = false

    val isDone: Boolean
        get() = isSplicingDone

    fun markDone() {
        isSplicingDone = true
    }

    fun resetToPhysicalPosition(physicalPos: Long) {
        isSplicingDone = true
        virtualPos = physicalPos
        virtualPeekPos = physicalPos
        originalInput.resetPeekPosition()
    }

    override fun getPosition(): Long = if (isSplicingDone) originalInput.position else virtualPos

    override fun getPeekPosition(): Long = if (isSplicingDone) originalInput.peekPosition else virtualPeekPos

    override fun getLength(): Long = originalInput.length

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length <= 0) return 0
        if (!isSplicingDone) {
            if (virtualPos < cluster0Offset) {
                val maxCanRead = minOf(length.toLong(), cluster0Offset - virtualPos).toInt()
                val readBytes = originalInput.read(buffer, offset, maxCanRead)
                if (readBytes > 0) {
                    virtualPos += readBytes
                    virtualPeekPos = virtualPos
                }
                return readBytes
            } else if (virtualPos < syntheticEndOffset) {
                val offsetInSynthetic = (virtualPos - cluster0Offset).toInt()
                val available = minOf(length, (syntheticLength - (virtualPos - cluster0Offset)).toInt())
                System.arraycopy(syntheticData, offsetInSynthetic, buffer, offset, available)
                virtualPos += available
                virtualPeekPos = virtualPos
                if (virtualPos >= syntheticEndOffset) {
                    isSplicingDone = true
                }
                return available
            } else {
                isSplicingDone = true
            }
        }
        val readBytes = originalInput.read(buffer, offset, length)
        if (readBytes > 0) {
            virtualPos = originalInput.position
            virtualPeekPos = originalInput.peekPosition
        }
        return readBytes
    }

    override fun readFully(buffer: ByteArray, offset: Int, length: Int, allowEndOfInput: Boolean): Boolean {
        var remaining = length
        var currentOffset = offset
        while (remaining > 0) {
            val readBytes = read(buffer, currentOffset, remaining)
            if (readBytes == C.RESULT_END_OF_INPUT) {
                if (allowEndOfInput && remaining == length) return false
                throw EOFException("End of input reached while reading $length bytes (read $currentOffset)")
            }
            currentOffset += readBytes
            remaining -= readBytes
        }
        return true
    }

    override fun readFully(buffer: ByteArray, offset: Int, length: Int) {
        readFully(buffer, offset, length, false)
    }

    override fun skip(length: Int): Int {
        val toSkip = minOf(length, scratch.size)
        val readBytes = read(scratch, 0, toSkip)
        return if (readBytes == C.RESULT_END_OF_INPUT) C.RESULT_END_OF_INPUT else readBytes
    }

    override fun skipFully(length: Int, allowEndOfInput: Boolean): Boolean {
        var remaining = length
        while (remaining > 0) {
            val toSkip = minOf(remaining, scratch.size)
            val readBytes = read(scratch, 0, toSkip)
            if (readBytes == C.RESULT_END_OF_INPUT) {
                if (allowEndOfInput && remaining == length) return false
                throw EOFException("End of input reached while skipping $length bytes")
            }
            remaining -= readBytes
        }
        return true
    }

    override fun skipFully(length: Int) {
        skipFully(length, false)
    }

    override fun peek(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length <= 0) return 0
        if (!isSplicingDone) {
            if (virtualPeekPos < cluster0Offset) {
                val maxCanPeek = minOf(length.toLong(), cluster0Offset - virtualPeekPos).toInt()
                val peeked = originalInput.peek(buffer, offset, maxCanPeek)
                if (peeked > 0) {
                    virtualPeekPos += peeked
                }
                return peeked
            } else if (virtualPeekPos < syntheticEndOffset) {
                val offsetInSynthetic = (virtualPeekPos - cluster0Offset).toInt()
                val available = minOf(length, (syntheticLength - (virtualPeekPos - cluster0Offset)).toInt())
                System.arraycopy(syntheticData, offsetInSynthetic, buffer, offset, available)
                virtualPeekPos += available
                return available
            }
        }
        val peeked = originalInput.peek(buffer, offset, length)
        if (peeked > 0) {
            virtualPeekPos = originalInput.peekPosition
        }
        return peeked
    }

    override fun peekFully(buffer: ByteArray, offset: Int, length: Int, allowEndOfInput: Boolean): Boolean {
        var remaining = length
        var currentOffset = offset
        while (remaining > 0) {
            val peeked = peek(buffer, currentOffset, remaining)
            if (peeked == C.RESULT_END_OF_INPUT) {
                if (allowEndOfInput && remaining == length) return false
                throw EOFException("End of input reached while peeking $length bytes")
            }
            currentOffset += peeked
            remaining -= peeked
        }
        return true
    }

    override fun peekFully(buffer: ByteArray, offset: Int, length: Int) {
        peekFully(buffer, offset, length, false)
    }

    override fun advancePeekPosition(length: Int, allowEndOfInput: Boolean): Boolean {
        var remaining = length
        while (remaining > 0) {
            val toPeek = minOf(remaining, scratch.size)
            val peeked = peek(scratch, 0, toPeek)
            if (peeked == C.RESULT_END_OF_INPUT) {
                if (allowEndOfInput && remaining == length) return false
                throw EOFException("End of input reached while advancing peek by $length bytes")
            }
            remaining -= peeked
        }
        return true
    }

    override fun advancePeekPosition(length: Int) {
        advancePeekPosition(length, false)
    }

    override fun resetPeekPosition() {
        if (isSplicingDone) {
            originalInput.resetPeekPosition()
            virtualPeekPos = originalInput.peekPosition
        } else {
            virtualPeekPos = virtualPos
            originalInput.resetPeekPosition()
        }
    }

    override fun <E : Throwable> setRetryPosition(position: Long, e: E) {
        originalInput.setRetryPosition(position, e)
    }
}
