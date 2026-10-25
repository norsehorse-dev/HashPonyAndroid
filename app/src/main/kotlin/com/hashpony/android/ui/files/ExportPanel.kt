package com.hashpony.android.ui.files

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.android.ui.theme.InstrumentChip
import com.hashpony.android.ui.theme.InstrumentPanel
import com.hashpony.android.ui.theme.MonoLabelStyle
import com.hashpony.android.ui.theme.PrimaryAction
import com.hashpony.android.ui.theme.SecondaryAction
import com.hashpony.android.ui.theme.SectionLabel
import com.hashpony.android.ui.theme.TextFaint
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.Hex
import com.hashpony.core.sums.SumsOutputFormat
import com.hashpony.core.sums.SumsWriter
import com.hashpony.core.sums.WriteEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Writes a sums file from the hashed files. Choose an algorithm (any that was
 * computed) and a format, then Save (CreateDocument) or Share as text. SFV is
 * offered only for CRC32.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportPanel(doneFiles: List<FileUiState>, uppercase: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val availableAlgorithms = remember(doneFiles) {
        AlgorithmRegistry.all.map { it.id }.filter { id -> doneFiles.any { it.results.containsKey(id) } }
    }
    if (availableAlgorithms.isEmpty()) return

    var algorithm by remember(availableAlgorithms) { mutableStateOf(availableAlgorithms.first()) }
    var format by remember { mutableStateOf(SumsOutputFormat.GNU) }
    var pendingText by remember { mutableStateOf("") }

    fun entries(): List<WriteEntry> = doneFiles
        .filter { it.results.containsKey(algorithm) }
        .map { WriteEntry(it.name, Hex.encode(it.results.getValue(algorithm))) }

    fun buildText(): String = SumsWriter.emit(algorithm, entries(), format, uppercase)

    val savedMsg = stringResource(R.string.export_saved)
    val shareTitle = stringResource(R.string.export_share_title)

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val text = pendingText
            scope.launch(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            }
            Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
        }
    }

    InstrumentPanel(modifier.padding(top = 4.dp)) {
        SectionLabel(stringResource(R.string.export_title))

        FieldCaption(stringResource(R.string.export_algorithm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (id in availableAlgorithms) {
                InstrumentChip(
                    label = AlgorithmRegistry.byId(id)?.displayName ?: id,
                    selected = algorithm == id,
                    role = Role.RadioButton,
                    onClick = {
                        algorithm = id
                        if (id != "crc32" && format == SumsOutputFormat.SFV) format = SumsOutputFormat.GNU
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        FieldCaption(stringResource(R.string.export_format))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InstrumentChip(
                label = stringResource(R.string.format_gnu),
                selected = format == SumsOutputFormat.GNU,
                role = Role.RadioButton,
                onClick = { format = SumsOutputFormat.GNU },
            )
            InstrumentChip(
                label = stringResource(R.string.format_bsd),
                selected = format == SumsOutputFormat.BSD_TAG,
                role = Role.RadioButton,
                onClick = { format = SumsOutputFormat.BSD_TAG },
            )
            if (algorithm == "crc32") {
                InstrumentChip(
                    label = stringResource(R.string.format_sfv),
                    selected = format == SumsOutputFormat.SFV,
                    role = Role.RadioButton,
                    onClick = { format = SumsOutputFormat.SFV },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row {
            PrimaryAction(
                text = stringResource(R.string.action_save),
                onClick = {
                    pendingText = buildText()
                    saver.launch(SumsWriter.suggestedFileName(algorithm, format, entries()))
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            SecondaryAction(
                text = stringResource(R.string.action_share),
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, buildText())
                    }
                    context.startActivity(Intent.createChooser(send, shareTitle))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FieldCaption(text: String) {
    Text(
        text.uppercase(Locale.getDefault()),
        style = MonoLabelStyle,
        color = TextFaint,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
