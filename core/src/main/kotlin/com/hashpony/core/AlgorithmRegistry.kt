package com.hashpony.core

import com.hashpony.core.backend.BouncyCastleDigests as Bc
import com.hashpony.core.backend.ChecksumBackend
import com.hashpony.core.backend.MessageDigestBackend
import com.hashpony.core.xxhash.Xxh32Digest
import com.hashpony.core.xxhash.Xxh3Digest
import com.hashpony.core.xxhash.Xxh64Digest
import java.util.zip.Adler32
import java.util.zip.CRC32

/**
 * The 1.0 "kitchen sink" algorithm set, grouped exactly as Settings presents it.
 * Everything outside the defaults is off until the user enables it, so the Files
 * tab stays clean regardless of how much is available.
 *
 * Platform [MessageDigestBackend] covers MD5, SHA-1 and the SHA-2 family
 * (BoringSSL-backed, hardware-accelerated). One BouncyCastle dependency covers
 * every other cryptographic digest. Checksums come from java.util.zip; xxHash is
 * the vendored pure-Kotlin port.
 */
object AlgorithmRegistry {

    val all: List<Algorithm> = buildList {
        // ---- Modern ----
        add(md("sha224", "SHA-224", AlgorithmGroup.MODERN, 224, "SHA-224"))
        add(md("sha256", "SHA-256", AlgorithmGroup.MODERN, 256, "SHA-256", enabledByDefault = true))
        add(md("sha384", "SHA-384", AlgorithmGroup.MODERN, 384, "SHA-384"))
        add(md("sha512", "SHA-512", AlgorithmGroup.MODERN, 512, "SHA-512", quickAccess = true))
        add(bc("sha3-224", "SHA3-224", AlgorithmGroup.MODERN, 224) { Bc.sha3(224) })
        add(bc("sha3-256", "SHA3-256", AlgorithmGroup.MODERN, 256) { Bc.sha3(256) })
        add(bc("sha3-384", "SHA3-384", AlgorithmGroup.MODERN, 384) { Bc.sha3(384) })
        add(bc("sha3-512", "SHA3-512", AlgorithmGroup.MODERN, 512) { Bc.sha3(512) })
        add(bc("blake2b-256", "BLAKE2b-256", AlgorithmGroup.MODERN, 256) { Bc.blake2b(256) })
        add(bc("blake2b-512", "BLAKE2b-512", AlgorithmGroup.MODERN, 512) { Bc.blake2b(512) })
        add(bc("blake2s-256", "BLAKE2s-256", AlgorithmGroup.MODERN, 256) { Bc.blake2s(256) })
        add(bc("blake3-256", "BLAKE3-256", AlgorithmGroup.MODERN, 256) { Bc.blake3(32) })
        add(
            bc(
                "keccak-256", "Keccak-256", AlgorithmGroup.MODERN, 256,
                note = "Ethereum-style padding. Not the same as SHA3-256.",
            ) { Bc.keccak256() },
        )

        // ---- Legacy ----
        add(md("md5", "MD5", AlgorithmGroup.LEGACY, 128, "MD5", quickAccess = true))
        add(md("sha1", "SHA-1", AlgorithmGroup.LEGACY, 160, "SHA-1", quickAccess = true))
        add(bc("md4", "MD4", AlgorithmGroup.LEGACY, 128) { Bc.md4() })
        add(bc("md2", "MD2", AlgorithmGroup.LEGACY, 128) { Bc.md2() })
        add(bc("ripemd160", "RIPEMD-160", AlgorithmGroup.LEGACY, 160) { Bc.ripemd160() })
        add(bc("whirlpool", "Whirlpool", AlgorithmGroup.LEGACY, 512) { Bc.whirlpool() })

        // ---- Regional ----
        add(bc("sm3", "SM3", AlgorithmGroup.REGIONAL, 256, note = "GB/T 32905") { Bc.sm3() })
        add(bc("streebog-256", "Streebog-256", AlgorithmGroup.REGIONAL, 256, note = "GOST R 34.11-2012") { Bc.streebog256() })
        add(bc("streebog-512", "Streebog-512", AlgorithmGroup.REGIONAL, 512, note = "GOST R 34.11-2012") { Bc.streebog512() })

        // ---- Checksums and fast hashes (non-cryptographic) ----
        add(checksum("crc32", "CRC32", 32) { ChecksumBackend(CRC32()) })
        add(checksum("adler32", "Adler-32", 32) { ChecksumBackend(Adler32()) })
        add(checksum("xxh32", "XXH32", 32) { Xxh32Digest() })
        add(checksum("xxh64", "XXH64", 64) { Xxh64Digest() })
        add(checksum("xxh3-64", "XXH3-64", 64) { Xxh3Digest(64) })
        add(checksum("xxh3-128", "XXH3-128", 128) { Xxh3Digest(128) })
    }

    private val byId: Map<String, Algorithm> = all.associateBy { it.id }

    fun byId(id: String): Algorithm? = byId[id]

    fun byGroup(group: AlgorithmGroup): List<Algorithm> = all.filter { it.group == group }

    val defaults: List<Algorithm> get() = all.filter { it.enabledByDefault }

    /** Algorithms whose digest is [hexLen] hex chars long — for length-based detection. */
    fun byHexLength(hexLen: Int): List<Algorithm> = all.filter { it.hexLength == hexLen }

    // --- small builders to keep the table above readable ---

    private fun md(
        id: String,
        name: String,
        group: AlgorithmGroup,
        bits: Int,
        jca: String,
        enabledByDefault: Boolean = false,
        quickAccess: Boolean = false,
    ) = Algorithm(id, name, group, bits, enabledByDefault, quickAccess) { MessageDigestBackend.of(jca) }

    private fun bc(
        id: String,
        name: String,
        group: AlgorithmGroup,
        bits: Int,
        note: String? = null,
        factory: () -> StreamingDigest,
    ) = Algorithm(id, name, group, bits, note = note, factory = factory)

    private fun checksum(
        id: String,
        name: String,
        bits: Int,
        factory: () -> StreamingDigest,
    ) = Algorithm(id, name, AlgorithmGroup.CHECKSUM, bits, nonCryptographic = true, factory = factory)
}
