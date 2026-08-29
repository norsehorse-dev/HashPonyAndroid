package com.hashpony.android.ui.verify

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hashpony.core.sums.ParsedSums
import com.hashpony.core.sums.SumsParser
import com.hashpony.core.sums.SumsVerifier
import com.hashpony.core.sums.VerifyReport
import com.hashpony.android.data.PreferencesRepository
import com.hashpony.android.hash.FileHashing
import com.hashpony.android.hash.SafTree
import com.hashpony.android.hash.SumsCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface VerifyUiState {
    data object Idle : VerifyUiState
    data object Working : VerifyUiState
    data class FolderScanned(val folderName: String, val candidates: List<SumsCandidate>) : VerifyUiState
    data class AwaitingFolder(val sumsName: String) : VerifyUiState
    data class NeedAlgorithm(val sumsName: String, val candidates: List<String>) : VerifyUiState
    data class Results(val sumsName: String, val report: VerifyReport) : VerifyUiState
    data class Failed(val message: String) : VerifyUiState
}

class VerifyViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PreferencesRepository(app)
    private val resolver get() = getApplication<Application>().contentResolver
    private val settings = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _state = MutableStateFlow<VerifyUiState>(VerifyUiState.Idle)
    val state = _state.asStateFlow()

    // Retained across an algorithm prompt or a sums-first folder grant.
    private var currentTree: SafTree? = null
    private var pendingParsed: ParsedSums? = null
    private var pendingSumsName: String? = null
    private var algorithmOverride: String? = null

    fun reset() {
        _state.value = VerifyUiState.Idle
        currentTree = null; pendingParsed = null; pendingSumsName = null; algorithmOverride = null
    }

    private fun enabled(): Set<String> = settings.value?.enabledAlgorithms ?: PreferencesRepository.DEFAULT_ENABLED

    // ---- Folder-first (primary) ----

    fun scanFolder(treeUri: Uri) {
        takePersistable(treeUri)
        _state.value = VerifyUiState.Working
        viewModelScope.launch {
            try {
                val folderName = FileHashing.queryMeta(resolver, treeUri).name
                val tree = withContext(Dispatchers.IO) { SafTree.index(resolver, treeUri) }
                currentTree = tree
                _state.value = VerifyUiState.FolderScanned(folderName, tree.sumsCandidates)
            } catch (e: Exception) {
                _state.value = VerifyUiState.Failed(e.message ?: "Could not read that folder")
            }
        }
    }

    fun verifyCandidate(candidate: SumsCandidate) {
        val tree = currentTree ?: return
        algorithmOverride = null
        _state.value = VerifyUiState.Working
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) { readText(candidate.uri) }
                    ?: return@launch fail("Could not open ${candidate.name}")
                val parsed = SumsParser.parse(text, candidate.name)
                runVerify(parsed, candidate.name, tree)
            } catch (e: Exception) {
                fail(e.message ?: "Verification failed")
            }
        }
    }

    // ---- Sums-file-first (secondary): open-with or explicit pick ----

    fun openSumsFile(sumsUri: Uri) {
        algorithmOverride = null
        _state.value = VerifyUiState.Working
        viewModelScope.launch {
            try {
                val name = FileHashing.queryMeta(resolver, sumsUri).name
                val text = withContext(Dispatchers.IO) { readText(sumsUri) }
                    ?: return@launch fail("Could not open $name")
                pendingParsed = SumsParser.parse(text, name)
                pendingSumsName = name
                _state.value = VerifyUiState.AwaitingFolder(name)
            } catch (e: Exception) {
                fail(e.message ?: "Could not read that sums file")
            }
        }
    }

    /** Sums-first: the user has now granted the containing folder. */
    fun provideFolderForSums(treeUri: Uri) {
        val parsed = pendingParsed ?: return
        val name = pendingSumsName ?: return
        takePersistable(treeUri)
        _state.value = VerifyUiState.Working
        viewModelScope.launch {
            try {
                val tree = withContext(Dispatchers.IO) { SafTree.index(resolver, treeUri) }
                currentTree = tree
                runVerify(parsed, name, tree)
            } catch (e: Exception) {
                fail(e.message ?: "Could not read that folder")
            }
        }
    }

    /** The user picked an algorithm for an ambiguous sums file; re-verify with it. */
    fun chooseAlgorithm(id: String) {
        val parsed = pendingParsed ?: return
        val name = pendingSumsName ?: return
        val tree = currentTree ?: return
        algorithmOverride = id
        _state.value = VerifyUiState.Working
        viewModelScope.launch { runVerify(parsed, name, tree) }
    }

    private suspend fun runVerify(parsed: ParsedSums, sumsName: String, tree: SafTree) {
        val override = algorithmOverride
        val ambiguous = LinkedHashSet<String>()
        val report = withContext(Dispatchers.IO) {
            SumsVerifier.verify(
                parsed, sumsName, enabled(),
                resolver = { name -> tree.open(name) },
                chooseAlgorithm = { _, candidates ->
                    ambiguous.addAll(candidates)
                    if (override != null && override in candidates) override else null
                },
            )
        }
        // Remember context in case we need to ask, then re-run.
        pendingParsed = parsed; pendingSumsName = sumsName; currentTree = tree
        if (override == null && report.unsupported > 0 && ambiguous.isNotEmpty()) {
            _state.value = VerifyUiState.NeedAlgorithm(sumsName, ambiguous.toList())
        } else {
            _state.value = VerifyUiState.Results(sumsName, report)
        }
    }

    private fun fail(message: String) { _state.value = VerifyUiState.Failed(message) }

    private fun readText(uri: Uri): String? =
        runCatching { resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()

    private fun takePersistable(treeUri: Uri) {
        runCatching {
            resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
