package com.hashpony.android.ui.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.Hex
import com.hashpony.android.ui.DigestRow

@Composable
fun TextScreen(viewModel: TextViewModel, modifier: Modifier = Modifier) {
    val input by viewModel.input.collectAsState()
    val results by viewModel.results.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val uppercase = settings?.uppercaseHex ?: false

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = viewModel::setInput,
            label = { Text("Text to hash") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Text(
            "UTF-8 encoding",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(8.dp))
        if (results.isEmpty()) {
            Text(
                "Enable at least one algorithm in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val ids = AlgorithmRegistry.all.map { it.id }.filter { results.containsKey(it) }
            for (id in ids) {
                DigestRow(
                    label = AlgorithmRegistry.byId(id)?.displayName ?: id,
                    hex = Hex.encode(results.getValue(id), uppercase),
                    matched = false,
                    shareContext = "text",
                )
            }
        }
    }
}
