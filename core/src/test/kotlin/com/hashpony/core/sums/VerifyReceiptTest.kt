package com.hashpony.core.sums

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifyReceiptTest {

    private val labels = VerifyReceipt.Labels(
        title = "HashPony verify receipt",
        sumsFile = "Sums file",
        checked = "Checked",
        result = "Result",
        verdict = "Verification found issues",
        counts = "OK 1 · Failed 1 · Missing 1",
        footer = "HashPony 1.0.0",
    )

    @Test
    fun receiptKeepsFileOrderAndFixedTokens() {
        val report = VerifyReport(
            listOf(
                EntryResult("a.iso", "sha256", "SHA-256", "aa", "aa", EntryStatus.OK),
                EntryResult("b.bin", "sha256", "SHA-256", "bb", "cc", EntryStatus.FAILED),
                EntryResult("sub/c.txt", "sha256", "SHA-256", "dd", null, EntryStatus.MISSING),
                EntryResult("d", null, null, "ee", null, EntryStatus.UNSUPPORTED),
            ),
        )
        val expected = """
            # HashPony verify receipt
            # Sums file: SHA256SUMS
            # Checked: 2026-10-24T20:31:05-05:00
            # Result: Verification found issues
            # OK 1 · Failed 1 · Missing 1

            a.iso: OK
                SHA-256  aa
            b.bin: FAILED
                SHA-256 expected  bb
                SHA-256 computed  cc
            sub/c.txt: MISSING
                SHA-256 expected  dd
            d: UNKNOWN ALGORITHM
                ? expected  ee

            # HashPony 1.0.0

        """.trimIndent()
        assertEquals(expected, VerifyReceipt.build("SHA256SUMS", report, "2026-10-24T20:31:05-05:00", labels))
    }

    @Test
    fun namesStayOnOneLine() {
        val report = VerifyReport(listOf(EntryResult("x\ny", "md5", "MD5", "00", "00", EntryStatus.OK)))
        val text = VerifyReceipt.build("MD5SUMS", report, "t", labels)
        assertTrue(text.contains("x\\ny: OK\n"))
    }
}
