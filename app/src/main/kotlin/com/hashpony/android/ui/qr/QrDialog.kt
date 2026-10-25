package com.hashpony.android.ui.qr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hashpony.android.R
import com.hashpony.android.ui.theme.InstrumentPanel
import com.hashpony.android.ui.theme.MonoDigestStyle
import com.hashpony.android.ui.theme.QuietAction
import com.hashpony.android.ui.theme.SectionLabel
import com.hashpony.android.ui.theme.TextDim
import com.hashpony.android.ui.theme.TextFaint

/**
 * Shows [value] as a QR code with its label and the hex underneath. The code is
 * drawn dark-on-white with a quiet zone, since many scanners reject inverted QR.
 */
@Composable
fun QrDialog(label: String, value: String, onDismiss: () -> Unit) {
    val modules = remember(value) { runCatching { QrHandoff.encode(value) }.getOrNull() }
    Dialog(onDismissRequest = onDismiss) {
        InstrumentPanel {
            SectionLabel(label)
            if (modules != null) {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(18.dp),
                ) {
                    val n = modules.size
                    val cell = size.minDimension / n
                    // Overdraw each cell slightly so antialiasing leaves no seams.
                    val drawn = Size(cell + 0.6f, cell + 0.6f)
                    for (y in 0 until n) for (x in 0 until n) {
                        if (modules[x, y]) drawRect(Color.Black, topLeft = Offset(x * cell, y * cell), size = drawn)
                    }
                }
            }
            Text(
                value,
                style = MonoDigestStyle,
                color = TextDim,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                stringResource(R.string.qr_hint),
                style = MaterialTheme.typography.bodySmall,
                color = TextFaint,
                modifier = Modifier.padding(top = 6.dp),
            )
            QuietAction(
                stringResource(R.string.action_close),
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
