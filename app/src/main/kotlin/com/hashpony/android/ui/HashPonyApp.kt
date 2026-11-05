package com.hashpony.android.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hashpony.android.R
import com.hashpony.android.ui.files.FilesViewModel
import com.hashpony.android.ui.hash.HashScreen
import com.hashpony.android.ui.settings.SettingsPage
import com.hashpony.android.ui.settings.SettingsScreen
import com.hashpony.android.ui.settings.SettingsViewModel
import com.hashpony.android.ui.text.TextViewModel
import com.hashpony.android.ui.theme.Ground
import com.hashpony.android.ui.theme.HashPonyTopBar
import com.hashpony.android.ui.theme.LineSoft
import com.hashpony.android.ui.theme.PanelHigh
import com.hashpony.android.ui.theme.SilverBright
import com.hashpony.android.ui.theme.TextFaint
import com.hashpony.android.ui.verify.VerifyScreen
import com.hashpony.android.ui.verify.VerifyViewModel

private enum class Pillar(@StringRes val labelRes: Int, val icon: ImageVector) {
    HASH(R.string.pillar_hash, Icons.Filled.Tag),
    VERIFY(R.string.tab_verify, Icons.Filled.FactCheck),
}

@Composable
fun HashPonyApp(
    sharedUris: List<Uri>,
    onSharedConsumed: () -> Unit,
    verifyUri: Uri?,
    onVerifyConsumed: () -> Unit,
    pickFilesRequest: Int = 0,
    onPickFilesConsumed: () -> Unit = {},
) {
    val filesViewModel: FilesViewModel = viewModel()
    val textViewModel: TextViewModel = viewModel()
    val verifyViewModel: VerifyViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    var pillar by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var settingsPage by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }

    // Back steps out of a Settings sub-page to Settings, then out of Settings,
    // before the app closes. The app bar arrow does the same.
    val goBack: () -> Unit = {
        if (settingsPage != SettingsPage.MAIN) settingsPage = SettingsPage.MAIN else showSettings = false
    }
    BackHandler(enabled = showSettings) { goBack() }

    // A share lands on Hash / Files; an open-with sums file lands on Verify.
    LaunchedEffect(sharedUris) {
        if (sharedUris.isNotEmpty()) { showSettings = false; pillar = Pillar.HASH.ordinal }
    }
    // The Quick Settings tile lands on Hash / Files, which then opens the picker.
    LaunchedEffect(pickFilesRequest) {
        if (pickFilesRequest > 0) { showSettings = false; pillar = Pillar.HASH.ordinal }
    }
    LaunchedEffect(verifyUri) {
        if (verifyUri != null) { showSettings = false; pillar = Pillar.VERIFY.ordinal }
    }

    Scaffold(
        containerColor = Ground,
        topBar = {
            HashPonyTopBar(
                inSettings = showSettings,
                onSettings = { settingsPage = SettingsPage.MAIN; showSettings = true },
                onBack = goBack,
                title = if (showSettings) settingsPage.titleRes?.let { stringResource(it) } else null,
            )
        },
        bottomBar = {
            if (!showSettings) {
                Column {
                    HorizontalDivider(thickness = 1.dp, color = LineSoft)
                    NavigationBar(containerColor = Ground, tonalElevation = 0.dp) {
                        Pillar.entries.forEachIndexed { index, p ->
                            val label = stringResource(p.labelRes)
                            NavigationBarItem(
                                selected = pillar == index,
                                onClick = { pillar = index },
                                icon = { Icon(p.icon, contentDescription = label) },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SilverBright,
                                    selectedTextColor = SilverBright,
                                    indicatorColor = PanelHigh,
                                    unselectedIconColor = TextFaint,
                                    unselectedTextColor = TextFaint,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                showSettings -> SettingsScreen(
                    settingsViewModel,
                    page = settingsPage,
                    onOpenPage = { settingsPage = it },
                )
                Pillar.entries[pillar] == Pillar.HASH ->
                    HashScreen(
                        filesViewModel, textViewModel, sharedUris, onSharedConsumed,
                        pickFilesRequest = pickFilesRequest,
                        onPickFilesConsumed = onPickFilesConsumed,
                    )
                else -> VerifyScreen(verifyViewModel, verifyUri, onVerifyConsumed)
            }
        }
    }
}
