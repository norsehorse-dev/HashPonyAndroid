package com.hashpony.android.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.hashpony.android.ui.theme.MatchGreen
import com.hashpony.android.ui.theme.MonoDigestStyle

/**
 * One algorithm's digest: label above, monospace hex below. Tap copies,
 * long-press opens a menu (copy uppercase / share). Green when [matched].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DigestRow(label: String, hex: String, matched: Boolean, shareContext: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    fun copy(text: String, toast: String) {
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = hex,
                style = MonoDigestStyle,
                color = if (matched) MatchGreen else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = { copy(hex, "Copied $label") },
                        onLongClick = { menuOpen = true },
                    ),
            )
            IconButton(onClick = { copy(hex, "Copied $label") }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy $label")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Copy uppercase") }, onClick = {
                    copy(hex.uppercase(), "Copied uppercase")
                    menuOpen = false
                })
                DropdownMenuItem(text = { Text("Share") }, onClick = {
                    menuOpen = false
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "$label ($shareContext)\n$hex")
                    }
                    context.startActivity(Intent.createChooser(send, "Share digest"))
                })
            }
        }
    }
}
