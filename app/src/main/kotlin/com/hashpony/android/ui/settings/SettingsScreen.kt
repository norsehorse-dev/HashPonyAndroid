package com.hashpony.android.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.android.ui.KitPicker
import com.hashpony.android.ui.Links
import com.hashpony.android.ui.theme.InstrumentPanel
import com.hashpony.android.ui.theme.LineSoft
import com.hashpony.android.ui.theme.MonoLabelStyle
import com.hashpony.android.ui.theme.MonoUiStyle
import com.hashpony.android.ui.theme.SectionLabel
import com.hashpony.android.ui.theme.Silver
import com.hashpony.android.ui.theme.SilverBright
import com.hashpony.android.ui.theme.TextDim
import com.hashpony.android.ui.theme.TextFaint
import com.hashpony.android.ui.theme.TextPrimary
import com.hashpony.android.ui.theme.instrumentSwitchColors
import com.hashpony.core.AlgorithmGroup
import com.hashpony.core.AlgorithmRegistry
import java.util.Locale

private enum class SettingsPage { MAIN, LICENSES }

private val ListPadding = PaddingValues(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    var page by remember { mutableStateOf(SettingsPage.MAIN) }
    // System back from Licenses returns to the main Settings page.
    BackHandler(enabled = page == SettingsPage.LICENSES) { page = SettingsPage.MAIN }
    when (page) {
        SettingsPage.MAIN -> MainSettings(viewModel, onOpenLicenses = { page = SettingsPage.LICENSES }, modifier = modifier)
        SettingsPage.LICENSES -> LicensesScreen(onBack = { page = SettingsPage.MAIN }, modifier = modifier)
    }
}

@Composable
private fun MainSettings(viewModel: SettingsViewModel, onOpenLicenses: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val enabled = settings?.enabledAlgorithms ?: emptySet()

    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    ) {
        SectionLabel(stringResource(R.string.kit_label))
        KitPicker(enabled = enabled, onSelect = { viewModel.applyKit(it) }, modifier = Modifier.padding(bottom = 18.dp))

        SectionLabel(stringResource(R.string.settings_algorithms))
        for (group in AlgorithmGroup.entries) {
            InstrumentPanel(Modifier.padding(bottom = 10.dp), contentPadding = ListPadding) {
                Text(
                    group.label.uppercase(Locale.getDefault()),
                    style = MonoLabelStyle,
                    color = Silver,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                )
                val algos = AlgorithmRegistry.byGroup(group)
                algos.forEachIndexed { i, algo ->
                    if (i > 0) RowDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(algo.displayName, style = MonoUiStyle, color = TextPrimary)
                            algo.note?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = TextFaint, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        Switch(
                            checked = algo.id in enabled,
                            onCheckedChange = { viewModel.setAlgorithmEnabled(algo.id, it) },
                            colors = instrumentSwitchColors(),
                        )
                    }
                }
            }
        }

        // Theme mode and Material You toggles are hidden while the dark Instrument
        // look is the only scheme (see HashPonyTheme); the prefs stay in place.
        Spacer(Modifier.height(8.dp))
        SectionLabel(stringResource(R.string.settings_display))
        InstrumentPanel(contentPadding = ListPadding) {
            SwitchRow(stringResource(R.string.settings_uppercase), settings?.uppercaseHex ?: false) { viewModel.setUppercaseHex(it) }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.settings_help))
        InstrumentPanel {
            HelpText(stringResource(R.string.help_files_title), stringResource(R.string.help_files_body))
            HelpText(stringResource(R.string.help_verify_title), stringResource(R.string.help_verify_body))
            HelpText(stringResource(R.string.help_text_title), stringResource(R.string.help_text_body))
            HelpText(stringResource(R.string.help_kits_title), stringResource(R.string.help_kits_body))
            HelpText(stringResource(R.string.help_compare_title), stringResource(R.string.help_compare_body))
            HelpText(stringResource(R.string.help_qr_title), stringResource(R.string.help_qr_body))
            HelpText(stringResource(R.string.help_receipt_title), stringResource(R.string.help_receipt_body))
            HelpText(stringResource(R.string.help_tile_title), stringResource(R.string.help_tile_body))
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.settings_more))
        InstrumentPanel(contentPadding = ListPadding) {
            Text(
                stringResource(R.string.more_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            for (app in Links.FAMILY) {
                RowDivider()
                FamilyRow(app.name, stringResource(app.tagline)) { open(app.url) }
            }
            RowDivider()
            LinkRow(stringResource(R.string.link_hub)) { open(Links.HUB) }
            RowDivider()
            LinkRow(stringResource(R.string.link_source)) { open(Links.SOURCE) }
            RowDivider()
            LinkRow(stringResource(R.string.link_licenses), onOpenLicenses)
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.settings_about))
        InstrumentPanel {
            Text(stringResource(R.string.about_version), style = MonoUiStyle, fontWeight = FontWeight.SemiBold, color = SilverBright)
            Text(
                stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun LicensesScreen(onBack: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = TextDim)
            }
            Text(stringResource(R.string.licenses_title), style = MonoUiStyle, fontWeight = FontWeight.SemiBold, color = SilverBright)
        }
        InstrumentPanel(contentPadding = ListPadding) {
            COMPONENTS.forEachIndexed { i, (name, license) ->
                if (i > 0) RowDivider()
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(name, style = MonoUiStyle, color = TextPrimary)
                    Text(license, style = MaterialTheme.typography.bodySmall, color = TextDim, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        Text(
            stringResource(R.string.licenses_note),
            style = MaterialTheme.typography.bodySmall,
            color = TextFaint,
            modifier = Modifier.padding(top = 12.dp, start = 2.dp),
        )
    }
}

private val COMPONENTS = listOf(
    "Bouncy Castle (bcprov)" to "Bouncy Castle License (MIT-style)",
    "xxHash (vendored port)" to "BSD 2-Clause — © Yann Collet",
    "IBM Plex Mono" to "SIL Open Font License 1.1 — © IBM Corp.",
    "ZXing core (QR encoder)" to "Apache License 2.0",
    "AndroidX & Jetpack Compose" to "Apache License 2.0",
    "Material Components / Material 3" to "Apache License 2.0",
    "Kotlin & Kotlin Coroutines" to "Apache License 2.0",
)

@Composable
private fun RowDivider() {
    HorizontalDivider(thickness = 1.dp, color = LineSoft)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MonoUiStyle, color = TextPrimary)
        Switch(checked = checked, onCheckedChange = onChange, colors = instrumentSwitchColors())
    }
}

@Composable
private fun HelpText(title: String, body: String) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Text(title, style = MonoUiStyle, fontWeight = FontWeight.SemiBold, color = Silver)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextDim, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MonoUiStyle, color = TextPrimary)
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextFaint)
    }
}

/** A sibling Pony app: mono name, tagline underneath, opens its product site. */
@Composable
private fun FamilyRow(name: String, tagline: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MonoUiStyle, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(tagline, style = MaterialTheme.typography.bodySmall, color = TextDim, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextFaint)
    }
}
