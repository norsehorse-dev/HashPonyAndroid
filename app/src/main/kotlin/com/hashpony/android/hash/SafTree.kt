package com.hashpony.android.hash

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.InputStream

/** A sums file discovered inside a granted tree. */
data class SumsCandidate(val name: String, val relativePath: String, val uri: Uri)

/** Any file discovered inside a granted tree, with its tree-relative path. */
data class IndexedFile(val relativePath: String, val uri: Uri, val size: Long?)

/**
 * An index of a granted document tree (from OpenDocumentTree), mapping each
 * file's tree-relative path to its document Uri. Built with one batched
 * DocumentsContract child query per directory rather than the DocumentFile
 * wrapper, which does a provider round-trip per node and crawls on big trees.
 */
class SafTree private constructor(
    private val resolver: ContentResolver,
    private val filesByPath: Map<String, Uri>,
    val files: List<IndexedFile>,
    val sumsCandidates: List<SumsCandidate>,
) {
    /** Open a referenced file by its `subdir/name` relative path, or basename fallback. */
    fun open(relativeName: String): InputStream? {
        val norm = relativeName.replace('\\', '/').trimStart('/')
        val uri = filesByPath[norm]
            ?: filesByPath.entries.firstOrNull { it.key.substringAfterLast('/') == norm.substringAfterLast('/') }?.value
        return uri?.let { runCatching { resolver.openInputStream(it) }.getOrNull() }
    }

    companion object {
        private const val DIR_MIME = DocumentsContract.Document.MIME_TYPE_DIR
        private const val MAX_NODES = 50_000

        fun index(resolver: ContentResolver, treeUri: Uri): SafTree {
            val byPath = LinkedHashMap<String, Uri>()
            val all = ArrayList<IndexedFile>()
            val candidates = ArrayList<SumsCandidate>()
            val rootId = DocumentsContract.getTreeDocumentId(treeUri)

            val queue = ArrayDeque<Pair<String, String>>() // (documentId, prefix)
            queue.add(rootId to "")
            var nodes = 0

            while (queue.isNotEmpty() && nodes < MAX_NODES) {
                val (parentId, prefix) = queue.removeFirst()
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
                resolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE,
                    ),
                    null, null, null,
                )?.use { c ->
                    val idIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    while (c.moveToNext()) {
                        nodes++
                        val docId = c.getString(idIdx)
                        val name = c.getString(nameIdx) ?: continue
                        val mime = c.getString(mimeIdx)
                        val path = if (prefix.isEmpty()) name else "$prefix$name"
                        if (mime == DIR_MIME) {
                            queue.add(docId to "$path/")
                        } else {
                            val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val size = if (sizeIdx >= 0 && !c.isNull(sizeIdx)) c.getLong(sizeIdx) else null
                            byPath[path] = uri
                            all.add(IndexedFile(path, uri, size))
                            if (isSumsFileName(name)) candidates.add(SumsCandidate(name, path, uri))
                        }
                    }
                }
            }
            return SafTree(resolver, byPath, all, candidates)
        }

        /** Recognise the common sums-file names, case-insensitively. */
        fun isSumsFileName(name: String): Boolean {
            val n = name.lowercase()
            if (n.endsWith("sums")) return true // SHA256SUMS, MD5SUMS, B2SUMS, ...
            val ext = n.substringAfterLast('.', "")
            return ext in setOf(
                "sha256", "sha512", "sha384", "sha224", "sha1", "md5", "b2", "sfv",
                "xxh32", "xxh64", "xxh128", "xxh3",
            )
        }
    }
}
