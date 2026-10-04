package com.example.sequencelab.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sequencelab.data.AppSettings
import com.example.sequencelab.data.HistoryRecord
import com.example.sequencelab.data.HistoryRepository
import com.example.sequencelab.data.SettingsRepository
import com.example.sequencelab.data.historyDataStore
import com.example.sequencelab.data.settingsDataStore
import com.example.sequencelab.domain.GenerateOutcome
import com.example.sequencelab.domain.GeneratedSequence
import com.example.sequencelab.domain.GeneratorField
import com.example.sequencelab.domain.GeneratorInputs
import com.example.sequencelab.domain.GeneratorType
import com.example.sequencelab.domain.NumberFormatter
import com.example.sequencelab.domain.ParseResult
import com.example.sequencelab.domain.PatternText
import com.example.sequencelab.domain.SequenceGenerator
import com.example.sequencelab.domain.SequenceLabCalculator
import com.example.sequencelab.domain.SequenceMode
import com.example.sequencelab.domain.SequenceParser
import com.example.sequencelab.domain.SequenceResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class IdentifyState(
    val input: String = "",
    val error: String? = null,
    val result: SequenceResult? = null,
)

data class GenerateState(
    val inputs: GeneratorInputs = GeneratorInputs(),
    val errors: Map<GeneratorField, String> = emptyMap(),
    val result: GeneratedSequence? = null,
)

data class CalculatorState(
    val mode: SequenceMode = SequenceMode.IDENTIFY,
    val identify: IdentifyState = IdentifyState(),
    val generate: GenerateState = GenerateState(),
)

class SequenceLabViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application.settingsDataStore)
    private val historyRepository = HistoryRepository(application.historyDataStore)

    /** Null until the first read from DataStore completes, so the theme never flashes. */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val history: StateFlow<List<HistoryRecord>> = historyRepository.records
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _calculator = MutableStateFlow(CalculatorState())
    val calculator: StateFlow<CalculatorState> = _calculator.asStateFlow()

    private val messages = Channel<String>(Channel.BUFFERED)
    val snackbarMessages: Flow<String> = messages.receiveAsFlow()

    private val current: AppSettings get() = settings.value ?: AppSettings()

    init {
        viewModelScope.launch {
            val initial = settingsRepository.settings.first()
            _calculator.update {
                it.copy(
                    mode = initial.defaultMode,
                    generate = GenerateState(
                        inputs = GeneratorInputs.defaults(initial.defaultGenerator, initial.defaultTerms),
                    ),
                )
            }
            generate()
            // Keep the identified next terms in step with the "Next terms" setting.
            var nextTerms = initial.nextTerms
            settingsRepository.settings.collect { s ->
                if (s.nextTerms != nextTerms) {
                    nextTerms = s.nextTerms
                    _calculator.value.identify.result?.let { runIdentify(it.sequence, s.nextTerms) }
                }
            }
        }
    }

    fun setMode(mode: SequenceMode) = _calculator.update { it.copy(mode = mode) }

    // region Identify

    fun onSequenceInput(text: String) = _calculator.update {
        it.copy(identify = it.identify.copy(input = text, error = null))
    }

    fun identify() {
        when (val parsed = SequenceParser.parse(_calculator.value.identify.input)) {
            is ParseResult.Error -> _calculator.update {
                it.copy(identify = it.identify.copy(error = parsed.message, result = null))
            }
            is ParseResult.Success -> runIdentify(parsed.values, current.nextTerms)
        }
    }

    private fun runIdentify(values: List<BigDecimal>, nextTerms: Int) {
        val result = SequenceLabCalculator.identifySequence(values, nextTerms)
        _calculator.update { it.copy(identify = it.identify.copy(error = null, result = result)) }
    }

    fun resetIdentify() = _calculator.update { it.copy(identify = IdentifyState()) }

    // endregion

    // region Generate

    fun setGeneratorType(type: GeneratorType) = _calculator.update {
        it.copy(generate = it.generate.copy(inputs = it.generate.inputs.copy(type = type), errors = emptyMap(), result = null))
    }

    fun onGeneratorInput(field: GeneratorField, value: String) = _calculator.update {
        it.copy(generate = it.generate.copy(inputs = it.generate.inputs.with(field, value), errors = it.generate.errors - field))
    }

    fun generate() {
        when (val outcome = SequenceGenerator.generate(_calculator.value.generate.inputs)) {
            is GenerateOutcome.Invalid -> _calculator.update {
                it.copy(generate = it.generate.copy(errors = outcome.errors, result = null))
            }
            is GenerateOutcome.Success -> _calculator.update {
                it.copy(generate = it.generate.copy(errors = emptyMap(), result = outcome.result))
            }
        }
    }

    fun resetGenerator() {
        _calculator.update {
            it.copy(generate = GenerateState(GeneratorInputs.defaults(current.defaultGenerator, current.defaultTerms)))
        }
        generate()
    }

    // endregion

    // region Copy / save / history

    fun copyText(): String? {
        val state = _calculator.value
        val decimals = current.decimals
        return when (state.mode) {
            SequenceMode.IDENTIFY -> state.identify.result?.let { PatternText.identifyCopyText(it, decimals) }
            SequenceMode.GENERATE -> state.generate.result?.let { PatternText.generateCopyText(it, decimals) }
        }
    }

    fun showMessage(message: String) {
        messages.trySend(message)
    }

    fun save() {
        val state = _calculator.value
        val decimals = current.decimals
        val now = System.currentTimeMillis()
        val record = when (state.mode) {
            SequenceMode.IDENTIFY -> state.identify.result?.let { r ->
                HistoryRecord(
                    id = now,
                    timestamp = now,
                    mode = SequenceMode.IDENTIFY,
                    patternType = r.type,
                    title = PatternText.name(r.pattern),
                    rule = r.pattern?.let { PatternText.rule(it, decimals) }
                        ?: "Differences: " + PatternText.observedDifferences(r.differences, decimals),
                    sequence = r.sequence,
                    nextTerms = r.nextTerms,
                    generatorInputs = null,
                )
            }
            SequenceMode.GENERATE -> state.generate.result?.let { r ->
                HistoryRecord(
                    id = now,
                    timestamp = now,
                    mode = SequenceMode.GENERATE,
                    patternType = r.pattern.type,
                    title = r.inputs.type.displayName,
                    rule = PatternText.rule(r.pattern, decimals),
                    sequence = r.terms,
                    nextTerms = emptyList(),
                    generatorInputs = r.inputs,
                )
            }
        } ?: return
        viewModelScope.launch {
            historyRepository.add(record)
            messages.send("Result saved")
        }
    }

    /** Restores a saved record into the calculator and recalculates it. */
    fun restore(record: HistoryRecord) {
        when (record.mode) {
            SequenceMode.IDENTIFY -> {
                _calculator.update {
                    it.copy(
                        mode = SequenceMode.IDENTIFY,
                        identify = IdentifyState(input = record.sequence.joinToString(", ") { v -> NumberFormatter.plain(v) }),
                    )
                }
                identify()
            }
            SequenceMode.GENERATE -> {
                val inputs = record.generatorInputs ?: return
                _calculator.update { it.copy(mode = SequenceMode.GENERATE, generate = GenerateState(inputs)) }
                generate()
            }
        }
    }

    fun deleteRecord(id: Long) = viewModelScope.launch { historyRepository.delete(id) }

    fun clearHistory() = viewModelScope.launch {
        historyRepository.clear()
        messages.send("History cleared")
    }

    // endregion

    // region Settings

    fun updateSettings(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        settingsRepository.update(transform)
    }

    fun resetSettings() = viewModelScope.launch {
        settingsRepository.reset()
        messages.send("Settings reset")
    }

    // endregion
}
