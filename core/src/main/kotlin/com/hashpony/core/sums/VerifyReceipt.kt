package com.hashpony.core.sums

/**
 * Plain-text record of one verify run, for sharing or saving. The header lines
 * are localized (passed in as [Labels]) and commented with `#`; the per-file
 * lines follow `sha256sum -c` style (`name: OK`) with fixed English status
 * tokens so a receipt reads the same to anyone, plus the digests involved.
 * Entries keep sums-file order, not the failures-first display order.
 */
object VerifyReceipt {

    data class Labels(
        val title: String,
        val sumsFile: String,
        val checked: String,
        val result: String,
        /** Already-formatted verdict, e.g. "All 12 files verified". */
        val verdict: String,
        /** Already-formatted counts line, e.g. "OK 12 · Failed 0 · Missing 0". */
        val counts: String,
        /** Closing line, e.g. "HashPony 1.0.0". */
        val footer: String,
    )

    fun build(sumsName: String, report: VerifyReport, checkedAt: String, labels: Labels): String = buildString {
        append("# ").append(labels.title).append('\n')
        append("# ").append(labels.sumsFile).append(": ").append(oneLine(sumsName)).append('\n')
        append("# ").append(labels.checked).append(": ").append(checkedAt).append('\n')
        append("# ").append(labels.result).append(": ").append(labels.verdict).append('\n')
        append("# ").append(labels.counts).append('\n')
        append('\n')
        for (e in report.results) {
            val name = oneLine(e.name).ifEmpty { "-" }
            val algo = e.algorithmName ?: "?"
            append(name).append(": ").append(token(e.status)).append('\n')
            when (e.status) {
                EntryStatus.OK ->
                    append("    ").append(algo).append("  ").append(e.computedHex ?: e.expectedHex).append('\n')
                EntryStatus.FAILED -> {
                    append("    ").append(algo).append(" expected  ").append(e.expectedHex).append('\n')
                    append("    ").append(algo).append(" computed  ").append(e.computedHex ?: "-").append('\n')
                }
                EntryStatus.MISSING, EntryStatus.UNSUPPORTED ->
                    append("    ").append(algo).append(" expected  ").append(e.expectedHex).append('\n')
            }
        }
        append('\n')
        append("# ").append(labels.footer).append('\n')
    }

    /** Stable status words, deliberately not localized (they mirror coreutils). */
    fun token(status: EntryStatus): String = when (status) {
        EntryStatus.OK -> "OK"
        EntryStatus.FAILED -> "FAILED"
        EntryStatus.MISSING -> "MISSING"
        EntryStatus.UNSUPPORTED -> "UNKNOWN ALGORITHM"
    }

    /** Names are user data; keep each on one line so the receipt stays parseable. */
    private fun oneLine(s: String): String = s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r")
}
