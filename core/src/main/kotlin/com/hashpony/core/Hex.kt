package com.hashpony.core

/** Hex encode/decode. Lowercase by default; uppercase is a display toggle. */
object Hex {
    private val LOWER = "0123456789abcdef".toCharArray()
    private val UPPER = "0123456789ABCDEF".toCharArray()

    fun encode(bytes: ByteArray, upperCase: Boolean = false): String {
        val table = if (upperCase) UPPER else LOWER
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xff
            out[i * 2] = table[v ushr 4]
            out[i * 2 + 1] = table[v and 0x0f]
        }
        return String(out)
    }

    fun decode(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string must have an even length" }
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(hex[i * 2], 16)
            val lo = Character.digit(hex[i * 2 + 1], 16)
            require(hi >= 0 && lo >= 0) { "Invalid hex character in \"$hex\"" }
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }
}
