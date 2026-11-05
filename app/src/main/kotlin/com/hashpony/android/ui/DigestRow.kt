package com.hashpony.android.ui

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.android.ui.qr.QrDialog
import com.hashpony.android.ui.theme.FailRed
import com.hashpony.android.ui.theme.MatchGreen
import com.hashpony.android.ui.theme.MonoDigestStyle
import com.hashpony.android.ui.theme.MetaStyle
import com.hashpony.android.ui.theme.Silver
import com.hashpony.android.ui.theme.StatusText
import com.hashpony.android.ui.theme.TextFaint
import com.hashpony.android.ui.theme.TextPrimary

/**
 * One algorithm's readout: the algorithm name (with a match status when it
 * matches), copy and a more-options menu on the first line, and the full hex
 * underneath so long digests are never cut. Tapping the hex copies it;
 * long-pressing it opens the same menu (copy uppercase, QR code, share).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DigestRow(
    label: String,
    hex: String,
    matched: Boolean,
    shareContext: String,
    diffAgainst: String? = null,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }

    val copiedMsg = stringResource(R.string.copied, label)
    val copiedUpperMsg = stringResource(R.string.copied_uppercase)
    val copyDesc = stringResource(R.string.copy_content_desc, label)
    val shareTitle = stringResource(R.string.share_digest_title)

    fun copy(text: String, toast: String) {
        clipboard.setText(AnnotatedString(text))
        // Android 13+ shows its own clipboard confirmation; don't double it.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MetaStyle,
                fontWeight = FontWeight.Medium,
                color = if (matched) MatchGreen else Silver,
            )
            if (matched) {
                Spacer(Modifier.width(12.dp))
                StatusText(stringResource(R.string.tag_match), MatchGreen, Icons.Filled.CheckCircle)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { copy(hex, copiedMsg) }) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = copyDesc,
                    tint = TextFaint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                        tint = TextFaint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_copy_uppercase), style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            copy(hex.uppercase(), copiedUpperMsg)
                            menuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_show_qr), style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            menuOpen = false
                            showQr = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_share), style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            menuOpen = false
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "$label ($shareContext)\n$hex")
                            }
                            context.startActivity(Intent.createChooser(send, shareTitle))
                        },
                    )
                }
            }
        }
        val shown = remember(hex, diffAgainst) { diffHighlight(hex, diffAgainst) }
        Text(
            text = shown,
            style = MonoDigestStyle,
            color = if (matched) MatchGreen else TextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .combinedClickable(
                    onClick = { copy(hex, copiedMsg) },
                    onLongClick = { menuOpen = true },
                ),
        )
    }
    if (showQr) {
        QrDialog(label = label, value = hex, onDismiss = { showQr = false })
    }
}

/**
 * [hex] with every position that differs from [expected] marked in red, so a
 * near-miss shows exactly where a pasted value diverges. Comparison ignores case
 * (the uppercase-hex setting changes display only). Lengths must match.
 */
private fun diffHighlight(hex: String, expected: String?): AnnotatedString {
    if (expected == null || expected.length != hex.length) return AnnotatedString(hex)
    val bad = SpanStyle(color = FailRed, background = FailRed.copy(alpha = 0.18f))
    return buildAnnotatedString {
        for (i in hex.indices) {
            if (hex[i].lowercaseChar() == expected[i].lowercaseChar()) append(hex[i])
            else withStyle(bad) { append(hex[i]) }
        }
    }
}
