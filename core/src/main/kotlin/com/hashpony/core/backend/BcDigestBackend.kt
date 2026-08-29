package com.hashpony.core.backend

import com.hashpony.core.StreamingDigest
import org.bouncycastle.crypto.Digest

/**
 * Adapts a BouncyCastle lightweight [Digest] to [StreamingDigest].
 *
 * We deliberately use BC's own digest classes rather than the JCA provider:
 * Android bundles a stripped, name-clashing copy of BC, and going through
 * `MessageDigest.getInstance(name, "BC")` invites provider conflicts. The
 * lightweight API sidesteps all of that. One bcprov dependency covers every
 * cryptographic digest outside the SHA-2 family.
 */
class BcDigestBackend(private val digest: Digest) : StreamingDigest {
    override val outputBytes: Int = digest.digestSize

    override fun update(input: ByteArray, offset: Int, length: Int) = digest.update(input, offset, length)

    override fun digest(): ByteArray {
        val out = ByteArray(digest.digestSize)
        digest.doFinal(out, 0) // doFinal resets the digest
        return out
    }

    override fun reset() = digest.reset()
}
