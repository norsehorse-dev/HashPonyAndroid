package com.hashpony.android.ui.files

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.HashCancelledException
import com.hashpony.android.data.PreferencesRepository
import com.hashpony.android.hash.FileHashing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

enum class HashStatus { PENDING, HASHING, DONE, ERROR, CANCELLED }

data class FileUiState(
    val uri: Uri,
    val name: String,
    val size: Long?,
    val status: HashStatus,
    val progress: Float?, // 0..1, or null for indeterminate
    val results: Map<String, ByteArray>,
    val error: String? = null,
)

class FilesViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PreferencesRepository(app)
    private val resolver get() = getApplication<Application>().contentResolver

    val settings = prefs.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )

    private val _files = MutableStateFlow<List<FileUiState>>(emptyList())
    val files = _files.asStateFlow()

    private val _compareInput = MutableStateFlow("")
    val compareInput = _compareInput.asStateFlow()

    private var hashJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.settings.map { it.enabledAlgorithms }.distinctUntilChanged().collect {
                if (_files.value.isNotEmpty()) rehashAll()
            }
        }
    }

    fun setCompareInput(text: String) { _compareInput.value = text }

    /** Quick-toggle an algorithm from the Files tab (SHA-512/SHA-1/MD5). */
    fun setAlgorithmEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { prefs.setAlgorithmEnabled(id, enabled) }
    }

    fun addUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val existing = _files.value.map { it.uri }.toSet()
        val added = uris.filter { it !in existing }.map { uri ->
            val meta = FileHashing.queryMeta(resolver, uri)
            FileUiState(uri, meta.name, meta.size, HashStatus.PENDING, null, emptyMap())
        }
        if (added.isEmpty()) return
        _files.value = _files.value + added
        rehashAll()
    }

    fun clear() {
        hashJob?.cancel()
        _files.value = emptyList()
    }

    fun cancel() { hashJob?.cancel() }

    private fun currentAlgorithms(): List<String> {
        val enabled = settings.value?.enabledAlgorithms ?: PreferencesRepository.DEFAULT_ENABLED
        return AlgorithmRegistry.all.map { it.id }.filter { it in enabled }
    }

    private fun rehashAll() {
        hashJob?.cancel()
        val algorithms = currentAlgorithms()
        hashJob = viewModelScope.launch(Dispatchers.IO) {
            val count = _files.value.size
            for (index in 0 until count) {
                val file = _files.value.getOrNull(index) ?: continue
                updateAt(index) {
                    it.copy(
                        status = HashStatus.HASHING,
                        progress = if (file.size != null && file.size > 0) 0f else null,
                        error = null,
                    )
                }
                try {
                    val results = FileHashing.hash(
                        resolver, file.uri, algorithms,
                        onProgress = { bytes ->
                            val sz = file.size
                            if (sz != null && sz > 0) {
                                updateAt(index) { it.copy(progress = (bytes.toFloat() / sz).coerceIn(0f, 1f)) }
                            }
                        },
                        isCancelled = { !isActive },
                    )
                    updateAt(index) { it.copy(status = HashStatus.DONE, progress = 1f, results = results) }
                } catch (c: HashCancelledException) {
                    updateAt(index) { it.copy(status = HashStatus.CANCELLED, progress = null) }
                    return@launch
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    updateAt(index) { it.copy(status = HashStatus.ERROR, error = e.message ?: "Failed", progress = null) }
                }
            }
        }
    }

    private fun updateAt(index: Int, transform: (FileUiState) -> FileUiState) {
        _files.update { list ->
            if (index !in list.indices) list
            else list.toMutableList().also { it[index] = transform(it[index]) }
        }
    }
}
