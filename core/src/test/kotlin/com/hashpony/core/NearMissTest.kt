package com.hashpony.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NearMissTest {

    private fun bytes(hex: String) = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    @Test
    fun picksSameLengthDigestWithFewestDifferences() {
        val results = linkedMapOf(
            "md5" to bytes("00112233445566778899aabbccddeeff"),
            "sha256" to bytes("0".repeat(64)),
            "sha3-256" to bytes("1".repeat(64)),
        )
        val expected = "0".repeat(60) + "abcd"
        val miss = HashComparer.nearest(expected, results)
        assertEquals(NearMiss("sha256", 4, 64), miss)
    }

    @Test
    fun nullWhenNoLengthMatches() {
        val results = mapOf("md5" to bytes("00112233445566778899aabbccddeeff"))
        assertNull(HashComparer.nearest("abcd", results))
        assertNull(HashComparer.nearest("", results))
    }
}
