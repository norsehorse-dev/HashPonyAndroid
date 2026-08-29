package com.hashpony.android.ui.files

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.HashComparer
import com.hashpony.core.Hex
import com.hashpony.android.ui.formatSize
import com.hashpony.android.ui.theme.MatchGreen
import com.hashpony.android.ui.theme.MonoDigestStyle

private val QUICK_TOGGLE_IDS = listOf("sha512", "sha1", "md5")

@Composable
fun FilesScreen(
    viewModel: FilesViewModel,
    sharedUris: List<Uri>,
    onSharedConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val files by viewModel.files.collectAsState()
    val compareInput by viewModel.compareInput.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val uppercase = settings?.uppercaseHex ?: false
    val enabled = settings?.enabledAlgorithms ?: emptySet()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addUris(uris)
    }

    androidx.compose.runtime.LaunchedEffect(sharedUris) {
        if (sharedUris.isNotEmpty()) {
            viewModel.addUris(sharedUris)
            onSharedConsumed()
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        androidx.compose.material3.OutlinedTextField(
            value = compareInput,
            onValueChange = viewModel::setCompareInput,
            label = { Text("Compare against an expected value") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (compareInput.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setCompareInput("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
        )

        Spacer(Modifier.height(8.dp))
        QuickToggles(enabled) { id, on -> viewModel.setAlgorithmEnabled(id, on) }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Pick files") }
            if (files.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = viewModel::clear) { Text("Clear") }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (files.isEmpty()) {
            Text(
                "Pick files, or share them to HashPony from any app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            val doneFiles = files.filter { it.status == HashStatus.DONE && it.results.isNotEmpty() }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(files, key = { it.uri.toString() }) { file ->
                    FileCard(file, compareInput, uppercase)
                }
                if (doneFiles.isNotEmpty()) {
                    item(key = "export") { ExportPanel(doneFiles, uppercase) }
                }
            }
        }
    }
}

@Composable
private fun QuickToggles(enabled: Set<String>, onToggle: (String, Boolean) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (id in QUICK_TOGGLE_IDS) {
            val algo = AlgorithmRegistry.byId(id) ?: continue
            val on = id in enabled
            FilterChip(
                selected = on,
                onClick = { onToggle(id, !on) },
                label = { Text(algo.displayName) },
            )
        }
    }
}

@Composable
private fun FileCard(file: FileUiState, compareInput: String, uppercase: Boolean) {
    val outcome = remember(file.results, compareInput) { HashComparer.compare(compareInput, file.results) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(file.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Text(
                "${formatSize(file.size)}  ·  ${statusLabel(file)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (file.status == HashStatus.HASHING) {
                Spacer(Modifier.height(8.dp))
                if (file.progress != null) {
                    LinearProgressIndicator(progress = { file.progress }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if (compareInput.isNotBlank() && file.results.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val matchNames = outcome.matched.mapNotNull { AlgorithmRegistry.byId(it)?.displayName }
                if (outcome.isMatch) {
                    Text("Match — ${matchNames.joinToString(", ")}", color = MatchGreen, fontWeight = FontWeight.Bold)
                } else {
                    Text("No match", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }

            // Result rows in registry order.
            val ids = AlgorithmRegistry.all.map { it.id }.filter { file.results.containsKey(it) }
            for (id in ids) {
                val bytes = file.results.getValue(id)
                com.hashpony.android.ui.DigestRow(
                    label = AlgorithmRegistry.byId(id)?.displayName ?: id,
                    hex = Hex.encode(bytes, uppercase),
                    matched = id in outcome.matched && compareInput.isNotBlank(),
                    shareContext = file.name,
                )
            }
        }
    }
}

private fun statusLabel(file: FileUiState): String = when (file.status) {
    HashStatus.PENDING -> "Queued"
    HashStatus.HASHING -> "Hashing…"
    HashStatus.DONE -> "Done"
    HashStatus.ERROR -> file.error ?: "Failed"
    HashStatus.CANCELLED -> "Cancelled"
}
