package com.hashpony.core

/**
 * Result of comparing a pasted expected value against a set of computed digests.
 * [matched] lists the algorithm ids whose digest equals the expected value. The
 * compare field never guesses an algorithm; it checks against everything
 * computed, so overlapping output lengths cost nothing.
 */
data class CompareOutcome(val normalizedExpected: String, val matched: List<String>) {
    val isMatch: Boolean get() = matched.isNotEmpty()
}

/**
 * The computed digest closest to a non-matching expected value: same length,
 * fewest differing hex characters. Lets the UI point at exactly where a pasted
 * value diverges instead of a bare "no match".
 */
data class NearMiss(val algorithmId: String, val differing: Int, val length: Int)

object HashComparer {
    fun compare(expectedRaw: String, results: Map<String, ByteArray>): CompareOutcome {
        val expected = InputNormalizer.normalizeExpectedHash(expectedRaw)
        if (expected.isEmpty()) return CompareOutcome(expected, emptyList())
        val matched = results.entries
            .filter { Hex.encode(it.value).equals(expected, ignoreCase = true) }
            .map { it.key }
        return CompareOutcome(expected, matched)
    }

    /**
     * Among [results], the same-length digest with the fewest positions differing
     * from [expectedNormalized] (already passed through [InputNormalizer]). Null
     * when nothing computed has that length. Ties keep [results] order.
     */
    fun nearest(expectedNormalized: String, results: Map<String, ByteArray>): NearMiss? {
        if (expectedNormalized.isEmpty()) return null
        var best: NearMiss? = null
        for ((id, bytes) in results) {
            val hex = Hex.encode(bytes)
            if (hex.length != expectedNormalized.length) continue
            var differing = 0
            for (i in hex.indices) if (hex[i] != expectedNormalized[i]) differing++
            if (best == null || differing < best.differing) best = NearMiss(id, differing, hex.length)
        }
        return best
    }
}
