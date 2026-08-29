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

object HashComparer {
    fun compare(expectedRaw: String, results: Map<String, ByteArray>): CompareOutcome {
        val expected = InputNormalizer.normalizeExpectedHash(expectedRaw)
        if (expected.isEmpty()) return CompareOutcome(expected, emptyList())
        val matched = results.entries
            .filter { Hex.encode(it.value).equals(expected, ignoreCase = true) }
            .map { it.key }
        return CompareOutcome(expected, matched)
    }
}
