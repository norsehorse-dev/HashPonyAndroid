package com.hashpony.android.ui.files

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.Hex
import com.hashpony.core.sums.SumsOutputFormat
import com.hashpony.core.sums.SumsWriter
import com.hashpony.core.sums.WriteEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Writes a sums file from the hashed files. Choose an algorithm (any that was
 * computed) and a format, then Save (CreateDocument) or Share as text. SFV is
 * offered only for CRC32.
 */
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

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val text = pendingText
            scope.launch(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            }
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
        }
    }

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Export sums file", fontWeight = FontWeight.SemiBold)

            Text("Algorithm", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (id in availableAlgorithms) {
                    FilterChip(
                        selected = algorithm == id,
                        onClick = {
                            algorithm = id
                            if (id != "crc32" && format == SumsOutputFormat.SFV) format = SumsOutputFormat.GNU
                        },
                        label = { Text(AlgorithmRegistry.byId(id)?.displayName ?: id) },
                    )
                }
            }

            Text("Format", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = format == SumsOutputFormat.GNU, onClick = { format = SumsOutputFormat.GNU }, label = { Text("GNU") })
                FilterChip(selected = format == SumsOutputFormat.BSD_TAG, onClick = { format = SumsOutputFormat.BSD_TAG }, label = { Text("BSD tag") })
                if (algorithm == "crc32") {
                    FilterChip(selected = format == SumsOutputFormat.SFV, onClick = { format = SumsOutputFormat.SFV }, label = { Text("SFV") })
                }
            }

            Spacer(Modifier.height(12.dp))
            Row {
                Button(onClick = {
                    pendingText = buildText()
                    saver.launch(SumsWriter.suggestedFileName(algorithm, format, entries()))
                }) { Text("Save") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, buildText())
                    }
                    context.startActivity(Intent.createChooser(send, "Share sums file"))
                }) { Text("Share") }
            }
        }
    }
}
