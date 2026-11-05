package com.hashpony.android.ui.files

import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.android.ui.DigestRow
import com.hashpony.android.ui.KitPicker
import com.hashpony.android.ui.formatSize
import com.hashpony.android.ui.qr.QrHandoff
import com.hashpony.android.ui.theme.FailRed
import com.hashpony.android.ui.theme.InstrumentField
import com.hashpony.android.ui.theme.InstrumentProgress
import com.hashpony.android.ui.theme.ListDivider
import com.hashpony.android.ui.theme.MatchGreen
import com.hashpony.android.ui.theme.MetaStyle
import com.hashpony.android.ui.theme.PrimaryAction
import com.hashpony.android.ui.theme.QuietAction
import com.hashpony.android.ui.theme.SecondaryAction
import com.hashpony.android.ui.theme.SectionHeader
import com.hashpony.android.ui.theme.StatusText
import com.hashpony.android.ui.theme.TextDim
import com.hashpony.android.ui.theme.TextFaint
import com.hashpony.android.ui.theme.TextPrimary
import com.hashpony.android.ui.theme.UiStyle
import com.hashpony.android.ui.theme.VerdictPanel
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.HashComparer
import com.hashpony.core.Hex

@Composable
fun FilesScreen(
    viewModel: FilesViewModel,
    sharedUris: List<Uri>,
    onSharedConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    pickFilesRequest: Int = 0,
    onPickFilesConsumed: () -> Unit = {},
) {
    val files by viewModel.files.collectAsState()
    val compareInput by viewModel.compareInput.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val uppercase = settings?.uppercaseHex ?: false
    val enabled = settings?.enabledAlgorithms

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addUris(uris)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.addFolder(uri)
    }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val noScannerMsg = stringResource(R.string.scan_no_app)
    // Scanning is handed to an installed scanner app; HashPony never asks for the camera.
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringExtra(QrHandoff.SCAN_RESULT)
        if (result.resultCode == Activity.RESULT_OK && !text.isNullOrBlank()) viewModel.setCompareInput(text.trim())
    }
    fun launchScan() {
        try {
            scanner.launch(QrHandoff.scanIntent())
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, noScannerMsg, Toast.LENGTH_LONG).show()
        }
    }

    // Quick Settings tile: open the picker as soon as Files is on screen.
    LaunchedEffect(pickFilesRequest) {
        if (pickFilesRequest > 0) {
            onPickFilesConsumed()
            picker.launch(arrayOf("*/*"))
        }
    }

    LaunchedEffect(sharedUris) {
        if (sharedUris.isNotEmpty()) {
            viewModel.addUris(sharedUris)
            onSharedConsumed()
        }
    }

    val doneFiles = files.filter { it.status == HashStatus.DONE && it.results.isNotEmpty() }

    // One scrolling column: controls, then each file's readout, then export.
    LazyColumn(
        modifier = modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
    ) {
        item(key = "controls") {
            Column {
                InstrumentField(
                    value = compareInput,
                    onValueChange = viewModel::setCompareInput,
                    label = stringResource(R.string.files_compare_label),
                    trailingIcon = {
                        Row {
                            if (compareInput.isEmpty()) {
                                IconButton(onClick = {
                                    clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }
                                        ?.let(viewModel::setCompareInput)
                                }) {
                                    Icon(Icons.Filled.ContentPaste, contentDescription = stringResource(R.string.action_paste))
                                }
                            }
                            IconButton(onClick = { launchScan() }) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.action_scan_qr))
                            }
                            if (compareInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setCompareInput("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                                }
                            }
                        }
                    },
                )

                // The kit readout waits for prefs so it never flashes "enable an algorithm".
                if (enabled != null) {
                    Spacer(Modifier.height(12.dp))
                    KitPicker(enabled = enabled, onSelect = viewModel::applyKit)
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrimaryAction(
                        text = stringResource(R.string.files_pick),
                        onClick = { picker.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    SecondaryAction(
                        text = stringResource(R.string.files_pick_folder),
                        onClick = { folderPicker.launch(null) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (files.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader(stringResource(R.string.tab_files), Modifier.weight(1f))
                        QuietAction(stringResource(R.string.action_clear), onClick = viewModel::clear)
                    }
                }
            }
        }

        if (files.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.files_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDim,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        } else {
            if (doneFiles.size == 2) {
                item(key = "vs") {
                    FileVsFileVerdict(doneFiles[0], doneFiles[1], Modifier.padding(bottom = 12.dp))
                }
            }
            items(files, key = { it.uri.toString() }) { file ->
                Column {
                    ListDivider()
                    FileBlock(file, compareInput, uppercase)
                }
            }
            if (doneFiles.isNotEmpty()) {
                item(key = "export") {
                    Column {
                        ListDivider()
                        ExportPanel(doneFiles, uppercase, Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
}

/** Identical/differ verdict for exactly two hashed files, over their shared digests. */
@Composable
private fun FileVsFileVerdict(a: FileUiState, b: FileUiState, modifier: Modifier = Modifier) {
    val shared = a.results.keys intersect b.results.keys
    if (shared.isEmpty()) return
    val identical = shared.all { a.results.getValue(it).contentEquals(b.results.getValue(it)) }
    val algoName = AlgorithmRegistry.byId(shared.first())?.displayName ?: shared.first()
    VerdictPanel(
        title = stringResource(if (identical) R.string.file_vs_identical else R.string.file_vs_differ),
        detail = stringResource(R.string.file_vs_sub, a.name, b.name, algoName),
        color = if (identical) MatchGreen else FailRed,
        icon = if (identical) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
        modifier = modifier,
    )
}

/** One file: name, size and status, then its digests. */
@Composable
private fun FileBlock(file: FileUiState, compareInput: String, uppercase: Boolean) {
    val outcome = remember(file.results, compareInput) { HashComparer.compare(compareInput, file.results) }
    val comparing = compareInput.isNotBlank() && file.results.isNotEmpty()

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                file.name,
                style = UiStyle,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (comparing) {
                Spacer(Modifier.width(8.dp))
                if (outcome.isMatch) {
                    StatusText(stringResource(R.string.tag_match), MatchGreen, Icons.Filled.CheckCircle)
                } else {
                    StatusText(stringResource(R.string.files_no_match), FailRed, Icons.Filled.Cancel)
                }
            }
        }
        val size = formatSize(file.size)
        Text(
            if (size.isEmpty()) statusLabel(file) else "$size, ${statusLabel(file)}",
            style = MetaStyle,
            color = if (file.status == HashStatus.ERROR) FailRed else TextFaint,
            modifier = Modifier.padding(top = 2.dp),
        )

        if (file.status == HashStatus.HASHING) {
            Spacer(Modifier.height(12.dp))
            InstrumentProgress(file.progress)
        }

        val nearMiss = remember(file.results, outcome) {
            if (comparing && !outcome.isMatch) HashComparer.nearest(outcome.normalizedExpected, file.results) else null
        }
        if (comparing && !outcome.isMatch && outcome.normalizedExpected.isNotEmpty() && file.status == HashStatus.DONE) {
            val note = if (nearMiss != null) {
                stringResource(
                    R.string.files_closest,
                    AlgorithmRegistry.byId(nearMiss.algorithmId)?.displayName ?: nearMiss.algorithmId,
                    nearMiss.differing,
                    nearMiss.length,
                )
            } else {
                stringResource(R.string.files_no_length, outcome.normalizedExpected.length)
            }
            Text(note, style = MetaStyle, color = FailRed, modifier = Modifier.padding(top = 8.dp))
        }

        if (comparing && outcome.isMatch) {
            val matchNames = outcome.matched.mapNotNull { AlgorithmRegistry.byId(it)?.displayName }
            Text(
                stringResource(R.string.files_match, matchNames.joinToString(", ")),
                style = MetaStyle,
                color = MatchGreen,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // Result rows in registry order.
        val ids = AlgorithmRegistry.all.map { it.id }.filter { file.results.containsKey(it) }
        if (ids.isNotEmpty()) Spacer(Modifier.height(8.dp))
        for (id in ids) {
            val bytes = file.results.getValue(id)
            DigestRow(
                label = AlgorithmRegistry.byId(id)?.displayName ?: id,
                hex = Hex.encode(bytes, uppercase),
                matched = comparing && id in outcome.matched,
                shareContext = file.name,
                diffAgainst = if (nearMiss?.algorithmId == id) outcome.normalizedExpected else null,
            )
        }
    }
}

@Composable
private fun statusLabel(file: FileUiState): String = when (file.status) {
    HashStatus.PENDING -> stringResource(R.string.status_queued)
    HashStatus.HASHING -> stringResource(R.string.status_hashing)
    HashStatus.DONE -> stringResource(R.string.status_done)
    HashStatus.ERROR -> file.error ?: stringResource(R.string.status_failed)
    HashStatus.CANCELLED -> stringResource(R.string.status_cancelled)
}
