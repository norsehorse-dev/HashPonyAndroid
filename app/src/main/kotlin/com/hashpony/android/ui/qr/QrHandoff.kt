package com.hashpony.android.ui.qr

import android.content.Intent
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR trust handoff without a camera permission: HashPony only ever renders QR
 * codes itself (ZXing's encoder, no network, no camera). Scanning is handed to
 * whatever scanner app the user has installed through the long-standing ZXing
 * SCAN intent, which Binary Eye and most scanners answer.
 */
object QrHandoff {

    /** Square module grid, true = dark. No quiet zone; the dialog draws one. */
    class Modules(val size: Int, private val dark: BooleanArray) {
        operator fun get(x: Int, y: Int): Boolean = dark[y * size + x]
    }

    fun encode(text: String): Modules {
        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 0,
        )
        // Width/height 0 asks ZXing for the bare matrix, one cell per module.
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
        val n = m.width
        return Modules(n, BooleanArray(n * n) { m.get(it % n, it / n) })
    }

    const val SCAN_ACTION = "com.google.zxing.client.android.SCAN"
    const val SCAN_RESULT = "SCAN_RESULT"

    fun scanIntent(): Intent = Intent(SCAN_ACTION)
        .putExtra("SCAN_FORMATS", "QR_CODE")
        .putExtra("SCAN_MODE", "QR_CODE_MODE")
        .putExtra("SAVE_HISTORY", false)
}
