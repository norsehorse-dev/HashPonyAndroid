package com.hashpony.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hashpony.core.AlgorithmRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "hashpony_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Everything the user can persist. */
data class AppSettings(
    val enabledAlgorithms: Set<String>,
    val uppercaseHex: Boolean,
    val dynamicColor: Boolean,
    val themeMode: ThemeMode,
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val ENABLED = stringSetPreferencesKey("enabled_algorithms")
        val UPPERCASE = booleanPreferencesKey("uppercase_hex")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val THEME = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            enabledAlgorithms = p[Keys.ENABLED] ?: DEFAULT_ENABLED,
            uppercaseHex = p[Keys.UPPERCASE] ?: false,
            dynamicColor = p[Keys.DYNAMIC] ?: true,
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        )
    }

    suspend fun setAlgorithmEnabled(id: String, enabled: Boolean) {
        context.dataStore.edit { p ->
            val current = (p[Keys.ENABLED] ?: DEFAULT_ENABLED).toMutableSet()
            if (enabled) current.add(id) else current.remove(id)
            p[Keys.ENABLED] = current
        }
    }

    suspend fun setUppercaseHex(value: Boolean) {
        context.dataStore.edit { it[Keys.UPPERCASE] = value }
    }

    suspend fun setDynamicColor(value: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC] = value }
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = value.name }
    }

    companion object {
        /** Only SHA-256 is enabled out of the box. */
        val DEFAULT_ENABLED: Set<String> = AlgorithmRegistry.defaults.map { it.id }.toSet()
    }
}
