package com.hashpony.core

import java.io.InputStream

/** Thrown by [HashEngine] when a caller-supplied cancellation predicate trips. */
class HashCancelledException : RuntimeException("Hashing cancelled")

/**
 * Single-pass, multi-digest hashing.
 *
 * One read loop feeds every enabled [StreamingDigest], so hashing a 4 GiB file
 * with six algorithms costs one read of the file, not six. With hardware SHA-2
 * the read, not the hash, is the bottleneck.
 */
object HashEngine {
    const val DEFAULT_BUFFER_SIZE = 1 shl 20 // 1 MiB

    /**
     * Hash [input] with every digest in [digests], keyed by algorithm id.
     *
     * @param onProgress invoked with the running byte count after each block.
     * @param isCancelled polled once per block; a true result aborts with
     *   [HashCancelledException] before any digest is finalised.
     * @return raw digest bytes per algorithm id.
     */
    fun hash(
        input: InputStream,
        digests: Map<String, StreamingDigest>,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        onProgress: ((Long) -> Unit)? = null,
        isCancelled: (() -> Boolean)? = null,
    ): Map<String, ByteArray> {
        val values = digests.values.toTypedArray()
        val buffer = ByteArray(bufferSize)
        var total = 0L
        while (true) {
            if (isCancelled?.invoke() == true) throw HashCancelledException()
            val n = input.read(buffer)
            if (n < 0) break
            if (n > 0) {
                for (d in values) d.update(buffer, 0, n)
                total += n
                onProgress?.invoke(total)
            }
        }
        return digests.mapValues { (_, d) -> d.digest() }
    }

    /** Convenience for in-memory data (the Text tab, small inputs). */
    fun hashBytes(data: ByteArray, digests: Map<String, StreamingDigest>): Map<String, ByteArray> {
        for (d in digests.values) d.update(data, 0, data.size)
        return digests.mapValues { (_, d) -> d.digest() }
    }
}
