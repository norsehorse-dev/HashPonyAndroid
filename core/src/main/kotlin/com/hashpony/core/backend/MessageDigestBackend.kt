package com.hashpony.core.backend

import com.hashpony.core.StreamingDigest
import java.security.MessageDigest

/**
 * Adapts a platform [MessageDigest] (BoringSSL-backed on Android: MD5, SHA-1,
 * and the SHA-2 family) to [StreamingDigest].
 */
class MessageDigestBackend(private val md: MessageDigest) : StreamingDigest {
    override val outputBytes: Int = md.digestLength

    override fun update(input: ByteArray, offset: Int, length: Int) = md.update(input, offset, length)

    override fun digest(): ByteArray = md.digest() // finalises and resets

    override fun reset() = md.reset()

    companion object {
        fun of(algorithm: String): MessageDigestBackend = MessageDigestBackend(MessageDigest.getInstance(algorithm))
    }
}
