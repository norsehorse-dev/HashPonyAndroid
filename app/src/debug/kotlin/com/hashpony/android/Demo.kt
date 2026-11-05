package com.hashpony.android

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.hashpony.android.data.AlgorithmKit
import com.hashpony.android.i18n.LanguageManager
import com.hashpony.android.i18n.SupportedLanguage
import com.hashpony.android.ui.files.FilesViewModel
import com.hashpony.android.ui.text.TextViewModel
import com.hashpony.android.ui.verify.VerifyViewModel
import com.hashpony.core.sums.FileResolver
import com.hashpony.core.sums.SumsParser
import com.hashpony.core.sums.SumsVerifier
import java.io.File
import java.security.MessageDigest
import kotlin.random.Random

/**
 * Store screenshots, debug builds only (tools/play-screenshots.sh drives it over adb):
 *
 *   am start -n com.hashpony.android.debug/com.hashpony.android.MainActivity --es demo_lang de --es demo_screen files
 *
 * demo_lang switches the in-app language. demo_screen picks the screen: files, text,
 * verify, algorithms or settings. Every digest on screen is real: the demo writes a
 * small download folder to the cache and HashPony hashes and verifies it the normal
 * way. Nothing is shown that the app didn't compute.
 */
object Demo {

    private const val SUMS = "SHA256SUMS"
    private const val APK = "release-2.4.1.apk"
    private const val ASC = "release-2.4.1.apk.asc"
    private const val README = "README.txt"
    private const val CHANGELOG = "CHANGELOG.txt"

    /** Returns the screen to show, or null when the intent isn't a demo request. */
    fun handle(
        context: Context,
        intent: Intent?,
        files: FilesViewModel,
        text: TextViewModel,
        verify: VerifyViewModel,
    ): String? {
        val screen = intent?.getStringExtra("demo_screen") ?: return null
        intent.getStringExtra("demo_lang")?.let { tag ->
            val lang = SupportedLanguage.resolve(tag) ?: SupportedLanguage.EN
            if (LanguageManager.current() != lang) LanguageManager.setLanguage(lang)
            intent.removeExtra("demo_lang")
        }
        seed(context, files, text, verify)
        return screen
    }

    private fun seed(context: Context, files: FilesViewModel, text: TextViewModel, verify: VerifyViewModel) {
        val dir = File(context.cacheDir, "demo/downloads").apply { mkdirs() }
        val apkBytes = Random(2041).nextBytes(3_418_112)
        val ascBytes = Random(2042).nextBytes(566)
        val readmePublished = "Release 2.4.1\n\nInstall the APK, then check it against SHA256SUMS.\n".toByteArray()
        val changelog = "2.4.1: bug fixes.\n".toByteArray()

        val apk = put(dir, APK, apkBytes)
        val asc = put(dir, ASC, ascBytes)
        // Edited after the sums file was published, so it verifies as Failed.
        val readme = put(dir, README, readmePublished + "Edited after publishing.\n".toByteArray())
        // CHANGELOG.txt is listed in the sums file but never written, so it verifies as Missing.
        File(dir, CHANGELOG).delete()

        val sums = listOf(APK to apkBytes, ASC to ascBytes, README to readmePublished, CHANGELOG to changelog)
            .joinToString("") { (name, bytes) -> "${sha256(bytes)}  $name\n" }
        put(dir, SUMS, sums.toByteArray())

        files.applyKit(AlgorithmKit.DOWNLOADS)
        val authority = context.packageName + ".demo"
        files.addUris(listOf(apk, asc, readme).map { FileProvider.getUriForFile(context, authority, it) })
        // The published value for the APK, pasted in, so the compare box shows a real match.
        files.setCompareInput(sha256(apkBytes))

        text.setInput("The quick brown fox jumps over the lazy dog")

        val report = SumsVerifier.verify(
            SumsParser.parse(sums, SUMS), SUMS, setOf("sha256"),
            resolver = FileResolver { name -> File(dir, name).takeIf { it.isFile }?.inputStream() },
        )
        verify.showDemo(SUMS, report)
    }

    /** Write [bytes] unless the file already holds exactly them. */
    private fun put(dir: File, name: String, bytes: ByteArray): File {
        val f = File(dir, name)
        if (!f.isFile || !f.readBytes().contentEquals(bytes)) f.writeBytes(bytes)
        return f
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
