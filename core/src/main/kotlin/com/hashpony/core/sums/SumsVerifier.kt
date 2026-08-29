package com.hashpony.core.sums

import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.HashEngine
import com.hashpony.core.Hex
import java.io.InputStream

enum class EntryStatus { OK, FAILED, MISSING, UNSUPPORTED }

data class EntryResult(
    val name: String,
    val algorithmId: String?,
    val algorithmName: String?,
    val expectedHex: String,
    val computedHex: String?,
    val status: EntryStatus,
)

data class VerifyReport(val results: List<EntryResult>) {
    val ok: Int get() = results.count { it.status == EntryStatus.OK }
    val failed: Int get() = results.count { it.status == EntryStatus.FAILED }
    val missing: Int get() = results.count { it.status == EntryStatus.MISSING }
    val unsupported: Int get() = results.count { it.status == EntryStatus.UNSUPPORTED }
    val allOk: Boolean get() = results.isNotEmpty() && failed == 0 && missing == 0 && unsupported == 0

    /** Failures pinned first, then missing/unsupported, then OK, each in file order. */
    fun sortedForDisplay(): List<EntryResult> = results.sortedBy {
        when (it.status) {
            EntryStatus.FAILED -> 0
            EntryStatus.MISSING -> 1
            EntryStatus.UNSUPPORTED -> 2
            EntryStatus.OK -> 3
        }
    }
}

/** Opens a referenced file by its (possibly `subdir/name`) relative path; null = missing. */
fun interface FileResolver {
    fun open(relativeName: String): InputStream?
}

/**
 * Verifies parsed entries against the actual files. Entries only: files present
 * in the folder but absent from the sums file are not reported, matching
 * `sha256sum -c`. When an entry's algorithm is ambiguous, [chooseAlgorithm] is
 * consulted (the UI's picker); returning null marks the entry UNSUPPORTED.
 */
object SumsVerifier {

    fun verify(
        parsed: ParsedSums,
        sourceFileName: String?,
        enabled: Set<String>,
        resolver: FileResolver,
        chooseAlgorithm: ((SumsEntry, List<String>) -> String?)? = null,
    ): VerifyReport {
        val results = parsed.entries.map { entry ->
            val name = entry.name.ifEmpty { deriveTargetName(sourceFileName) ?: "" }

            val algorithmId = when (val d = AlgorithmDetection.detect(entry, sourceFileName, enabled)) {
                is Detection.Resolved -> d.algorithmId
                is Detection.Ambiguous -> chooseAlgorithm?.invoke(entry, d.candidates)
                Detection.None -> null
            }
            val algorithm = algorithmId?.let { AlgorithmRegistry.byId(it) }

            if (algorithm == null || name.isEmpty()) {
                return@map EntryResult(name, algorithmId, algorithm?.displayName, entry.expectedHex, null, EntryStatus.UNSUPPORTED)
            }

            val stream = resolver.open(name)
                ?: return@map EntryResult(name, algorithmId, algorithm.displayName, entry.expectedHex, null, EntryStatus.MISSING)

            val computed = stream.use {
                Hex.encode(HashEngine.hash(it, mapOf(algorithmId to algorithm.newDigest())).getValue(algorithmId))
            }
            val status = if (computed.equals(entry.expectedHex, ignoreCase = true)) EntryStatus.OK else EntryStatus.FAILED
            EntryResult(name, algorithmId, algorithm.displayName, entry.expectedHex, computed, status)
        }
        return VerifyReport(results)
    }

    /** foo.iso.sha256 → foo.iso, so a lone-hash file can name its target. */
    private fun deriveTargetName(sumsName: String?): String? {
        if (sumsName == null) return null
        val ext = sumsName.substringAfterLast('.', "").lowercase()
        val known = setOf("sha256", "sha512", "sha384", "sha224", "sha1", "md5", "b2", "sfv", "xxh32", "xxh64", "xxh128", "xxh3")
        return if (ext in known) sumsName.substring(0, sumsName.length - ext.length - 1) else null
    }
}
