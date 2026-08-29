package com.hashpony.core

/**
 * Normalises a pasted "expected hash" for comparison.
 *
 * Real-world sources vary wildly: a website table, `certutil -hashfile` output
 * (space-separated byte pairs), a colon-separated fingerprint, a value with a
 * `0x` prefix, mixed case. All of those must compare equal to a plain lowercase
 * hex digest. The rule is deliberately simple and matches the product spec:
 * drop a leading `0x`, keep only hex nibbles, fold to lowercase.
 */
object InputNormalizer {
    fun normalizeExpectedHash(raw: String): String {
        var s = raw.trim()
        if (s.length >= 2 && s[0] == '0' && (s[1] == 'x' || s[1] == 'X')) {
            s = s.substring(2)
        }
        val sb = StringBuilder(s.length)
        for (c in s) {
            when (c) {
                in '0'..'9', in 'a'..'f' -> sb.append(c)
                in 'A'..'F' -> sb.append(c + 32) // to lowercase
                else -> {} // whitespace, ':', '-', separators: dropped
            }
        }
        return sb.toString()
    }
}
