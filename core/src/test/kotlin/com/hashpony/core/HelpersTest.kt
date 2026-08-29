package com.hashpony.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpersTest {

    @Test
    fun hexRoundTrip() {
        val bytes = byteArrayOf(0x00, 0x0f.toByte(), 0xa5.toByte(), 0xff.toByte())
        assertEquals("000fa5ff", Hex.encode(bytes))
        assertEquals("000FA5FF", Hex.encode(bytes, upperCase = true))
        assertTrue(bytes.contentEquals(Hex.decode("000fa5ff")))
        assertTrue(bytes.contentEquals(Hex.decode("000FA5FF")))
    }

    @Test
    fun normalizerStripsSeparatorsAndCase() {
        // CertUtil-style spaced byte pairs
        assertEquals("e3b0c44298fc", InputNormalizer.normalizeExpectedHash("E3 B0 C4 42 98 FC"))
        // colon-separated fingerprint
        assertEquals("ab12cd", InputNormalizer.normalizeExpectedHash("AB:12:CD"))
        // 0x prefix
        assertEquals("deadbeef", InputNormalizer.normalizeExpectedHash("0xDEADBEEF"))
        // surrounding whitespace
        assertEquals("cafef00d", InputNormalizer.normalizeExpectedHash("  cafe f00d \n"))
    }

    @Test
    fun comparerFindsMatchingAlgorithm() {
        val results = mapOf(
            "sha256" to Hex.decode("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
            "md5" to Hex.decode("900150983cd24fb0d6963f7d28e17f72"),
        )
        // SHA-256("abc"), pasted with noise
        val outcome = HashComparer.compare(
            "BA78:16BF 8F01CFEA 414140DE5DAE2223 B00361A3 96177A9C B410FF61 F20015AD",
            results,
        )
        assertTrue(outcome.isMatch)
        assertEquals(listOf("sha256"), outcome.matched)

        val noMatch = HashComparer.compare("0000", results)
        assertFalse(noMatch.isMatch)

        val empty = HashComparer.compare("", results)
        assertFalse(empty.isMatch)
    }

    @Test
    fun engineIsSinglePassMultiDigest() {
        val data = "The quick brown fox jumps over the lazy dog".toByteArray()
        val out = HashEngine.hashBytes(
            data,
            mapOf(
                "sha256" to AlgorithmRegistry.byId("sha256")!!.newDigest(),
                "crc32" to AlgorithmRegistry.byId("crc32")!!.newDigest(),
            ),
        )
        assertEquals(
            "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592",
            Hex.encode(out.getValue("sha256")),
        )
        assertEquals("414fa339", Hex.encode(out.getValue("crc32")))
    }
}
