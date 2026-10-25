package com.hashpony.android.ui

import android.content.Intent
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
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.android.ui.theme.MatchGreen
import com.hashpony.android.ui.theme.MonoDigestStyle
import com.hashpony.android.ui.theme.MonoLabelStyle
import com.hashpony.android.ui.theme.Silver
import com.hashpony.android.ui.theme.StatusTag
import com.hashpony.android.ui.theme.TextFaint
import com.hashpony.android.ui.theme.TextPrimary

/**
 * One algorithm's readout: mono label with an optional MATCH tag and copy on the
 * first line, the full hex wrapping underneath so long digests are never cut.
 * Tap the hex copies it, long-press opens the copy-uppercase / share menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DigestRow(label: String, hex: String, matched: Boolean, shareContext: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    val copiedMsg = stringResource(R.string.copied, label)
    val copiedUpperMsg = stringResource(R.string.copied_uppercase)
    val copyDesc = stringResource(R.string.copy_content_desc, label)
    val shareTitle = stringResource(R.string.share_digest_title)

    fun copy(text: String, toast: String) {
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MonoLabelStyle,
                color = if (matched) MatchGreen else Silver,
            )
            if (matched) {
                Spacer(Modifier.width(8.dp))
                StatusTag(stringResource(R.string.tag_match), MatchGreen)
            }
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { copy(hex, copiedMsg) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = copyDesc,
                        tint = TextFaint,
                        modifier = Modifier.size(18.dp),
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
        Text(
            text = hex,
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
}
