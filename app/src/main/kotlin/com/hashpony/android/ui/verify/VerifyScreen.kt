package com.hashpony.android.ui.verify

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.sums.EntryResult
import com.hashpony.core.sums.EntryStatus
import com.hashpony.core.sums.VerifyReport
import com.hashpony.android.hash.SumsCandidate
import com.hashpony.android.ui.theme.MatchGreen
import com.hashpony.android.ui.theme.MonoDigestStyle

@Composable
fun VerifyScreen(
    viewModel: VerifyViewModel,
    initialSumsUri: Uri?,
    onSumsConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.scanFolder(uri)
    }
    val sumsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.openSumsFile(uri)
    }
    val folderForSumsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.provideFolderForSums(uri)
    }

    LaunchedEffect(initialSumsUri) {
        if (initialSumsUri != null) {
            viewModel.openSumsFile(initialSumsUri)
            onSumsConsumed()
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        when (val s = state) {
            is VerifyUiState.Idle -> IdleView(
                onPickFolder = { folderPicker.launch(null) },
                onOpenSums = { sumsPicker.launch(arrayOf("*/*")) },
            )

            is VerifyUiState.Working -> CenterProgress(stringResource(R.string.verify_working))

            is VerifyUiState.FolderScanned -> FolderScannedView(
                folderName = s.folderName,
                candidates = s.candidates,
                onVerify = viewModel::verifyCandidate,
                onOpenSums = { sumsPicker.launch(arrayOf("*/*")) },
                onReset = viewModel::reset,
            )

            is VerifyUiState.AwaitingFolder -> Column {
                Text(stringResource(R.string.verify_opened, s.sumsName), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.verify_need_folder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Button(onClick = { folderForSumsPicker.launch(null) }) { Text(stringResource(R.string.verify_choose_folder)) }
            }

            is VerifyUiState.NeedAlgorithm -> Column {
                Text(stringResource(R.string.verify_need_algo_title, s.sumsName), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.verify_need_algo_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                for (id in s.candidates) {
                    OutlinedButton(
                        onClick = { viewModel.chooseAlgorithm(id) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    ) { Text(AlgorithmRegistry.byId(id)?.displayName ?: id) }
                }
            }

            is VerifyUiState.Results -> ResultsView(s.sumsName, s.report, onReset = viewModel::reset)

            is VerifyUiState.Failed -> Column {
                Text(stringResource(R.string.verify_failed_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                Text(s.message, modifier = Modifier.padding(vertical = 8.dp))
                TextButton(onClick = viewModel::reset) { Text(stringResource(R.string.verify_start_over)) }
            }
        }
    }
}

@Composable
private fun IdleView(onPickFolder: () -> Unit, onOpenSums: () -> Unit) {
    Column {
        Text(stringResource(R.string.verify_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.verify_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Button(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.verify_pick_folder)) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenSums, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.verify_open_sums)) }
    }
}

@Composable
private fun FolderScannedView(
    folderName: String,
    candidates: List<SumsCandidate>,
    onVerify: (SumsCandidate) -> Unit,
    onOpenSums: () -> Unit,
    onReset: () -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(folderName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onReset) { Text(stringResource(R.string.verify_change)) }
        }
        if (candidates.isEmpty()) {
            Text(
                stringResource(R.string.verify_no_sums),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            OutlinedButton(onClick = onOpenSums) { Text(stringResource(R.string.verify_open_sums)) }
        } else {
            Text(stringResource(R.string.verify_tap_hint), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(candidates, key = { it.relativePath }) { c ->
                    Card(Modifier.fillMaxWidth().clickable { onVerify(c) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(c.name, fontWeight = FontWeight.Medium)
                            if (c.relativePath != c.name) {
                                Text(c.relativePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsView(sumsName: String, report: VerifyReport, onReset: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(sumsName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onReset) { Text(stringResource(R.string.verify_done)) }
        }
        val verdict = if (report.allOk) stringResource(R.string.verify_all_ok, report.ok) else stringResource(R.string.verify_issues)
        Text(
            verdict,
            fontWeight = FontWeight.Bold,
            color = if (report.allOk) MatchGreen else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        val counts = stringResource(R.string.verify_counts, report.ok, report.failed, report.missing)
        Text(
            if (report.unsupported > 0) stringResource(R.string.verify_counts_unknown, counts, report.unsupported) else counts,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(report.sortedForDisplay(), key = { it.name + it.expectedHex }) { entry ->
                EntryRow(entry)
            }
        }
    }
}

@Composable
private fun EntryRow(entry: EntryResult) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIcon(entry.status)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.name.ifEmpty { stringResource(R.string.entry_unnamed) }, fontWeight = FontWeight.Medium)
                    Text(
                        "${statusLabel(entry.status)}${entry.algorithmName?.let { "  ·  $it" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.verify_expected), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(entry.expectedHex, style = MonoDigestStyle)
                val computed = entry.computedHex
                if (computed != null) {
                    Text(stringResource(R.string.verify_computed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        computed,
                        style = MonoDigestStyle,
                        color = if (entry.status == EntryStatus.OK) MatchGreen else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: EntryStatus) {
    val red = MaterialTheme.colorScheme.error
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when (status) {
        EntryStatus.OK -> Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.entry_ok), tint = MatchGreen)
        EntryStatus.FAILED -> Icon(Icons.Filled.Cancel, contentDescription = stringResource(R.string.entry_failed), tint = red)
        EntryStatus.MISSING -> Icon(Icons.Filled.RemoveCircleOutline, contentDescription = stringResource(R.string.entry_missing), tint = muted)
        EntryStatus.UNSUPPORTED -> Icon(Icons.Filled.HelpOutline, contentDescription = stringResource(R.string.entry_unknown), tint = muted)
    }
}

@Composable
private fun CenterProgress(label: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text(label, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun statusLabel(status: EntryStatus): String = when (status) {
    EntryStatus.OK -> stringResource(R.string.entry_ok)
    EntryStatus.FAILED -> stringResource(R.string.entry_failed)
    EntryStatus.MISSING -> stringResource(R.string.entry_missing)
    EntryStatus.UNSUPPORTED -> stringResource(R.string.entry_unknown)
}
