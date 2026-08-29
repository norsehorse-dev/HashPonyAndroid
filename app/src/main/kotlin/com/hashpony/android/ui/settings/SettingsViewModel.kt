package com.hashpony.android.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hashpony.android.data.PreferencesRepository
import com.hashpony.android.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PreferencesRepository(app)

    val settings = prefs.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )

    fun setAlgorithmEnabled(id: String, enabled: Boolean) =
        viewModelScope.launch { prefs.setAlgorithmEnabled(id, enabled) }

    fun setUppercaseHex(value: Boolean) =
        viewModelScope.launch { prefs.setUppercaseHex(value) }

    fun setDynamicColor(value: Boolean) =
        viewModelScope.launch { prefs.setDynamicColor(value) }

    fun setThemeMode(mode: ThemeMode) =
        viewModelScope.launch { prefs.setThemeMode(mode) }
}
