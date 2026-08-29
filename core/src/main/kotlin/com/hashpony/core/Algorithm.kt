package com.hashpony.core

/** The four groups exactly as they appear in Settings. */
enum class AlgorithmGroup(val label: String) {
    MODERN("Modern"),
    LEGACY("Legacy"),
    REGIONAL("Regional"),
    CHECKSUM("Checksums and fast hashes"),
}

/**
 * One entry in the algorithm registry.
 *
 * @param id stable, lowercase key used for preferences, sums-file detection and
 *   result maps. Never localised, never changed once shipped.
 * @param displayName the label shown in the UI.
 * @param outputBits digest length in bits (also disambiguates by hex length).
 * @param enabledByDefault true only for SHA-256.
 * @param quickAccess shown as a one-tap toggle on the Files tab (SHA-512/1, MD5).
 * @param nonCryptographic labels the checksum/fast-hash group honestly.
 * @param note optional one-line UI explainer (e.g. Keccak vs SHA3).
 * @param factory builds a fresh [StreamingDigest].
 */
data class Algorithm(
    val id: String,
    val displayName: String,
    val group: AlgorithmGroup,
    val outputBits: Int,
    val enabledByDefault: Boolean = false,
    val quickAccess: Boolean = false,
    val nonCryptographic: Boolean = false,
    val note: String? = null,
    val factory: () -> StreamingDigest,
) {
    /** Digest length in hex characters — used by length-based sums detection. */
    val hexLength: Int get() = outputBits / 4

    fun newDigest(): StreamingDigest = factory()
}
