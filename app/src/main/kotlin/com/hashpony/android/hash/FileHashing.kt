package com.hashpony.android.hash

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.HashEngine
import com.hashpony.core.StreamingDigest

/** File name + size resolved from a content Uri. Size is null when unknown. */
data class FileMeta(val uri: Uri, val name: String, val size: Long?)

object FileHashing {

    /** Resolve display name and size for a SAF/content [uri]. */
    fun queryMeta(resolver: ContentResolver, uri: Uri): FileMeta {
        var name = uri.lastPathSegment ?: "file"
        var size: Long? = null
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && !c.isNull(nameIdx)) name = c.getString(nameIdx)
                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0 && !c.isNull(sizeIdx)) size = c.getLong(sizeIdx)
                }
            }
        }
        return FileMeta(uri, name, size)
    }

    /**
     * Hash [uri] with every algorithm id in [algorithmIds], in a single read pass.
     * Returns raw digest bytes keyed by algorithm id. The caller formats hex.
     */
    fun hash(
        resolver: ContentResolver,
        uri: Uri,
        algorithmIds: List<String>,
        onProgress: ((Long) -> Unit)? = null,
        isCancelled: (() -> Boolean)? = null,
    ): Map<String, ByteArray> {
        val digests: Map<String, StreamingDigest> = algorithmIds
            .mapNotNull { id -> AlgorithmRegistry.byId(id)?.let { id to it.newDigest() } }
            .toMap()
        if (digests.isEmpty()) return emptyMap()
        val input = resolver.openInputStream(uri)
            ?: throw java.io.IOException("Could not open $uri")
        return input.use { HashEngine.hash(it, digests, onProgress = onProgress, isCancelled = isCancelled) }
    }
}
