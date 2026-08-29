package com.hashpony.android.ui.text

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hashpony.core.AlgorithmRegistry
import com.hashpony.core.HashEngine
import com.hashpony.android.data.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class TextViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PreferencesRepository(app)

    val settings = prefs.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )

    private val _input = MutableStateFlow("")
    val input = _input.asStateFlow()

    /** Live digests for the enabled algorithms over the UTF-8 bytes of the input. */
    val results = combine(_input, prefs.settings) { text, s -> text to s.enabledAlgorithms }
        .map { (text, enabled) ->
            withContext(Dispatchers.Default) { compute(text, enabled) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setInput(text: String) { _input.value = text }

    private fun compute(text: String, enabled: Set<String>): Map<String, ByteArray> {
        val ids = AlgorithmRegistry.all.map { it.id }.filter { it in enabled }
        val digests = ids.mapNotNull { id -> AlgorithmRegistry.byId(id)?.let { id to it.newDigest() } }.toMap()
        if (digests.isEmpty()) return emptyMap()
        return HashEngine.hashBytes(text.toByteArray(Charsets.UTF_8), digests)
    }
}
