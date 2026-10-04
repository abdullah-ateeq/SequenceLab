package com.example.sequencelab.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.sequencelab.domain.GeneratorType
import com.example.sequencelab.domain.SequenceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val defaultMode: SequenceMode = SequenceMode.IDENTIFY,
    val defaultGenerator: GeneratorType = GeneratorType.ARITHMETIC,
    val defaultTerms: Int = 6,
    val decimals: Int = 3,
    val nextTerms: Int = 3,
    val haptics: Boolean = true,
    val showFormula: Boolean = true,
    val showSteps: Boolean = true,
) {
    companion object {
        val DECIMAL_OPTIONS = listOf(1, 2, 3, 4, 6)
        val NEXT_TERM_OPTIONS = listOf(3, 5, 10)
        val DEFAULT_TERM_OPTIONS = listOf(5, 6, 8, 10, 20)
    }
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map(::fromPreferences)

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs -> write(prefs, transform(fromPreferences(prefs))) }
    }

    suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_MODE = stringPreferencesKey("default_mode")
        val DEFAULT_GENERATOR = stringPreferencesKey("default_generator")
        val DEFAULT_TERMS = intPreferencesKey("default_terms")
        val DECIMALS = intPreferencesKey("decimals")
        val NEXT_TERMS = intPreferencesKey("next_terms")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SHOW_FORMULA = booleanPreferencesKey("show_formula")
        val SHOW_STEPS = booleanPreferencesKey("show_steps")
    }

    companion object {
        private val defaults = AppSettings()

        fun fromPreferences(prefs: Preferences) = AppSettings(
            theme = enumOrDefault(prefs[Keys.THEME], defaults.theme),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            defaultMode = enumOrDefault(prefs[Keys.DEFAULT_MODE], defaults.defaultMode),
            defaultGenerator = enumOrDefault(prefs[Keys.DEFAULT_GENERATOR], defaults.defaultGenerator),
            defaultTerms = prefs[Keys.DEFAULT_TERMS]?.takeIf { it in 2..100 } ?: defaults.defaultTerms,
            decimals = prefs[Keys.DECIMALS]?.takeIf { it in AppSettings.DECIMAL_OPTIONS } ?: defaults.decimals,
            nextTerms = prefs[Keys.NEXT_TERMS]?.takeIf { it in AppSettings.NEXT_TERM_OPTIONS } ?: defaults.nextTerms,
            haptics = prefs[Keys.HAPTICS] ?: defaults.haptics,
            showFormula = prefs[Keys.SHOW_FORMULA] ?: defaults.showFormula,
            showSteps = prefs[Keys.SHOW_STEPS] ?: defaults.showSteps,
        )

        private fun write(prefs: MutablePreferences, s: AppSettings) {
            prefs[Keys.THEME] = s.theme.name
            prefs[Keys.DYNAMIC_COLOR] = s.dynamicColor
            prefs[Keys.DEFAULT_MODE] = s.defaultMode.name
            prefs[Keys.DEFAULT_GENERATOR] = s.defaultGenerator.name
            prefs[Keys.DEFAULT_TERMS] = s.defaultTerms
            prefs[Keys.DECIMALS] = s.decimals
            prefs[Keys.NEXT_TERMS] = s.nextTerms
            prefs[Keys.HAPTICS] = s.haptics
            prefs[Keys.SHOW_FORMULA] = s.showFormula
            prefs[Keys.SHOW_STEPS] = s.showSteps
        }

        private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
