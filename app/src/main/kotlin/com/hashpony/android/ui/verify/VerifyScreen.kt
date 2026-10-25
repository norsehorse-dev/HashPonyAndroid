package com.hashpony.android.ui.verify

import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.hashpony.core.sums.VerifyReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.android.hash.SumsCandidate
import com.hashpony.android.ui.theme.FailRed
import com.hashpony.android.ui.theme.InstrumentPanel
import com.hashpony.android.ui.theme.InstrumentWorking
import com.hashpony.android.ui.theme.MatchGreen
import com.hashpony.android.ui.theme.MissAmber
import com.hashpony.android.ui.theme.MonoDigestStyle
import com.hashpony.android.ui.theme.MonoLabelStyle
import com.hashpony.android.ui.theme.MonoUiStyle
import com.hashpony.android.ui.theme.PrimaryAction
import com.hashpony.android.ui.theme.QuietAction
import com.hashpony.android.ui.theme.SecondaryAction
import com.hashpony.android.ui.theme.SectionLabel
import com.hashpony.android.ui.theme.SilverBright
import com.hashpony.android.ui.theme.SilverDim
import com.hashpony.android.ui.theme.StatusTag
import com.hashpony.android.ui.theme.TextDim
import com.hashpony.android.ui.theme.TextFaint
import com.hashpony.android.ui.theme.TextPrimary
import com.hashpony.android.ui.theme.VerdictPanel
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.sums.EntryResult
import com.hashpony.core.sums.EntryStatus
import com.hashpony.core.sums.VerifyReport
import java.util.Locale

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

    Column(modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
        when (val s = state) {
            is VerifyUiState.Idle -> IdleView(
                onPickFolder = { folderPicker.launch(null) },
                onOpenSums = { sumsPicker.launch(arrayOf("*/*")) },
            )

            is VerifyUiState.Working -> InstrumentWorking(stringResource(R.string.verify_working))

            is VerifyUiState.FolderScanned -> FolderScannedView(
                folderName = s.folderName,
                candidates = s.candidates,
                onVerify = viewModel::verifyCandidate,
                onOpenSums = { sumsPicker.launch(arrayOf("*/*")) },
                onReset = viewModel::reset,
            )

            is VerifyUiState.AwaitingFolder -> InstrumentPanel {
                Text(
                    stringResource(R.string.verify_opened, s.sumsName),
                    style = MonoUiStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = SilverBright,
                )
                Text(
                    stringResource(R.string.verify_need_folder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDim,
                    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
                )
                PrimaryAction(
                    text = stringResource(R.string.verify_choose_folder),
                    onClick = { folderForSumsPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is VerifyUiState.NeedAlgorithm -> Column(Modifier.verticalScroll(rememberScrollState())) {
                InstrumentPanel {
                    Text(
                        stringResource(R.string.verify_need_algo_title, s.sumsName),
                        style = MonoUiStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = SilverBright,
                    )
                    Text(
                        stringResource(R.string.verify_need_algo_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDim,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                for (id in s.candidates) {
                    SecondaryAction(
                        text = AlgorithmRegistry.byId(id)?.displayName ?: id,
                        onClick = { viewModel.chooseAlgorithm(id) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }
            }

            is VerifyUiState.Results -> ResultsView(s.sumsName, s.report, onReset = viewModel::reset)

            is VerifyUiState.Failed -> Column {
                VerdictPanel(
                    title = stringResource(R.string.verify_failed_title),
                    detail = s.message,
                    color = FailRed,
                )
                QuietAction(
                    stringResource(R.string.verify_start_over),
                    onClick = viewModel::reset,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun IdleView(onPickFolder: () -> Unit, onOpenSums: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SectionLabel(stringResource(R.string.tab_verify))
        InstrumentPanel {
            Text(
                stringResource(R.string.verify_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = SilverBright,
            )
            Text(
                stringResource(R.string.verify_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            PrimaryAction(
                text = stringResource(R.string.verify_pick_folder),
                onClick = onPickFolder,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            SecondaryAction(
                text = stringResource(R.string.verify_open_sums),
                onClick = onOpenSums,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HeaderRow(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MonoUiStyle,
            fontWeight = FontWeight.SemiBold,
            color = SilverBright,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        QuietAction(action, onClick = onAction)
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
        HeaderRow(folderName, stringResource(R.string.verify_change), onReset)
        if (candidates.isEmpty()) {
            InstrumentPanel(Modifier.padding(top = 4.dp)) {
                Text(
                    stringResource(R.string.verify_no_sums),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDim,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                SecondaryAction(
                    text = stringResource(R.string.verify_open_sums),
                    onClick = onOpenSums,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            SectionLabel(stringResource(R.string.verify_tap_hint), Modifier.padding(top = 4.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(candidates, key = { it.relativePath }) { c ->
                    InstrumentPanel(onClick = { onVerify(c) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MonoUiStyle, fontWeight = FontWeight.Medium, color = TextPrimary)
                                if (c.relativePath != c.name) {
                                    Text(c.relativePath, style = MonoLabelStyle, color = TextFaint, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextFaint)
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
        HeaderRow(sumsName, stringResource(R.string.verify_done), onReset)
        val verdict = if (report.allOk) stringResource(R.string.verify_all_ok, report.ok) else stringResource(R.string.verify_issues)
        val counts = stringResource(R.string.verify_counts, report.ok, report.failed, report.missing)
        val countsLine = if (report.unsupported > 0) stringResource(R.string.verify_counts_unknown, counts, report.unsupported) else counts
        VerdictPanel(
            title = verdict,
            detail = countsLine,
            color = if (report.allOk) MatchGreen else FailRed,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        ReceiptActions(sumsName, report, verdict, countsLine)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(report.sortedForDisplay(), key = { it.name + it.expectedHex }) { entry ->
                EntryRow(entry)
            }
        }
    }
}

private fun statusColor(status: EntryStatus): Color = when (status) {
    EntryStatus.OK -> MatchGreen
    EntryStatus.FAILED -> FailRed
    EntryStatus.MISSING -> MissAmber
    EntryStatus.UNSUPPORTED -> SilverDim
}

@Composable
private fun EntryRow(entry: EntryResult) {
    var expanded by remember { mutableStateOf(false) }
    val color = statusColor(entry.status)
    InstrumentPanel(onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    entry.name.ifEmpty { stringResource(R.string.entry_unnamed) },
                    style = MonoUiStyle,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                entry.algorithmName?.let {
                    Text(it, style = MonoLabelStyle, color = TextFaint, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            StatusTag(statusLabel(entry.status), color)
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.verify_expected).uppercase(Locale.getDefault()),
                style = MonoLabelStyle,
                color = TextFaint,
            )
            Text(entry.expectedHex, style = MonoDigestStyle, color = TextDim)
            val computed = entry.computedHex
            if (computed != null) {
                Text(
                    stringResource(R.string.verify_computed).uppercase(Locale.getDefault()),
                    style = MonoLabelStyle,
                    color = TextFaint,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    computed,
                    style = MonoDigestStyle,
                    color = if (entry.status == EntryStatus.OK) MatchGreen else FailRed,
                )
            }
        }
    }
}

@Composable
private fun statusLabel(status: EntryStatus): String = when (status) {
    EntryStatus.OK -> stringResource(R.string.entry_ok)
    EntryStatus.FAILED -> stringResource(R.string.entry_failed)
    EntryStatus.MISSING -> stringResource(R.string.entry_missing)
    EntryStatus.UNSUPPORTED -> stringResource(R.string.entry_unknown)
}

/** Share or save a plain-text receipt of this verify run. */
@Composable
private fun ReceiptActions(sumsName: String, report: VerifyReport, verdict: String, countsLine: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val labels = VerifyReceipt.Labels(
        title = stringResource(R.string.receipt_title),
        sumsFile = stringResource(R.string.receipt_sums_file),
        checked = stringResource(R.string.receipt_checked),
        result = stringResource(R.string.receipt_result),
        verdict = verdict,
        counts = countsLine,
        footer = stringResource(R.string.about_version),
    )
    // Stamped once per result, so sharing twice gives the same receipt.
    val checkedAt = remember(report) { OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString() }
    val receipt = remember(report, labels, checkedAt) { VerifyReceipt.build(sumsName, report, checkedAt, labels) }
    val shareTitle = stringResource(R.string.receipt_share_title)
    val savedMsg = stringResource(R.string.export_saved)

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(receipt.toByteArray()) } }
            }
            Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
        }
    }

    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        SecondaryAction(
            text = stringResource(R.string.verify_share_receipt),
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, receipt)
                }
                context.startActivity(Intent.createChooser(send, shareTitle))
            },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        SecondaryAction(
            text = stringResource(R.string.verify_save_receipt),
            onClick = { saver.launch("$sumsName.receipt.txt") },
            modifier = Modifier.weight(1f),
        )
    }
}
