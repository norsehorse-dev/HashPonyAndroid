package com.hashpony.core.sums

/** How the source lines were laid out. Used for reporting and to seed defaults. */
enum class SumsFormat { GNU, BSD_TAG, OPENSSL, XXHSUM_TAG, SFV, PLAIN, MIXED, UNKNOWN }

/**
 * One reference from a sums file: a filename and its expected digest. The
 * algorithm is only known here when the line carried it explicitly (a BSD/xxhsum
 * tag, an OpenSSL prefix, or an SFV entry, which is always CRC32). Otherwise it
 * is resolved later by [AlgorithmDetection].
 */
data class SumsEntry(
    val name: String,
    val expectedHex: String,
    val explicitAlgorithmId: String? = null,
    val binary: Boolean = false,
    val lineNumber: Int = 0,
)

/** Result of parsing a sums file. */
data class ParsedSums(
    val entries: List<SumsEntry>,
    val format: SumsFormat,
    val ignoredLines: Int,
)
