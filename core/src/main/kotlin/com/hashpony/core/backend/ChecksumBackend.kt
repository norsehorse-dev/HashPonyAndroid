package com.hashpony.core.backend

import com.hashpony.core.StreamingDigest
import java.util.zip.Checksum

/**
 * Adapts a [Checksum] (CRC32, Adler-32 from java.util.zip) to [StreamingDigest].
 * The 32-bit result is emitted big-endian, the conventional on-disk order for
 * `.sfv` and the like.
 */
class ChecksumBackend(private val checksum: Checksum) : StreamingDigest {
    override val outputBytes: Int = 4

    override fun update(input: ByteArray, offset: Int, length: Int) = checksum.update(input, offset, length)

    override fun digest(): ByteArray {
        val v = checksum.value and 0xffffffffL
        reset()
        return byteArrayOf(
            (v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte(),
        )
    }

    override fun reset() = checksum.reset()
}
