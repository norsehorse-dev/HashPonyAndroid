package com.hashpony.android.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.android.ui.KitPicker
import com.hashpony.android.ui.Links
import com.hashpony.android.ui.rememberVersionName
import com.hashpony.android.ui.theme.ListDivider
import com.hashpony.android.ui.theme.MetaStyle
import com.hashpony.android.ui.theme.SectionHeader
import com.hashpony.android.ui.theme.TextDim
import com.hashpony.android.ui.theme.TextFaint
import com.hashpony.android.ui.theme.TextPrimary
import com.hashpony.android.ui.theme.UiStyle
import com.hashpony.android.ui.theme.instrumentSwitchColors
import com.hashpony.core.AlgorithmGroup
import com.hashpony.core.AlgorithmRegistry

/** Settings, or its Licenses page. Navigation and the app bar live in HashPonyApp. */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    showLicenses: Boolean,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (showLicenses) LicensesScreen(modifier) else MainSettings(viewModel, onOpenLicenses, modifier)
}

@Composable
private fun MainSettings(viewModel: SettingsViewModel, onOpenLicenses: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val loaded by viewModel.settings.collectAsState()
    // Draw nothing until preferences load, so switches never flash the wrong state.
    val settings = loaded ?: return
    val enabled = settings.enabledAlgorithms
    val version = rememberVersionName()

    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
    ) {
        SectionHeader(stringResource(R.string.settings_algorithms))
        KitPicker(enabled = enabled, onSelect = { viewModel.applyKit(it) }, modifier = Modifier.padding(bottom = 8.dp))
        for (group in AlgorithmGroup.entries) {
            Text(
                groupLabel(group),
                style = MaterialTheme.typography.titleSmall,
                color = TextDim,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            for (algo in AlgorithmRegistry.byGroup(group)) {
                ListDivider()
                val on = algo.id in enabled
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = on, role = Role.Switch, onValueChange = { viewModel.setAlgorithmEnabled(algo.id, it) })
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(algo.displayName, style = UiStyle, color = TextPrimary)
                        val note = if (algo.id == "keccak-256") stringResource(R.string.note_keccak) else algo.note
                        if (note != null) {
                            Text(note, style = MetaStyle, color = TextFaint, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    Switch(checked = on, onCheckedChange = null, colors = instrumentSwitchColors())
                }
            }
        }

        // Theme mode and Material You toggles are hidden while the dark look is
        // the only scheme (see HashPonyTheme); the prefs stay in place.
        SectionStart(stringResource(R.string.settings_display))
        SwitchRow(stringResource(R.string.settings_uppercase), settings.uppercaseHex) { viewModel.setUppercaseHex(it) }

        SectionStart(stringResource(R.string.settings_help))
        HelpText(stringResource(R.string.help_files_title), stringResource(R.string.help_files_body))
        HelpText(stringResource(R.string.help_verify_title), stringResource(R.string.help_verify_body))
        HelpText(stringResource(R.string.help_text_title), stringResource(R.string.help_text_body))
        HelpText(stringResource(R.string.help_kits_title), stringResource(R.string.help_kits_body))
        HelpText(stringResource(R.string.help_compare_title), stringResource(R.string.help_compare_body))
        HelpText(stringResource(R.string.help_qr_title), stringResource(R.string.help_qr_body))
        HelpText(stringResource(R.string.help_receipt_title), stringResource(R.string.help_receipt_body))
        HelpText(stringResource(R.string.help_tile_title), stringResource(R.string.help_tile_body))

        SectionStart(stringResource(R.string.settings_more))
        Text(
            stringResource(R.string.more_body),
            style = MaterialTheme.typography.bodyMedium,
            color = TextDim,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        for (app in Links.FAMILY) {
            ListDivider()
            LinkRow(app.name, stringResource(app.tagline), Icons.AutoMirrored.Filled.OpenInNew) { open(app.url) }
        }
        ListDivider()
        LinkRow(stringResource(R.string.link_hub), null, Icons.AutoMirrored.Filled.OpenInNew) { open(Links.HUB) }
        ListDivider()
        LinkRow(stringResource(R.string.link_source), null, Icons.AutoMirrored.Filled.OpenInNew) { open(Links.SOURCE) }
        ListDivider()
        LinkRow(stringResource(R.string.link_licenses), null, Icons.Filled.ChevronRight, onOpenLicenses)

        SectionStart(stringResource(R.string.settings_about))
        Text(stringResource(R.string.about_version, version), style = UiStyle, color = TextPrimary)
        Text(
            stringResource(R.string.about_body),
            style = MaterialTheme.typography.bodyMedium,
            color = TextDim,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun LicensesScreen(modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    ) {
        COMPONENTS.forEachIndexed { i, (name, license) ->
            if (i > 0) ListDivider()
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(name, style = UiStyle, color = TextPrimary)
                Text(license, style = MetaStyle, color = TextDim, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Text(
            stringResource(R.string.licenses_note),
            style = MetaStyle,
            color = TextFaint,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

private val COMPONENTS = listOf(
    "Bouncy Castle (bcprov)" to "Bouncy Castle License (MIT-style)",
    "xxHash (vendored port)" to "BSD 2-Clause, © Yann Collet",
    "IBM Plex Mono" to "SIL Open Font License 1.1, © IBM Corp.",
    "ZXing core (QR encoder)" to "Apache License 2.0",
    "AndroidX & Jetpack Compose" to "Apache License 2.0",
    "Material Components / Material 3" to "Apache License 2.0",
    "Kotlin & Kotlin Coroutines" to "Apache License 2.0",
)

/** Space, then a section heading. */
@Composable
private fun SectionStart(title: String) {
    Spacer(Modifier.height(24.dp))
    SectionHeader(title)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = UiStyle, color = TextPrimary)
        Switch(checked = checked, onCheckedChange = null, colors = instrumentSwitchColors())
    }
}

@Composable
private fun groupLabel(group: AlgorithmGroup): String = stringResource(
    when (group) {
        AlgorithmGroup.MODERN -> R.string.group_modern
        AlgorithmGroup.LEGACY -> R.string.group_legacy
        AlgorithmGroup.REGIONAL -> R.string.group_regional
        AlgorithmGroup.CHECKSUM -> R.string.group_checksum
    },
)

@Composable
private fun HelpText(title: String, body: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(title, style = UiStyle, fontWeight = FontWeight.Medium, color = TextPrimary)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextDim, modifier = Modifier.padding(top = 2.dp))
    }
}

/** A tappable row: title, optional second line, trailing icon. */
@Composable
private fun LinkRow(title: String, subtitle: String?, trailing: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = UiStyle, color = TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MetaStyle, color = TextDim, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Icon(trailing, contentDescription = null, tint = TextFaint)
    }
}
