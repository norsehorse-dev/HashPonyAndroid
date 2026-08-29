package com.hashpony.core.sums

import com.hashpony.core.AlgorithmRegistry

/** Outcome of resolving which algorithm a sums entry was produced with. */
sealed interface Detection {
    data class Resolved(val algorithmId: String) : Detection
    data class Ambiguous(val candidates: List<String>) : Detection
    data object None : Detection
}

/**
 * Resolves the algorithm for an entry in the order the plan specifies:
 * explicit tag → filename convention (extension or SHA256SUMS-style name;
 * `.sfv` means CRC32) → hex length (auto only when exactly one enabled algorithm
 * matches) → otherwise ambiguous, so the UI can ask.
 */
object AlgorithmDetection {

    fun detect(entry: SumsEntry, sourceFileName: String?, enabled: Set<String>): Detection {
        entry.explicitAlgorithmId?.let { id ->
            if (AlgorithmRegistry.byId(id) != null) return Detection.Resolved(id)
        }
        fromFilename(sourceFileName)?.let { return Detection.Resolved(it) }

        val matches = AlgorithmRegistry.byHexLength(entry.expectedHex.length).map { it.id }
        if (matches.isEmpty()) return Detection.None
        val enabledMatches = matches.filter { it in enabled }
        if (enabledMatches.size == 1) return Detection.Resolved(enabledMatches.first())
        // Present enabled candidates first, then the rest of that length.
        return Detection.Ambiguous(enabledMatches + matches.filter { it !in enabled })
    }

    /** Algorithm implied by the sums file's own name, or null. */
    fun fromFilename(name: String?): String? {
        if (name == null) return null
        val n = name.lowercase()
        when (n.substringAfterLast('.', "")) {
            "sha256" -> return "sha256"
            "sha512" -> return "sha512"
            "sha384" -> return "sha384"
            "sha224" -> return "sha224"
            "sha1" -> return "sha1"
            "md5" -> return "md5"
            "b2" -> return "blake2b-512"
            "sfv" -> return "crc32"
            "xxh32" -> return "xxh32"
            "xxh64" -> return "xxh64"
            "xxh128" -> return "xxh3-128"
            "xxh3" -> return "xxh3-64"
        }
        return when {
            n.contains("sha256sums") -> "sha256"
            n.contains("sha512sums") -> "sha512"
            n.contains("sha1sums") -> "sha1"
            n.contains("md5sums") -> "md5"
            n.contains("b2sums") -> "blake2b-512"
            else -> null
        }
    }
}
