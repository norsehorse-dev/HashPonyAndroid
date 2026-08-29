package com.hashpony.core

/**
 * A single incremental digest.
 *
 * The whole engine is built on this one abstraction so that platform
 * [java.security.MessageDigest], BouncyCastle digests, [java.util.zip.Checksum]
 * checksums, and the vendored xxHash port can all be driven by one read loop.
 *
 * Contract: feed bytes with [update] (possibly many times), then call [digest]
 * exactly once to finalise. After [digest] the instance is reset and may be
 * reused from a clean state, identical to calling [reset].
 */
interface StreamingDigest {
    /** Length of the finalised digest in bytes. */
    val outputBytes: Int

    /** Absorb [length] bytes of [input] starting at [offset]. */
    fun update(input: ByteArray, offset: Int, length: Int)

    /** Finalise and return the raw digest bytes. Resets the instance for reuse. */
    fun digest(): ByteArray

    /** Discard buffered state and return to the initial condition. */
    fun reset()
}
