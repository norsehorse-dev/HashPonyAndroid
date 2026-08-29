/*
 * Vendored pure-Kotlin port of xxHash (XXH32, XXH64, XXH3-64, XXH3-128).
 *
 * Adapted from the reference C implementation by Yann Collet:
 *   https://github.com/Cyan4973/xxHash  (xxhash.h, v0.8.2)
 * Original work Copyright (c) Yann Collet, licensed under the BSD 2-Clause /
 * Apache-2.0 dual license. This Kotlin port carries the same attribution; see
 * the project NOTICE file. Verified against the reference vectors produced by
 * the official implementation (see :core test suite).
 *
 * No runtime dependency is introduced by this port: it exists specifically so
 * HashPony can offer the xxHash family without pulling hash4j (which reaches
 * memory through VarHandle, available only from API 33) or Zero-Allocation-
 * Hashing (which leans on sun.misc.Unsafe). Everything here is plain Kotlin and
 * runs unchanged on minSdk 26.
 */
package com.hashpony.core.xxhash

// ---------------------------------------------------------------------------
// Primes
// ---------------------------------------------------------------------------

internal const val P32_1: UInt = 0x9E3779B1u
internal const val P32_2: UInt = 0x85EBCA77u
internal const val P32_3: UInt = 0xC2B2AE3Du
internal const val P32_4: UInt = 0x27D4EB2Fu
internal const val P32_5: UInt = 0x165667B1u

internal const val P64_1: ULong = 0x9E3779B185EBCA87uL
internal const val P64_2: ULong = 0xC2B2AE3D27D4EB4FuL
internal const val P64_3: ULong = 0x165667B19E3779F9uL
internal const val P64_4: ULong = 0x85EBCA77C2B2AE63uL
internal const val P64_5: ULong = 0x27D4EB2F165667C5uL

internal const val PRIME_MX1: ULong = 0x165667919E3779F9uL
internal const val PRIME_MX2: ULong = 0x9FB21C651E98DF25uL

// ---------------------------------------------------------------------------
// Little-endian reads / writes
// ---------------------------------------------------------------------------

internal fun readLE32(b: ByteArray, i: Int): UInt =
    (b[i].toUByte().toUInt()) or
        (b[i + 1].toUByte().toUInt() shl 8) or
        (b[i + 2].toUByte().toUInt() shl 16) or
        (b[i + 3].toUByte().toUInt() shl 24)

internal fun readLE64(b: ByteArray, i: Int): ULong =
    (b[i].toUByte().toULong()) or
        (b[i + 1].toUByte().toULong() shl 8) or
        (b[i + 2].toUByte().toULong() shl 16) or
        (b[i + 3].toUByte().toULong() shl 24) or
        (b[i + 4].toUByte().toULong() shl 32) or
        (b[i + 5].toUByte().toULong() shl 40) or
        (b[i + 6].toUByte().toULong() shl 48) or
        (b[i + 7].toUByte().toULong() shl 56)

internal fun writeLE64(b: ByteArray, i: Int, v: ULong) {
    b[i] = (v and 0xffu).toByte()
    b[i + 1] = ((v shr 8) and 0xffu).toByte()
    b[i + 2] = ((v shr 16) and 0xffu).toByte()
    b[i + 3] = ((v shr 24) and 0xffu).toByte()
    b[i + 4] = ((v shr 32) and 0xffu).toByte()
    b[i + 5] = ((v shr 40) and 0xffu).toByte()
    b[i + 6] = ((v shr 48) and 0xffu).toByte()
    b[i + 7] = ((v shr 56) and 0xffu).toByte()
}

internal fun swap32(x: UInt): UInt = java.lang.Integer.reverseBytes(x.toInt()).toUInt()
internal fun swap64(x: ULong): ULong = java.lang.Long.reverseBytes(x.toLong()).toULong()

// ---------------------------------------------------------------------------
// 64x64 -> 128 multiply (portable scalar path) and folds
// ---------------------------------------------------------------------------

/** Low 64 bits of lhs*rhs. */
internal inline fun mul128Low(lhs: ULong, rhs: ULong): ULong = lhs * rhs

/** High 64 bits of lhs*rhs, computed with 32-bit grade-school partials. */
internal fun mul128High(lhs: ULong, rhs: ULong): ULong {
    val loLo = (lhs and 0xffffffffuL) * (rhs and 0xffffffffuL)
    val hiLo = (lhs shr 32) * (rhs and 0xffffffffuL)
    val loHi = (lhs and 0xffffffffuL) * (rhs shr 32)
    val hiHi = (lhs shr 32) * (rhs shr 32)
    val cross = (loLo shr 32) + (hiLo and 0xffffffffuL) + loHi
    return (hiLo shr 32) + (cross shr 32) + hiHi
}

/** low64 ^ high64 of the 128-bit product. */
internal fun mul128Fold64(lhs: ULong, rhs: ULong): ULong =
    mul128Low(lhs, rhs) xor mul128High(lhs, rhs)

// ---------------------------------------------------------------------------
// Avalanche stages
// ---------------------------------------------------------------------------

internal fun xorshift64(v: ULong, shift: Int): ULong = v xor (v shr shift)

/** XXH64_avalanche */
internal fun xxh64Avalanche(h0: ULong): ULong {
    var h = h0
    h = h xor (h shr 33); h *= P64_2
    h = h xor (h shr 29); h *= P64_3
    h = h xor (h shr 32)
    return h
}

/** XXH3_avalanche */
internal fun xxh3Avalanche(h0: ULong): ULong {
    var h = xorshift64(h0, 37)
    h *= PRIME_MX1
    return xorshift64(h, 32)
}

/** XXH3_rrmxmx */
internal fun rrmxmx(h0: ULong, len: ULong): ULong {
    var h = h0
    h = h xor (h.rotateLeft(49) xor h.rotateLeft(24))
    h *= PRIME_MX2
    h = h xor ((h shr 35) + len)
    h *= PRIME_MX2
    return xorshift64(h, 28)
}

// ---------------------------------------------------------------------------
// XXH3 secret + accumulator core
// ---------------------------------------------------------------------------

internal const val STRIPE_LEN = 64
internal const val ACC_NB = 8
internal const val SECRET_DEFAULT_SIZE = 192
internal const val SECRET_CONSUME_RATE = 8
internal const val SECRET_LASTACC_START = 7
internal const val SECRET_MERGEACCS_START = 11
internal const val MIDSIZE_MAX = 240
internal const val MIDSIZE_STARTOFFSET = 3
internal const val MIDSIZE_LASTOFFSET = 17
internal const val INTERNALBUFFER_SIZE = 256
internal const val SECRET_SIZE_MIN = 136

/** XXH3_kSecret — the 192-byte default secret, verbatim from xxhash.h. */
internal val K_SECRET: ByteArray = intArrayOf(
    0xb8, 0xfe, 0x6c, 0x39, 0x23, 0xa4, 0x4b, 0xbe, 0x7c, 0x01, 0x81, 0x2c, 0xf7, 0x21, 0xad, 0x1c,
    0xde, 0xd4, 0x6d, 0xe9, 0x83, 0x90, 0x97, 0xdb, 0x72, 0x40, 0xa4, 0xa4, 0xb7, 0xb3, 0x67, 0x1f,
    0xcb, 0x79, 0xe6, 0x4e, 0xcc, 0xc0, 0xe5, 0x78, 0x82, 0x5a, 0xd0, 0x7d, 0xcc, 0xff, 0x72, 0x21,
    0xb8, 0x08, 0x46, 0x74, 0xf7, 0x43, 0x24, 0x8e, 0xe0, 0x35, 0x90, 0xe6, 0x81, 0x3a, 0x26, 0x4c,
    0x3c, 0x28, 0x52, 0xbb, 0x91, 0xc3, 0x00, 0xcb, 0x88, 0xd0, 0x65, 0x8b, 0x1b, 0x53, 0x2e, 0xa3,
    0x71, 0x64, 0x48, 0x97, 0xa2, 0x0d, 0xf9, 0x4e, 0x38, 0x19, 0xef, 0x46, 0xa9, 0xde, 0xac, 0xd8,
    0xa8, 0xfa, 0x76, 0x3f, 0xe3, 0x9c, 0x34, 0x3f, 0xf9, 0xdc, 0xbb, 0xc7, 0xc7, 0x0b, 0x4f, 0x1d,
    0x8a, 0x51, 0xe0, 0x4b, 0xcd, 0xb4, 0x59, 0x31, 0xc8, 0x9f, 0x7e, 0xc9, 0xd9, 0x78, 0x73, 0x64,
    0xea, 0xc5, 0xac, 0x83, 0x34, 0xd3, 0xeb, 0xc3, 0xc5, 0x81, 0xa0, 0xff, 0xfa, 0x13, 0x63, 0xeb,
    0x17, 0x0d, 0xdd, 0x51, 0xb7, 0xf0, 0xda, 0x49, 0xd3, 0x16, 0x55, 0x26, 0x29, 0xd4, 0x68, 0x9e,
    0x2b, 0x16, 0xbe, 0x58, 0x7d, 0x47, 0xa1, 0xfc, 0x8f, 0xf8, 0xb8, 0xd1, 0x7a, 0xd0, 0x31, 0xce,
    0x45, 0xcb, 0x3a, 0x8f, 0x95, 0x16, 0x04, 0x28, 0xaf, 0xd7, 0xfb, 0xca, 0xbb, 0x4b, 0x40, 0x7e,
).map { it.toByte() }.toByteArray()

internal fun initCustomSecret(seed: ULong): ByteArray {
    val out = ByteArray(SECRET_DEFAULT_SIZE)
    for (i in 0 until SECRET_DEFAULT_SIZE / 16) {
        writeLE64(out, 16 * i, readLE64(K_SECRET, 16 * i) + seed)
        writeLE64(out, 16 * i + 8, readLE64(K_SECRET, 16 * i + 8) - seed)
    }
    return out
}

internal fun initAcc(): ULongArray = ulongArrayOf(
    P32_3.toULong(), P64_1, P64_2, P64_3, P64_4, P32_2.toULong(), P64_5, P32_1.toULong(),
)

/** XXH3_accumulate_512_scalar over one 64-byte stripe. */
internal fun accumulate512(acc: ULongArray, input: ByteArray, inOff: Int, secret: ByteArray, secOff: Int) {
    for (lane in 0 until ACC_NB) {
        val dataVal = readLE64(input, inOff + lane * 8)
        val dataKey = dataVal xor readLE64(secret, secOff + lane * 8)
        acc[lane xor 1] += dataVal
        acc[lane] += (dataKey and 0xffffffffuL) * (dataKey shr 32)
    }
}

/** XXH3_scrambleAcc_scalar. */
internal fun scrambleAcc(acc: ULongArray, secret: ByteArray, secOff: Int) {
    for (lane in 0 until ACC_NB) {
        val key64 = readLE64(secret, secOff + lane * 8)
        var a = acc[lane]
        a = a xor (a shr 47)
        a = a xor key64
        a *= P32_1.toULong()
        acc[lane] = a
    }
}

internal fun mix2Accs(acc: ULongArray, accOff: Int, secret: ByteArray, secOff: Int): ULong =
    mul128Fold64(acc[accOff] xor readLE64(secret, secOff), acc[accOff + 1] xor readLE64(secret, secOff + 8))

internal fun mergeAccs(acc: ULongArray, secret: ByteArray, secOff: Int, start: ULong): ULong {
    var result = start
    for (i in 0 until 4) {
        result += mix2Accs(acc, 2 * i, secret, secOff + 16 * i)
    }
    return xxh3Avalanche(result)
}

/** XXH3_hashLong_internal_loop (one-shot long path). */
internal fun hashLongLoop(acc: ULongArray, input: ByteArray, off: Int, len: Int, secret: ByteArray, secretSize: Int) {
    val nbStripesPerBlock = (secretSize - STRIPE_LEN) / SECRET_CONSUME_RATE
    val blockLen = STRIPE_LEN * nbStripesPerBlock
    val nbBlocks = (len - 1) / blockLen
    for (n in 0 until nbBlocks) {
        val base = off + n * blockLen
        for (s in 0 until nbStripesPerBlock) {
            accumulate512(acc, input, base + s * STRIPE_LEN, secret, s * SECRET_CONSUME_RATE)
        }
        scrambleAcc(acc, secret, secretSize - STRIPE_LEN)
    }
    val nbStripes = ((len - 1) - blockLen * nbBlocks) / STRIPE_LEN
    val base = off + nbBlocks * blockLen
    for (s in 0 until nbStripes) {
        accumulate512(acc, input, base + s * STRIPE_LEN, secret, s * SECRET_CONSUME_RATE)
    }
    // last stripe
    accumulate512(acc, input, off + len - STRIPE_LEN, secret, secretSize - STRIPE_LEN - SECRET_LASTACC_START)
}
