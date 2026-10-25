package com.hashpony.android.ui.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.android.ui.DigestRow
import com.hashpony.android.ui.theme.InstrumentField
import com.hashpony.android.ui.theme.InstrumentPanel
import com.hashpony.android.ui.theme.MonoLabelStyle
import com.hashpony.android.ui.theme.SectionLabel
import com.hashpony.android.ui.theme.TextDim
import com.hashpony.android.ui.theme.TextFaint
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.Hex

@Composable
fun TextScreen(viewModel: TextViewModel, modifier: Modifier = Modifier) {
    val input by viewModel.input.collectAsState()
    val results by viewModel.results.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val uppercase = settings?.uppercaseHex ?: false
    val shareContext = stringResource(R.string.tab_text)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    ) {
        SectionLabel(stringResource(R.string.text_label))
        InstrumentField(
            value = input,
            onValueChange = viewModel::setInput,
            singleLine = false,
            minLines = 4,
            mono = false,
        )
        Text(
            stringResource(R.string.text_encoding),
            style = MonoLabelStyle,
            color = TextFaint,
            modifier = Modifier.padding(start = 2.dp, top = 6.dp),
        )

        Spacer(Modifier.height(16.dp))
        if (results.isEmpty()) {
            InstrumentPanel {
                Text(
                    stringResource(R.string.text_enable_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDim,
                )
            }
        } else {
            SectionLabel(stringResource(R.string.label_digests))
            InstrumentPanel(contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 10.dp)) {
                val ids = AlgorithmRegistry.all.map { it.id }.filter { results.containsKey(it) }
                for (id in ids) {
                    DigestRow(
                        label = AlgorithmRegistry.byId(id)?.displayName ?: id,
                        hex = Hex.encode(results.getValue(id), uppercase),
                        matched = false,
                        shareContext = shareContext,
                    )
                }
            }
        }
    }
}
