/*
 * XXH32 and XXH64 streaming digests. See XxhInternal.kt for the port's
 * provenance and licensing.
 */
package com.hashpony.core.xxhash

import com.hashpony.core.StreamingDigest

private fun be32(v: UInt): ByteArray = byteArrayOf(
    (v shr 24).toByte(), (v shr 16).toByte(), (v shr 8).toByte(), v.toByte(),
)

private fun be64(v: ULong): ByteArray = byteArrayOf(
    (v shr 56).toByte(), (v shr 48).toByte(), (v shr 40).toByte(), (v shr 32).toByte(),
    (v shr 24).toByte(), (v shr 16).toByte(), (v shr 8).toByte(), v.toByte(),
)

/** XXH32, streaming. */
class Xxh32Digest(private val seed: UInt = 0u) : StreamingDigest {
    override val outputBytes: Int = 4

    private var totalLen: ULong = 0uL
    private var v1: UInt = 0u
    private var v2: UInt = 0u
    private var v3: UInt = 0u
    private var v4: UInt = 0u
    private val mem = ByteArray(16)
    private var memSize = 0

    init { reset() }

    override fun reset() {
        totalLen = 0uL
        v1 = seed + P32_1 + P32_2
        v2 = seed + P32_2
        v3 = seed
        v4 = seed - P32_1
        memSize = 0
    }

    private fun round(acc: UInt, input: UInt): UInt {
        var a = acc + input * P32_2
        a = a.rotateLeft(13)
        return a * P32_1
    }

    override fun update(input: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        var off = offset
        var len = length
        totalLen += len.toULong()

        if (memSize + len < 16) {
            System.arraycopy(input, off, mem, memSize, len)
            memSize += len
            return
        }
        if (memSize > 0) {
            val fill = 16 - memSize
            System.arraycopy(input, off, mem, memSize, fill)
            v1 = round(v1, readLE32(mem, 0))
            v2 = round(v2, readLE32(mem, 4))
            v3 = round(v3, readLE32(mem, 8))
            v4 = round(v4, readLE32(mem, 12))
            off += fill
            len -= fill
            memSize = 0
        }
        while (len >= 16) {
            v1 = round(v1, readLE32(input, off))
            v2 = round(v2, readLE32(input, off + 4))
            v3 = round(v3, readLE32(input, off + 8))
            v4 = round(v4, readLE32(input, off + 12))
            off += 16
            len -= 16
        }
        if (len > 0) {
            System.arraycopy(input, off, mem, 0, len)
            memSize = len
        }
    }

    override fun digest(): ByteArray {
        var h: UInt = if (totalLen >= 16uL) {
            v1.rotateLeft(1) + v2.rotateLeft(7) + v3.rotateLeft(12) + v4.rotateLeft(18)
        } else {
            seed + P32_5
        }
        h += totalLen.toUInt()

        var p = 0
        var rem = memSize
        while (rem >= 4) {
            h += readLE32(mem, p) * P32_3
            h = h.rotateLeft(17) * P32_4
            p += 4; rem -= 4
        }
        while (rem >= 1) {
            h += mem[p].toUByte().toUInt() * P32_5
            h = h.rotateLeft(11) * P32_1
            p += 1; rem -= 1
        }
        h = h xor (h shr 15); h *= P32_2
        h = h xor (h shr 13); h *= P32_3
        h = h xor (h shr 16)

        val out = be32(h)
        reset()
        return out
    }
}

/** XXH64, streaming. */
class Xxh64Digest(private val seed: ULong = 0uL) : StreamingDigest {
    override val outputBytes: Int = 8

    private var totalLen: ULong = 0uL
    private var v1: ULong = 0uL
    private var v2: ULong = 0uL
    private var v3: ULong = 0uL
    private var v4: ULong = 0uL
    private val mem = ByteArray(32)
    private var memSize = 0

    init { reset() }

    override fun reset() {
        totalLen = 0uL
        v1 = seed + P64_1 + P64_2
        v2 = seed + P64_2
        v3 = seed
        v4 = seed - P64_1
        memSize = 0
    }

    private fun round(acc: ULong, input: ULong): ULong {
        var a = acc + input * P64_2
        a = a.rotateLeft(31)
        return a * P64_1
    }

    private fun mergeRound(acc: ULong, value: ULong): ULong {
        val v = round(0uL, value)
        var a = acc xor v
        a = a * P64_1 + P64_4
        return a
    }

    override fun update(input: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        var off = offset
        var len = length
        totalLen += len.toULong()

        if (memSize + len < 32) {
            System.arraycopy(input, off, mem, memSize, len)
            memSize += len
            return
        }
        if (memSize > 0) {
            val fill = 32 - memSize
            System.arraycopy(input, off, mem, memSize, fill)
            v1 = round(v1, readLE64(mem, 0))
            v2 = round(v2, readLE64(mem, 8))
            v3 = round(v3, readLE64(mem, 16))
            v4 = round(v4, readLE64(mem, 24))
            off += fill
            len -= fill
            memSize = 0
        }
        while (len >= 32) {
            v1 = round(v1, readLE64(input, off))
            v2 = round(v2, readLE64(input, off + 8))
            v3 = round(v3, readLE64(input, off + 16))
            v4 = round(v4, readLE64(input, off + 24))
            off += 32
            len -= 32
        }
        if (len > 0) {
            System.arraycopy(input, off, mem, 0, len)
            memSize = len
        }
    }

    override fun digest(): ByteArray {
        var h: ULong
        if (totalLen >= 32uL) {
            h = v1.rotateLeft(1) + v2.rotateLeft(7) + v3.rotateLeft(12) + v4.rotateLeft(18)
            h = mergeRound(h, v1)
            h = mergeRound(h, v2)
            h = mergeRound(h, v3)
            h = mergeRound(h, v4)
        } else {
            h = seed + P64_5
        }
        h += totalLen

        var p = 0
        var rem = memSize
        while (rem >= 8) {
            val k1 = round(0uL, readLE64(mem, p))
            h = h xor k1
            h = h.rotateLeft(27) * P64_1 + P64_4
            p += 8; rem -= 8
        }
        if (rem >= 4) {
            h = h xor (readLE32(mem, p).toULong() * P64_1)
            h = h.rotateLeft(23) * P64_2 + P64_3
            p += 4; rem -= 4
        }
        while (rem >= 1) {
            h = h xor (mem[p].toUByte().toULong() * P64_5)
            h = h.rotateLeft(11) * P64_1
            p += 1; rem -= 1
        }
        h = xxh64Avalanche(h)

        val out = be64(h)
        reset()
        return out
    }
}
