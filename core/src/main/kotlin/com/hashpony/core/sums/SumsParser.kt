package com.hashpony.core.sums

/**
 * Parses the sums-file formats HashPony accepts (section 7 of the plan):
 * GNU coreutils (two-space and binary `*` marker, plus the `\`-escaped-filename
 * convention), BSD/`shasum --tag`, OpenSSL 1.x and 3.x, xxhsum (default GNU-style
 * and tagged), SFV (reversed `name crc32`), and plain single-hash files. Comment
 * lines (`#`, `;`), blank lines, CRLF endings, mixed-case hex and leading
 * progress padding are tolerated silently.
 */
object SumsParser {

    private val TAG = Regex("^(\\S+) \\((.*)\\) = ([0-9A-Fa-f]+)$")
    private val OPENSSL = Regex("^([A-Za-z0-9-]+)\\((.*)\\)= ?([0-9A-Fa-f]+)$")
    private val GNU = Regex("^([0-9A-Fa-f]+) ([ *])(.*)$")
    private val SFV = Regex("^(.+?)[ \\t]+([0-9A-Fa-f]{8})$")
    private val PLAIN_NAMED = Regex("^([0-9A-Fa-f]+)[ \\t]+(.+)$")
    private val PLAIN_LONE = Regex("^([0-9A-Fa-f]+)$")

    fun parse(text: String, sourceFileName: String? = null): ParsedSums {
        val entries = ArrayList<SumsEntry>()
        var ignored = 0
        val seenFormats = HashSet<SumsFormat>()
        val sfvHint = sourceFileName?.lowercase()?.endsWith(".sfv") == true

        text.split('\n').forEachIndexed { idx, rawLine ->
            val lineNo = idx + 1
            // strip CR and leading progress/whitespace padding
            var line = rawLine.trimEnd('\r').trimStart(' ', '\t')
            if (line.isEmpty()) return@forEachIndexed
            if (line.startsWith('#') || line.startsWith(';')) { return@forEachIndexed }

            // Escaped-filename convention: a leading backslash flags a name that
            // contains `\n`, `\r` or `\\` escapes (GNU coreutils only).
            var escaped = false
            if (line.startsWith('\\')) { escaped = true; line = line.substring(1) }

            val tag = TAG.matchEntire(line)
            if (tag != null && !escaped) {
                val id = SumsAlgorithmNames.toId(tag.groupValues[1])
                val fmt = if (tag.groupValues[1].uppercase().startsWith("XXH")) SumsFormat.XXHSUM_TAG else SumsFormat.BSD_TAG
                seenFormats += fmt
                entries += SumsEntry(tag.groupValues[2], tag.groupValues[3].lowercase(), id, lineNumber = lineNo)
                return@forEachIndexed
            }

            val ssl = OPENSSL.matchEntire(line)
            if (ssl != null && !escaped) {
                seenFormats += SumsFormat.OPENSSL
                val id = SumsAlgorithmNames.toId(ssl.groupValues[1])
                entries += SumsEntry(ssl.groupValues[2], ssl.groupValues[3].lowercase(), id, lineNumber = lineNo)
                return@forEachIndexed
            }

            val gnu = GNU.matchEntire(line)
            if (gnu != null) {
                seenFormats += SumsFormat.GNU
                val binary = gnu.groupValues[2] == "*"
                val name = if (escaped) unescapeName(gnu.groupValues[3]) else gnu.groupValues[3]
                entries += SumsEntry(name, gnu.groupValues[1].lowercase(), null, binary, lineNo)
                return@forEachIndexed
            }

            if (!escaped) {
                val sfv = SFV.matchEntire(line)
                if (sfv != null && (sfvHint || seenFormats.contains(SumsFormat.SFV) || looksLikeSfvName(sfv.groupValues[1]))) {
                    seenFormats += SumsFormat.SFV
                    entries += SumsEntry(sfv.groupValues[1].trim(), sfv.groupValues[2].lowercase(), "crc32", lineNumber = lineNo)
                    return@forEachIndexed
                }

                val named = PLAIN_NAMED.matchEntire(line)
                if (named != null) {
                    seenFormats += SumsFormat.PLAIN
                    entries += SumsEntry(named.groupValues[2].trim(), named.groupValues[1].lowercase(), null, lineNumber = lineNo)
                    return@forEachIndexed
                }
                val lone = PLAIN_LONE.matchEntire(line)
                if (lone != null) {
                    seenFormats += SumsFormat.PLAIN
                    entries += SumsEntry("", lone.groupValues[1].lowercase(), null, lineNumber = lineNo)
                    return@forEachIndexed
                }
            }

            ignored++
        }

        val format = when {
            seenFormats.isEmpty() -> SumsFormat.UNKNOWN
            seenFormats.size == 1 -> seenFormats.first()
            else -> SumsFormat.MIXED
        }
        return ParsedSums(entries, format, ignored)
    }

    /** A name field with no hex-only look, i.e. plausible as an SFV filename. */
    private fun looksLikeSfvName(name: String): Boolean =
        name.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }

    /** Reverse coreutils' filename escaping: `\\`→`\`, `\n`→LF, `\r`→CR. */
    private fun unescapeName(raw: String): String {
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 1 < raw.length) {
                when (raw[i + 1]) {
                    '\\' -> { sb.append('\\'); i += 2; continue }
                    'n' -> { sb.append('\n'); i += 2; continue }
                    'r' -> { sb.append('\r'); i += 2; continue }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}
