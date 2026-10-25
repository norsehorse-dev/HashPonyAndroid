package com.hashpony.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.android.data.AlgorithmKit
import com.hashpony.android.ui.theme.InstrumentChip
import com.hashpony.android.ui.theme.MonoLabelStyle
import com.hashpony.android.ui.theme.TextFaint
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

/** Kit chips plus a one-line readout of exactly what will be computed. */
@OptIn(ExperimentalLayoutApi::class)
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
        else -> names.joinToString("  ·  ")
    }
    Column(modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (kit in AlgorithmKit.entries) {
                InstrumentChip(
                    label = kitLabel(kit),
                    selected = kit == active,
                    onClick = { onSelect(kit) },
                    role = Role.RadioButton,
                )
            }
        }
        Text(
            summary,
            style = MonoLabelStyle,
            color = TextFaint,
            modifier = Modifier.padding(start = 2.dp, top = 8.dp),
        )
    }
}
