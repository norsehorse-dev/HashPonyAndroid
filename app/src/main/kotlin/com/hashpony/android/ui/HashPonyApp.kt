package com.hashpony.android.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hashpony.android.ui.files.FilesScreen
import com.hashpony.android.ui.files.FilesViewModel
import com.hashpony.android.ui.settings.SettingsScreen
import com.hashpony.android.ui.settings.SettingsViewModel
import com.hashpony.android.ui.text.TextScreen
import com.hashpony.android.ui.text.TextViewModel
import com.hashpony.android.ui.verify.VerifyScreen
import com.hashpony.android.ui.verify.VerifyViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    FILES("Files", Icons.Filled.Description),
    TEXT("Text", Icons.Filled.TextFields),
    VERIFY("Verify", Icons.Filled.FactCheck),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@Composable
fun HashPonyApp(
    sharedUris: List<Uri>,
    onSharedConsumed: () -> Unit,
    verifyUri: Uri?,
    onVerifyConsumed: () -> Unit,
) {
    val filesViewModel: FilesViewModel = viewModel()
    val textViewModel: TextViewModel = viewModel()
    val verifyViewModel: VerifyViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    var selected by rememberSaveable { mutableIntStateOf(0) }

    // A share always lands on the Files tab.
    LaunchedEffect(sharedUris) {
        if (sharedUris.isNotEmpty()) selected = Tab.FILES.ordinal
    }
    // Open-with of a sums file lands on the Verify tab.
    LaunchedEffect(verifyUri) {
        if (verifyUri != null) selected = Tab.VERIFY.ordinal
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (Tab.entries[selected]) {
                Tab.FILES -> FilesScreen(filesViewModel, sharedUris, onSharedConsumed)
                Tab.TEXT -> TextScreen(textViewModel)
                Tab.VERIFY -> VerifyScreen(verifyViewModel, verifyUri, onVerifyConsumed)
                Tab.SETTINGS -> SettingsScreen(settingsViewModel)
            }
        }
    }
}
