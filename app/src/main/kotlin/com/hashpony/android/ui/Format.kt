package com.hashpony.android.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Human-readable byte size, e.g. "4.0 GB". Unknown size renders as empty. */
fun formatSize(bytes: Long?): String {
    if (bytes == null) return ""
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB", "PB")
    var value = bytes.toDouble() / 1024.0
    var i = 0
    while (value >= 1024.0 && i < units.lastIndex) {
        value /= 1024.0
        i++
    }
    return String.format(Locale.US, "%.1f %s", value, units[i])
}

/** The installed versionName, e.g. "1.0.0", so the About line and receipts never drift from the build. */
@Composable
fun rememberVersionName(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            info.versionName
        }.getOrNull() ?: ""
    }
}
