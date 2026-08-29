package com.hashpony.core.sums

/**
 * Maps the algorithm labels that appear in tagged sums files to registry ids.
 * The exact xxHash tag strings (XXH32/XXH64/XXH128/XXH3) were locked in from
 * real xxhsum output: note "XXH3" is the 64-bit XXH3 and "XXH128" is XXH3-128.
 * OpenSSL 3.x uses SHA2-256 etc.; older OpenSSL and BSD/shasum use SHA256.
 */
internal object SumsAlgorithmNames {

    private val map: Map<String, String> = buildMap {
        fun put(vararg labels: String, id: String) { labels.forEach { put(it.uppercase(), id) } }

        put("SHA1", "SHA-1", id = "sha1")
        put("SHA224", "SHA-224", "SHA2-224", id = "sha224")
        put("SHA256", "SHA-256", "SHA2-256", id = "sha256")
        put("SHA384", "SHA-384", "SHA2-384", id = "sha384")
        put("SHA512", "SHA-512", "SHA2-512", id = "sha512")
        put("SHA3-224", id = "sha3-224")
        put("SHA3-256", id = "sha3-256")
        put("SHA3-384", id = "sha3-384")
        put("SHA3-512", id = "sha3-512")
        put("MD5", id = "md5")
        put("MD4", id = "md4")
        put("MD2", id = "md2")
        put("RIPEMD160", "RIPEMD-160", "RMD160", id = "ripemd160")
        put("WHIRLPOOL", id = "whirlpool")
        put("SM3", id = "sm3")
        put("STREEBOG-256", "STREEBOG256", "MD_GOST12_256", id = "streebog-256")
        put("STREEBOG-512", "STREEBOG512", "MD_GOST12_512", id = "streebog-512")
        put("BLAKE2B", "BLAKE2B512", "BLAKE2B-512", id = "blake2b-512")
        put("BLAKE2B-256", id = "blake2b-256")
        put("BLAKE2S", "BLAKE2S256", "BLAKE2S-256", id = "blake2s-256")
        put("BLAKE3", "BLAKE3-256", id = "blake3-256")
        put("KECCAK-256", "KECCAK256", id = "keccak-256")
        // xxHash tag strings, exactly as xxhsum emits them
        put("XXH32", id = "xxh32")
        put("XXH64", id = "xxh64")
        put("XXH3", id = "xxh3-64")
        put("XXH128", id = "xxh3-128")
    }

    fun toId(tag: String): String? = map[tag.trim().uppercase()]
}
