package com.hashpony.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the BouncyCastle-backed algorithms — routed through the registry, so
 * both the wiring and the constructor unit conventions are covered — against
 * published reference values (SHA-3, BLAKE2/3, Keccak-256, RIPEMD-160, MD2/MD4,
 * SM3, Whirlpool, Streebog). Runs on the JVM with bcprov on the test classpath.
 */
class BcAlgorithmVectorTest {

    @Test
    fun bcVectorsMatch() {
        val stream = requireNotNull(javaClass.getResourceAsStream("/bc_vectors.tsv")) {
            "missing test resource bc_vectors.tsv"
        }
        var checked = 0
        stream.bufferedReader().useLines { lines ->
            for (line in lines) {
                if (line.isBlank()) continue
                val p = line.split("|")
                val id = p[0]; val label = p[1]; val data = Hex.decode(p[2]); val expected = p[3]
                val algorithm = AlgorithmRegistry.byId(id)
                    ?: throw AssertionError("registry has no algorithm id \"$id\"")
                val digest = algorithm.newDigest()
                digest.update(data, 0, data.size)
                assertEquals("$id ($label)", expected, Hex.encode(digest.digest()))
                checked++
            }
        }
        assertTrue("expected BC vectors, ran $checked", checked > 30)
    }
}
