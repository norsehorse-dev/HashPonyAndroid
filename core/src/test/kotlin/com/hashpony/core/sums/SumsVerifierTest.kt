package com.hashpony.core.sums

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class SumsVerifierTest {

    private val SHA_HELLO = "a948904f2f0f479b8f8197694b30184b0d2ed1c1cd2a1ec0fb85d299a192a447"
    private val CRC_HELLO = "af083b2d"

    private fun resolver(files: Map<String, ByteArray>) = FileResolver { name ->
        files[name]?.let { ByteArrayInputStream(it) }
    }

    @Test
    fun okFailedMissingAgainstRealFiles() {
        val files = mapOf(
            "hello.txt" to "hello world\n".toByteArray(),
            "other.txt" to "whatever".toByteArray(),
        )
        val text = buildString {
            append("$SHA_HELLO  hello.txt\n")            // OK
            append("${"0".repeat(64)}  other.txt\n")     // present but wrong -> FAILED
            append("${"1".repeat(64)}  gone.txt\n")      // absent -> MISSING
        }
        val parsed = SumsParser.parse(text, "SHA256SUMS")
        val report = SumsVerifier.verify(parsed, "SHA256SUMS", setOf("sha256"), resolver(files))

        assertEquals(1, report.ok)
        assertEquals(1, report.failed)
        assertEquals(1, report.missing)
        assertFalse(report.allOk)
        assertEquals(EntryStatus.FAILED, report.sortedForDisplay().first().status) // failures pinned first

        val hello = report.results.first { it.name == "hello.txt" }
        assertEquals(EntryStatus.OK, hello.status)
        assertEquals(SHA_HELLO, hello.computedHex)
    }

    @Test
    fun sfvCrc32Verifies() {
        val files = mapOf("hello.txt" to "hello world\n".toByteArray())
        val parsed = SumsParser.parse("hello.txt ${CRC_HELLO.uppercase()}\n", "files.sfv")
        val report = SumsVerifier.verify(parsed, "files.sfv", setOf("sha256"), resolver(files))
        assertTrue(report.allOk)
        assertEquals("crc32", report.results[0].algorithmId)
        assertEquals(CRC_HELLO, report.results[0].computedHex)
    }

    @Test
    fun ambiguousAlgorithmUsesPicker() {
        val files = mapOf("hello.txt" to "hello world\n".toByteArray())
        // Plain entry, no filename hint, two enabled 64-hex algorithms -> ambiguous.
        val parsed = SumsParser.parse("$SHA_HELLO  hello.txt\n")
        var askedCandidates: List<String>? = null
        val report = SumsVerifier.verify(
            parsed, sourceFileName = null, enabled = setOf("sha256", "sha3-256"),
            resolver = resolver(files),
            chooseAlgorithm = { _, candidates -> askedCandidates = candidates; "sha256" },
        )
        assertTrue(askedCandidates!!.contains("sha256") && askedCandidates!!.contains("sha3-256"))
        assertTrue(report.allOk)
    }

    @Test
    fun ambiguousWithoutPickerIsUnsupported() {
        val files = mapOf("hello.txt" to "hello world\n".toByteArray())
        val parsed = SumsParser.parse("$SHA_HELLO  hello.txt\n")
        val report = SumsVerifier.verify(parsed, null, setOf("sha256", "sha3-256"), resolver(files))
        assertEquals(1, report.unsupported)
    }

    @Test
    fun detectionOrder() {
        // explicit tag wins
        assertEquals(
            Detection.Resolved("sha512"),
            AlgorithmDetection.detect(SumsEntry("f", "ab".repeat(64), "sha512"), null, emptySet()),
        )
        // filename convention
        assertEquals(
            Detection.Resolved("sha256"),
            AlgorithmDetection.detect(SumsEntry("f", "0".repeat(64), null), "SHA256SUMS", emptySet()),
        )
        // single enabled length match auto-picks
        assertEquals(
            Detection.Resolved("crc32"),
            AlgorithmDetection.detect(SumsEntry("f", "af083b2d", null), null, setOf("crc32")),
        )
        // multiple enabled -> ambiguous
        val d = AlgorithmDetection.detect(SumsEntry("f", "0".repeat(64), null), null, setOf("sha256", "sha3-256"))
        assertTrue(d is Detection.Ambiguous && d.candidates.containsAll(listOf("sha256", "sha3-256")))
    }
}
