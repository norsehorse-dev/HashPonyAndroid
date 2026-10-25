package com.hashpony.android.ui.hash

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hashpony.android.R
import com.hashpony.android.ui.files.FilesScreen
import com.hashpony.android.ui.files.FilesViewModel
import com.hashpony.android.ui.text.TextScreen
import com.hashpony.android.ui.text.TextViewModel
import com.hashpony.android.ui.theme.SourceSwitcher

/**
 * The Hash pillar: one surface with a source switcher. Files hosts the file and
 * folder pickers and the share-in flow; Text hashes live as you type. This folds
 * the old standalone Text tab under Hash.
 */
@Composable
fun HashScreen(
    filesViewModel: FilesViewModel,
    textViewModel: TextViewModel,
    sharedUris: List<Uri>,
    onSharedConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    pickFilesRequest: Int = 0,
    onPickFilesConsumed: () -> Unit = {},
) {
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 = Files, 1 = Text

    // A share, or the Quick Settings tile, always lands on the Files source.
    LaunchedEffect(sharedUris) {
        if (sharedUris.isNotEmpty()) mode = 0
    }
    LaunchedEffect(pickFilesRequest) {
        if (pickFilesRequest > 0) mode = 0
    }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            SourceSwitcher(
                options = listOf(stringResource(R.string.tab_files), stringResource(R.string.tab_text)),
                selected = mode,
                onSelect = { mode = it },
            )
        }
        Spacer(Modifier.height(4.dp))
        when (mode) {
            0 -> FilesScreen(
                filesViewModel, sharedUris, onSharedConsumed,
                pickFilesRequest = pickFilesRequest,
                onPickFilesConsumed = onPickFilesConsumed,
            )
            else -> TextScreen(textViewModel)
        }
    }
}
