package com.hashpony.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.android.data.AlgorithmKit
import com.hashpony.android.ui.theme.ChoiceField
import com.hashpony.android.ui.theme.MetaStyle
import com.hashpony.android.ui.theme.TextDim
import com.hashpony.core.AlgorithmRegistry

@Composable
fun kitLabel(kit: AlgorithmKit): String = stringResource(
    when (kit) {
        AlgorithmKit.DOWNLOADS -> R.string.kit_downloads
        AlgorithmKit.FORENSICS -> R.string.kit_forensics
        AlgorithmKit.EVERYTHING -> R.string.kit_everything
        AlgorithmKit.CUSTOM -> R.string.kit_custom
    },
)

/** Kit menu plus a one-line readout of exactly what will be computed. */
@Composable
fun KitPicker(
    enabled: Set<String>,
    onSelect: (AlgorithmKit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = AlgorithmKit.matching(enabled)
    val names = AlgorithmRegistry.all.filter { it.id in enabled }.map { it.displayName }
    val summary = when {
        names.isEmpty() -> stringResource(R.string.text_enable_hint)
        names.size == AlgorithmRegistry.all.size -> stringResource(R.string.kit_all_count, names.size)
        else -> names.joinToString(", ")
    }
    Column(modifier) {
        ChoiceField(
            label = stringResource(R.string.kit_label),
            selected = active,
            options = AlgorithmKit.entries,
            optionLabel = { kitLabel(it) },
            onSelect = onSelect,
        )
        Text(
            summary,
            style = MetaStyle,
            color = TextDim,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
