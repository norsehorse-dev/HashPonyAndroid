package com.hashpony.android.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilterChip
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
import com.hashpony.core.AlgorithmGroup
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.android.R
import com.hashpony.android.data.ThemeMode
import com.hashpony.android.ui.Links

private enum class SettingsPage { MAIN, LICENSES }

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    var page by remember { mutableStateOf(SettingsPage.MAIN) }
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
            .padding(16.dp),
    ) {
        SectionHeader(stringResource(R.string.settings_algorithms))
        for (group in AlgorithmGroup.entries) {
            Text(
                group.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            for (algo in AlgorithmRegistry.byGroup(group)) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(algo.displayName, style = MaterialTheme.typography.bodyLarge)
                        algo.note?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = algo.id in enabled,
                        onCheckedChange = { viewModel.setAlgorithmEnabled(algo.id, it) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        SectionHeader(stringResource(R.string.settings_display))
        SwitchRow(stringResource(R.string.settings_uppercase), settings?.uppercaseHex ?: false) { viewModel.setUppercaseHex(it) }
        SwitchRow(stringResource(R.string.settings_dynamic), settings?.dynamicColor ?: true) { viewModel.setDynamicColor(it) }

        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val current = settings?.themeMode ?: ThemeMode.SYSTEM
            for (mode in ThemeMode.entries) {
                val label = when (mode) {
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                }
                FilterChip(
                    selected = current == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    label = { Text(label) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        SectionHeader(stringResource(R.string.settings_help))
        HelpText(stringResource(R.string.help_files_title), stringResource(R.string.help_files_body))
        HelpText(stringResource(R.string.help_verify_title), stringResource(R.string.help_verify_body))
        HelpText(stringResource(R.string.help_text_title), stringResource(R.string.help_text_body))

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        SectionHeader(stringResource(R.string.settings_more))
        Text(
            stringResource(R.string.more_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        LinkRow(stringResource(R.string.link_hub)) { open(Links.HUB) }
        LinkRow(stringResource(R.string.link_source)) { open(Links.SOURCE) }
        NavRow(stringResource(R.string.link_licenses), onOpenLicenses)

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        SectionHeader(stringResource(R.string.settings_about))
        Text(stringResource(R.string.about_version), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.about_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun LicensesScreen(onBack: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.licenses_title), style = MaterialTheme.typography.titleLarge)
        }
        for ((name, license) in COMPONENTS) {
            Column(Modifier.padding(vertical = 6.dp)) {
                Text(name, fontWeight = FontWeight.Medium)
                Text(license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.licenses_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val COMPONENTS = listOf(
    "Bouncy Castle (bcprov)" to "Bouncy Castle License (MIT-style)",
    "xxHash (vendored port)" to "BSD 2-Clause — © Yann Collet",
    "AndroidX & Jetpack Compose" to "Apache License 2.0",
    "Material Components / Material 3" to "Apache License 2.0",
    "Kotlin & Kotlin Coroutines" to "Apache License 2.0",
)

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun HelpText(title: String, body: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) = LinkRow(label, onClick)
