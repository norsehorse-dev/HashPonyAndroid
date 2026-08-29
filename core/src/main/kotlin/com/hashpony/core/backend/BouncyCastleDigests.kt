package com.hashpony.core.backend

import com.hashpony.core.StreamingDigest
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.digests.Blake2sDigest
import org.bouncycastle.crypto.digests.Blake3Digest
import org.bouncycastle.crypto.digests.GOST3411_2012_256Digest
import org.bouncycastle.crypto.digests.GOST3411_2012_512Digest
import org.bouncycastle.crypto.digests.KeccakDigest
import org.bouncycastle.crypto.digests.MD2Digest
import org.bouncycastle.crypto.digests.MD4Digest
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.crypto.digests.SHA3Digest
import org.bouncycastle.crypto.digests.SM3Digest
import org.bouncycastle.crypto.digests.WhirlpoolDigest

/**
 * Factory shims for the BouncyCastle-backed digests. Kept in one file so the
 * single bcprov dependency has exactly one point of contact.
 *
 * Note the differing constructor unit conventions in BC: SHA3/Keccak/BLAKE2
 * take a bit length, BLAKE3 takes a byte length. The :core vector tests confirm
 * each one against published reference values.
 */
object BouncyCastleDigests {
    fun sha3(bits: Int): StreamingDigest = BcDigestBackend(SHA3Digest(bits))

    /** Ethereum-style Keccak padding (0x01), distinct from SHA3's 0x06. */
    fun keccak256(): StreamingDigest = BcDigestBackend(KeccakDigest(256))

    fun blake2b(bits: Int): StreamingDigest = BcDigestBackend(Blake2bDigest(bits))
    fun blake2s(bits: Int): StreamingDigest = BcDigestBackend(Blake2sDigest(bits))
    fun blake3(bytes: Int = 32): StreamingDigest = BcDigestBackend(Blake3Digest(bytes))

    fun md4(): StreamingDigest = BcDigestBackend(MD4Digest())
    fun md2(): StreamingDigest = BcDigestBackend(MD2Digest())
    fun ripemd160(): StreamingDigest = BcDigestBackend(RIPEMD160Digest())
    fun whirlpool(): StreamingDigest = BcDigestBackend(WhirlpoolDigest())

    fun sm3(): StreamingDigest = BcDigestBackend(SM3Digest())
    fun streebog256(): StreamingDigest = BcDigestBackend(GOST3411_2012_256Digest())
    fun streebog512(): StreamingDigest = BcDigestBackend(GOST3411_2012_512Digest())
}
