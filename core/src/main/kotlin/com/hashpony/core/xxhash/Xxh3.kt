/*
 * XXH3 (64-bit and 128-bit), one-shot and streaming. See XxhInternal.kt for the
 * port's provenance and licensing. This is the dense part of the family; the
 * scalar reference paths are transcribed faithfully and verified end to end
 * against the official implementation's output.
 */
package com.hashpony.core.xxhash

import com.hashpony.core.StreamingDigest

// ===========================================================================
// Shared 16-byte mixer
// ===========================================================================

private fun mix16B(input: ByteArray, inOff: Int, secret: ByteArray, secOff: Int, seed: ULong): ULong {
    val inputLo = readLE64(input, inOff)
    val inputHi = readLE64(input, inOff + 8)
    return mul128Fold64(
        inputLo xor (readLE64(secret, secOff) + seed),
        inputHi xor (readLE64(secret, secOff + 8) - seed),
    )
}

// ===========================================================================
// XXH3-64 one-shot
// ===========================================================================

private fun len1to3_64(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULong {
    val c1 = input[off].toUByte().toUInt()
    val c2 = input[off + (len shr 1)].toUByte().toUInt()
    val c3 = input[off + len - 1].toUByte().toUInt()
    val combined = (c1 shl 16) or (c2 shl 24) or c3 or (len.toUInt() shl 8)
    val bitflip = (readLE32(secret, 0) xor readLE32(secret, 4)).toULong() + seed
    return xxh64Avalanche(combined.toULong() xor bitflip)
}

private fun len4to8_64(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULong {
    val s = seed xor (swap32(seed.toUInt()).toULong() shl 32)
    val input1 = readLE32(input, off)
    val input2 = readLE32(input, off + len - 4)
    val bitflip = (readLE64(secret, 8) xor readLE64(secret, 16)) - s
    val input64 = input2.toULong() + (input1.toULong() shl 32)
    return rrmxmx(input64 xor bitflip, len.toULong())
}

private fun len9to16_64(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULong {
    val bitflip1 = (readLE64(secret, 24) xor readLE64(secret, 32)) + seed
    val bitflip2 = (readLE64(secret, 40) xor readLE64(secret, 48)) - seed
    val inputLo = readLE64(input, off) xor bitflip1
    val inputHi = readLE64(input, off + len - 8) xor bitflip2
    val acc = len.toULong() + swap64(inputLo) + inputHi + mul128Fold64(inputLo, inputHi)
    return xxh3Avalanche(acc)
}

private fun len0to16_64(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULong {
    if (len > 8) return len9to16_64(input, off, len, secret, seed)
    if (len >= 4) return len4to8_64(input, off, len, secret, seed)
    if (len > 0) return len1to3_64(input, off, len, secret, seed)
    return xxh64Avalanche(seed xor (readLE64(secret, 56) xor readLE64(secret, 64)))
}

private fun len17to128_64(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULong {
    var acc = len.toULong() * P64_1
    if (len > 32) {
        if (len > 64) {
            if (len > 96) {
                acc += mix16B(input, off + 48, secret, 96, seed)
                acc += mix16B(input, off + len - 64, secret, 112, seed)
            }
            acc += mix16B(input, off + 32, secret, 64, seed)
            acc += mix16B(input, off + len - 48, secret, 80, seed)
        }
        acc += mix16B(input, off + 16, secret, 32, seed)
        acc += mix16B(input, off + len - 32, secret, 48, seed)
    }
    acc += mix16B(input, off, secret, 0, seed)
    acc += mix16B(input, off + len - 16, secret, 16, seed)
    return xxh3Avalanche(acc)
}

private fun len129to240_64(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULong {
    var acc = len.toULong() * P64_1
    val nbRounds = len / 16
    for (i in 0 until 8) {
        acc += mix16B(input, off + 16 * i, secret, 16 * i, seed)
    }
    var accEnd = mix16B(input, off + len - 16, secret, SECRET_SIZE_MIN - MIDSIZE_LASTOFFSET, seed)
    acc = xxh3Avalanche(acc)
    for (i in 8 until nbRounds) {
        accEnd += mix16B(input, off + 16 * i, secret, 16 * (i - 8) + MIDSIZE_STARTOFFSET, seed)
    }
    return xxh3Avalanche(acc + accEnd)
}

internal fun xxh3_64(input: ByteArray, off: Int, len: Int, seed: ULong): ULong {
    if (len <= 16) return len0to16_64(input, off, len, K_SECRET, seed)
    if (len <= 128) return len17to128_64(input, off, len, K_SECRET, seed)
    if (len <= MIDSIZE_MAX) return len129to240_64(input, off, len, K_SECRET, seed)
    val secret = if (seed == 0uL) K_SECRET else initCustomSecret(seed)
    val acc = initAcc()
    hashLongLoop(acc, input, off, len, secret, secret.size)
    return mergeAccs(acc, secret, SECRET_MERGEACCS_START, len.toULong() * P64_1)
}

// ===========================================================================
// XXH3-128 one-shot
// ===========================================================================

private fun len1to3_128(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULongArray {
    val c1 = input[off].toUByte().toUInt()
    val c2 = input[off + (len shr 1)].toUByte().toUInt()
    val c3 = input[off + len - 1].toUByte().toUInt()
    val combinedl = (c1 shl 16) or (c2 shl 24) or c3 or (len.toUInt() shl 8)
    val combinedh = swap32(combinedl).rotateLeft(13)
    val bitflipl = (readLE32(secret, 0) xor readLE32(secret, 4)).toULong() + seed
    val bitfliph = (readLE32(secret, 8) xor readLE32(secret, 12)).toULong() - seed
    val low = xxh64Avalanche(combinedl.toULong() xor bitflipl)
    val high = xxh64Avalanche(combinedh.toULong() xor bitfliph)
    return ulongArrayOf(low, high)
}

private fun len4to8_128(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULongArray {
    val s = seed xor (swap32(seed.toUInt()).toULong() shl 32)
    val inputLo = readLE32(input, off)
    val inputHi = readLE32(input, off + len - 4)
    val input64 = inputLo.toULong() + (inputHi.toULong() shl 32)
    val bitflip = (readLE64(secret, 16) xor readLE64(secret, 24)) + s
    val keyed = input64 xor bitflip
    val mul = P64_1 + (len.toULong() shl 2)
    var mLow = mul128Low(keyed, mul)
    var mHigh = mul128High(keyed, mul)
    mHigh += (mLow shl 1)
    mLow = mLow xor (mHigh shr 3)
    mLow = xorshift64(mLow, 35); mLow *= PRIME_MX2; mLow = xorshift64(mLow, 28)
    mHigh = xxh3Avalanche(mHigh)
    return ulongArrayOf(mLow, mHigh)
}

private fun len9to16_128(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULongArray {
    val bitflipl = (readLE64(secret, 32) xor readLE64(secret, 40)) - seed
    val bitfliph = (readLE64(secret, 48) xor readLE64(secret, 56)) + seed
    val inputLo = readLE64(input, off)
    var inputHi = readLE64(input, off + len - 8)
    var mLow = mul128Low(inputLo xor inputHi xor bitflipl, P64_1)
    var mHigh = mul128High(inputLo xor inputHi xor bitflipl, P64_1)
    mLow += (len - 1).toULong() shl 54
    inputHi = inputHi xor bitfliph
    mHigh += inputHi + (inputHi and 0xffffffffuL) * (P32_2 - 1u).toULong()
    mLow = mLow xor swap64(mHigh)
    var hLow = mul128Low(mLow, P64_2)
    var hHigh = mul128High(mLow, P64_2) + mHigh * P64_2
    hLow = xxh3Avalanche(hLow)
    hHigh = xxh3Avalanche(hHigh)
    return ulongArrayOf(hLow, hHigh)
}

private fun len0to16_128(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULongArray {
    if (len > 8) return len9to16_128(input, off, len, secret, seed)
    if (len >= 4) return len4to8_128(input, off, len, secret, seed)
    if (len > 0) return len1to3_128(input, off, len, secret, seed)
    val bitflipl = readLE64(secret, 64) xor readLE64(secret, 72)
    val bitfliph = readLE64(secret, 80) xor readLE64(secret, 88)
    return ulongArrayOf(xxh64Avalanche(seed xor bitflipl), xxh64Avalanche(seed xor bitfliph))
}

/** XXH128_mix32B — mutates acc[0]=low, acc[1]=high in place. */
private fun mix32B(acc: ULongArray, input: ByteArray, in1: Int, in2: Int, secret: ByteArray, secOff: Int, seed: ULong) {
    acc[0] += mix16B(input, in1, secret, secOff, seed)
    acc[0] = acc[0] xor (readLE64(input, in2) + readLE64(input, in2 + 8))
    acc[1] += mix16B(input, in2, secret, secOff + 16, seed)
    acc[1] = acc[1] xor (readLE64(input, in1) + readLE64(input, in1 + 8))
}

private fun finalize128(acc: ULongArray, len: Int, seed: ULong): ULongArray {
    var hLow = acc[0] + acc[1]
    var hHigh = acc[0] * P64_1 + acc[1] * P64_4 + (len.toULong() - seed) * P64_2
    hLow = xxh3Avalanche(hLow)
    hHigh = 0uL - xxh3Avalanche(hHigh)
    return ulongArrayOf(hLow, hHigh)
}

private fun len17to128_128(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULongArray {
    val acc = ulongArrayOf(len.toULong() * P64_1, 0uL)
    if (len > 32) {
        if (len > 64) {
            if (len > 96) {
                mix32B(acc, input, off + 48, off + len - 64, secret, 96, seed)
            }
            mix32B(acc, input, off + 32, off + len - 48, secret, 64, seed)
        }
        mix32B(acc, input, off + 16, off + len - 32, secret, 32, seed)
    }
    mix32B(acc, input, off, off + len - 16, secret, 0, seed)
    return finalize128(acc, len, seed)
}

private fun len129to240_128(input: ByteArray, off: Int, len: Int, secret: ByteArray, seed: ULong): ULongArray {
    val acc = ulongArrayOf(len.toULong() * P64_1, 0uL)
    var i = 32
    while (i < 160) {
        mix32B(acc, input, off + i - 32, off + i - 16, secret, i - 32, seed)
        i += 32
    }
    acc[0] = xxh3Avalanche(acc[0])
    acc[1] = xxh3Avalanche(acc[1])
    i = 160
    while (i <= len) {
        mix32B(acc, input, off + i - 32, off + i - 16, secret, MIDSIZE_STARTOFFSET + i - 160, seed)
        i += 32
    }
    mix32B(acc, input, off + len - 16, off + len - 32, secret, SECRET_SIZE_MIN - MIDSIZE_LASTOFFSET - 16, 0uL - seed)
    return finalize128(acc, len, seed)
}

/** Returns [low64, high64]. */
internal fun xxh3_128(input: ByteArray, off: Int, len: Int, seed: ULong): ULongArray {
    if (len <= 16) return len0to16_128(input, off, len, K_SECRET, seed)
    if (len <= 128) return len17to128_128(input, off, len, K_SECRET, seed)
    if (len <= MIDSIZE_MAX) return len129to240_128(input, off, len, K_SECRET, seed)
    val secret = if (seed == 0uL) K_SECRET else initCustomSecret(seed)
    val acc = initAcc()
    hashLongLoop(acc, input, off, len, secret, secret.size)
    val low = mergeAccs(acc, secret, SECRET_MERGEACCS_START, len.toULong() * P64_1)
    val high = mergeAccs(acc, secret, secret.size - 64 - SECRET_MERGEACCS_START, (len.toULong() * P64_2).inv())
    return ulongArrayOf(low, high)
}

// ===========================================================================
// XXH3 streaming digest (64 or 128 bit output)
// ===========================================================================

/**
 * Streaming XXH3. [bits] selects 64- or 128-bit output. When [seed] is non-zero
 * the custom secret is derived once here, matching XXH3_*_reset_withSeed.
 */
class Xxh3Digest(private val bits: Int, private val seed: ULong = 0uL) : StreamingDigest {
    init { require(bits == 64 || bits == 128) { "XXH3 output must be 64 or 128 bits" } }

    override val outputBytes: Int = bits / 8

    private val secret: ByteArray = if (seed == 0uL) K_SECRET else initCustomSecret(seed)
    private val secretLimit = SECRET_DEFAULT_SIZE - STRIPE_LEN // 128
    private val nbStripesPerBlock = secretLimit / SECRET_CONSUME_RATE // 16

    private val acc = initAcc()
    private val buffer = ByteArray(INTERNALBUFFER_SIZE)
    private var bufferedSize = 0
    private var nbStripesSoFar = 0
    private var totalLen: ULong = 0uL

    override fun reset() {
        val fresh = initAcc()
        for (i in 0 until ACC_NB) acc[i] = fresh[i]
        bufferedSize = 0
        nbStripesSoFar = 0
        totalLen = 0uL
    }

    /** Accumulate [nbStripes] stripes from [arr]@[inOff]; updates [target] and returns new nbStripesSoFar. */
    private fun consumeStripes(target: ULongArray, soFarIn: Int, arr: ByteArray, inOffIn: Int, nbStripes: Int): Int {
        var soFar = soFarIn
        var inOff = inOffIn
        var nb = nbStripes
        if (nb >= nbStripesPerBlock - soFar) {
            var nbThisIter = nbStripesPerBlock - soFar
            var initialSecOff = soFar * SECRET_CONSUME_RATE
            do {
                for (s in 0 until nbThisIter) {
                    accumulate512(target, arr, inOff + s * STRIPE_LEN, secret, initialSecOff + s * SECRET_CONSUME_RATE)
                }
                scrambleAcc(target, secret, secretLimit)
                inOff += nbThisIter * STRIPE_LEN
                nb -= nbThisIter
                nbThisIter = nbStripesPerBlock
                initialSecOff = 0
            } while (nb >= nbStripesPerBlock)
            soFar = 0
        }
        if (nb > 0) {
            val startOff = soFar * SECRET_CONSUME_RATE
            for (s in 0 until nb) {
                accumulate512(target, arr, inOff + s * STRIPE_LEN, secret, startOff + s * SECRET_CONSUME_RATE)
            }
            soFar += nb
        }
        return soFar
    }

    override fun update(input: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        val end = offset + length
        totalLen += length.toULong()

        if (length <= INTERNALBUFFER_SIZE - bufferedSize) {
            System.arraycopy(input, offset, buffer, bufferedSize, length)
            bufferedSize += length
            return
        }

        var p = offset
        if (bufferedSize > 0) {
            val loadSize = INTERNALBUFFER_SIZE - bufferedSize
            System.arraycopy(input, p, buffer, bufferedSize, loadSize)
            p += loadSize
            nbStripesSoFar = consumeStripes(acc, nbStripesSoFar, buffer, 0, INTERNALBUFFER_SIZE / STRIPE_LEN)
            bufferedSize = 0
        }

        if (end - p > INTERNALBUFFER_SIZE) {
            val nbStripes = (end - 1 - p) / STRIPE_LEN
            nbStripesSoFar = consumeStripes(acc, nbStripesSoFar, input, p, nbStripes)
            p += nbStripes * STRIPE_LEN
            System.arraycopy(input, p - STRIPE_LEN, buffer, buffer.size - STRIPE_LEN, STRIPE_LEN)
        }

        val rem = end - p
        System.arraycopy(input, p, buffer, 0, rem)
        bufferedSize = rem
    }

    /** XXH3_digest_long — accumulates the tail into a copy of [acc]. */
    private fun digestLong(): ULongArray {
        val a = acc.copyOf()
        if (bufferedSize >= STRIPE_LEN) {
            val nbStripes = (bufferedSize - 1) / STRIPE_LEN
            consumeStripes(a, nbStripesSoFar, buffer, 0, nbStripes)
            accumulate512(a, buffer, bufferedSize - STRIPE_LEN, secret, secretLimit - SECRET_LASTACC_START)
        } else {
            val catchup = STRIPE_LEN - bufferedSize
            val lastStripe = ByteArray(STRIPE_LEN)
            System.arraycopy(buffer, buffer.size - catchup, lastStripe, 0, catchup)
            System.arraycopy(buffer, 0, lastStripe, catchup, bufferedSize)
            accumulate512(a, lastStripe, 0, secret, secretLimit - SECRET_LASTACC_START)
        }
        return a
    }

    override fun digest(): ByteArray {
        val out: ByteArray
        if (totalLen > MIDSIZE_MAX.toULong()) {
            val a = digestLong()
            val low = mergeAccs(a, secret, SECRET_MERGEACCS_START, totalLen * P64_1)
            if (bits == 64) {
                out = be64(low)
            } else {
                val high = mergeAccs(
                    a, secret,
                    secretLimit + STRIPE_LEN - 64 - SECRET_MERGEACCS_START,
                    (totalLen * P64_2).inv(),
                )
                out = be64(high) + be64(low)
            }
        } else {
            val len = totalLen.toInt()
            if (bits == 64) {
                out = be64(xxh3_64(buffer, 0, len, seed))
            } else {
                val h = xxh3_128(buffer, 0, len, seed) // [low, high]
                out = be64(h[1]) + be64(h[0])
            }
        }
        reset()
        return out
    }

    private fun be64(v: ULong): ByteArray = byteArrayOf(
        (v shr 56).toByte(), (v shr 48).toByte(), (v shr 40).toByte(), (v shr 32).toByte(),
        (v shr 24).toByte(), (v shr 16).toByte(), (v shr 8).toByte(), v.toByte(),
    )
}
