package com.hashpony.android.ui.verify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Verify tab. Full sums-file checking (folder-first and sums-file-first flows,
 * tree resolution, results UI) lands in Phase B — this is the phase where the
 * app earns its existence. This placeholder keeps the tab visible in Phase A.
 */
@Composable
fun VerifyScreen(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Verify", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Check a folder against its sums file — SHA256SUMS, .sha256, .md5, .sfv, " +
                "BSD tags and plain single-hash files — with per-entry OK / FAILED / MISSING. " +
                "Arriving in the next update.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
