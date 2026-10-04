package com.example.sequencelab.ui.screens

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sequencelab.R
import com.example.sequencelab.data.AppSettings
import com.example.sequencelab.domain.GeneratedSequence
import com.example.sequencelab.domain.GeneratorField
import com.example.sequencelab.domain.GeneratorType
import com.example.sequencelab.domain.NumberFormatter
import com.example.sequencelab.domain.PatternText
import com.example.sequencelab.domain.SequenceLabCalculator
import com.example.sequencelab.domain.SequenceMode
import com.example.sequencelab.domain.SequenceResult
import com.example.sequencelab.domain.fields
import com.example.sequencelab.ui.GenerateState
import com.example.sequencelab.ui.IdentifyState
import com.example.sequencelab.ui.SequenceLabViewModel
import com.example.sequencelab.ui.components.DropdownSelector
import com.example.sequencelab.ui.components.FormulaText
import com.example.sequencelab.ui.components.SectionLabel
import com.example.sequencelab.ui.components.SequenceFlow
import com.example.sequencelab.ui.components.StepsView
import com.example.sequencelab.ui.rememberHaptic
import kotlinx.coroutines.launch

private const val MAX_DISPLAYED_TERMS = 50
private val EXAMPLES = listOf("2, 4, 6, 8", "2, 4, 8, 16", "1, 1, 2, 3, 5", "1, 4, 9, 16")

@Composable
fun CalculatorScreen(viewModel: SequenceLabViewModel, settings: AppSettings) {
    val state by viewModel.calculator.collectAsStateWithLifecycle()
    val haptic = rememberHaptic(settings.haptics)
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    val onCopy: () -> Unit = {
        viewModel.copyText()?.let { text ->
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("SequenceLab", text)))
                viewModel.showMessage("Sequence copied")
            }
        }
    }
    val onSave: () -> Unit = {
        haptic()
        viewModel.save()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Header()
            Spacer(Modifier.height(20.dp))
            ModeSelector(state.mode, viewModel::setMode, Modifier.widthIn(max = 480.dp))
            Spacer(Modifier.height(20.dp))

            val inputs: @Composable () -> Unit = {
                when (state.mode) {
                    SequenceMode.IDENTIFY -> IdentifyInputs(
                        state = state.identify,
                        onInput = viewModel::onSequenceInput,
                        onIdentify = {
                            focus.clearFocus()
                            haptic()
                            viewModel.identify()
                        },
                        onExample = {
                            viewModel.onSequenceInput(it)
                            haptic()
                            viewModel.identify()
                        },
                        onReset = {
                            haptic()
                            viewModel.resetIdentify()
                        },
                    )
                    SequenceMode.GENERATE -> GenerateInputs(
                        state = state.generate,
                        onType = viewModel::setGeneratorType,
                        onInput = viewModel::onGeneratorInput,
                        onGenerate = {
                            focus.clearFocus()
                            haptic()
                            viewModel.generate()
                        },
                        onReset = {
                            haptic()
                            viewModel.resetGenerator()
                        },
                    )
                }
            }
            val result: @Composable () -> Unit = {
                when (state.mode) {
                    SequenceMode.IDENTIFY -> state.identify.result?.let {
                        IdentifyResult(it, settings, onCopy, onSave)
                    }
                    SequenceMode.GENERATE -> state.generate.result?.let {
                        GenerateResult(it, settings, onCopy, onSave)
                    }
                }
            }

            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f)) { inputs() }
                    Column(Modifier.weight(1f)) { result() }
                }
            } else {
                inputs()
                Spacer(Modifier.height(20.dp))
                result()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Header() {
    Column {
        Text(
            "SEQUENCELAB",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 2,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Identify or generate simple number patterns.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(mode: SequenceMode, onMode: (SequenceMode) -> Unit, modifier: Modifier = Modifier) {
    val modes = SequenceMode.entries
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, item ->
            SegmentedButton(
                selected = mode == item,
                onClick = { onMode(item) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                label = { Text(if (item == SequenceMode.IDENTIFY) "Identify" else "Generate") },
            )
        }
    }
}

// region Identify

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentifyInputs(
    state: IdentifyState,
    onInput: (String) -> Unit,
    onIdentify: () -> Unit,
    onExample: (String) -> Unit,
    onReset: () -> Unit,
) {
    SectionLabel("Sequence")
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = state.input,
        onValueChange = onInput,
        placeholder = { Text("2, 4, 6, 8") },
        minLines = 2,
        maxLines = 6,
        isError = state.error != null,
        supportingText = {
            Text(state.error ?: "Separate with commas, spaces or new lines · 2–50 numbers")
        },
        trailingIcon = if (state.input.isNotEmpty()) {
            {
                IconButton(onClick = { onInput("") }) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear sequence")
                }
            }
        } else {
            null
        },
        textStyle = MaterialTheme.typography.titleMedium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onIdentify() }),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Sequence input" },
    )
    Spacer(Modifier.height(12.dp))
    PrimaryActions(
        primaryLabel = "Identify",
        primaryDescription = "Identify sequence",
        onPrimary = onIdentify,
        onReset = onReset,
    )
    if (state.result == null) {
        Spacer(Modifier.height(16.dp))
        SectionLabel("Try an example")
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EXAMPLES.forEach { example ->
                SuggestionChip(onClick = { onExample(example) }, label = { Text(example) })
            }
        }
    }
}

@Composable
private fun IdentifyResult(
    result: SequenceResult,
    settings: AppSettings,
    onCopy: () -> Unit,
    onSave: () -> Unit,
) {
    val d = settings.decimals
    val pattern = result.pattern
    ResultCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Pattern", Modifier.weight(1f))
            if (pattern != null && !result.confidence) Tag(PatternText.PLAUSIBLE)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            PatternText.name(pattern),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics {
                contentDescription = pattern?.let { "Detected ${it.type.displayName.lowercase()} sequence" }
                    ?: PatternText.NO_PATTERN
            },
        )

        if (pattern == null) {
            if (result.differences.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("Observed differences")
                Spacer(Modifier.height(4.dp))
                Text(PatternText.observedDifferences(result.differences, d), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            if (SequenceLabCalculator.moreTermsMayHelp(result.sequence)) {
                Text(PatternText.MORE_TERMS, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Only common simple patterns are checked; the sequence may still follow another rule.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(Modifier.height(16.dp))
            SectionLabel("Rule")
            Spacer(Modifier.height(4.dp))
            RuleRow(PatternText.shortRule(pattern, d), PatternText.rule(pattern, d))
            if (settings.showFormula) {
                Spacer(Modifier.height(12.dp))
                FormulaText(PatternText.formula(pattern, d))
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("Next terms")
            Spacer(Modifier.height(6.dp))
            SequenceFlow(
                terms = result.sequence.takeLast(4).map { NumberFormatter.format(it, d) }.let {
                    if (result.sequence.size > 4) listOf("…") + it else it
                },
                highlighted = result.nextTerms.map { NumberFormatter.format(it, d) },
            )
        }
        ResultActions(onCopy, onSave)
    }
    if (pattern != null && settings.showSteps) {
        PatternText.steps(pattern, result.sequence, d)?.let { steps ->
            Spacer(Modifier.height(16.dp))
            SectionLabel("Steps")
            Spacer(Modifier.height(8.dp))
            StepsView(steps, Modifier.padding(horizontal = 4.dp))
        }
    }
}

// endregion

// region Generate

@Composable
private fun GenerateInputs(
    state: GenerateState,
    onType: (GeneratorType) -> Unit,
    onInput: (GeneratorField, String) -> Unit,
    onGenerate: () -> Unit,
    onReset: () -> Unit,
) {
    SectionLabel("Sequence type")
    Spacer(Modifier.height(8.dp))
    DropdownSelector(
        label = "Sequence type",
        selected = state.inputs.type,
        options = GeneratorType.entries,
        optionLabel = { it.displayName },
        onSelect = onType,
    )
    Spacer(Modifier.height(16.dp))
    state.inputs.type.fields().chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { field ->
                GeneratorTextField(
                    field = field,
                    value = state.inputs[field],
                    error = state.errors[field],
                    onValueChange = { onInput(field, it) },
                    onDone = onGenerate,
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(8.dp))
    PrimaryActions(
        primaryLabel = "Generate",
        primaryDescription = "Generate sequence",
        onPrimary = onGenerate,
        onReset = onReset,
    )
}

@Composable
private fun GeneratorTextField(
    field: GeneratorField,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wholeNumber = field == GeneratorField.TERMS || field == GeneratorField.START_N
    OutlinedTextField(
        value = value,
        onValueChange = { text -> if (text.length <= 20) onValueChange(text) },
        label = { Text(field.label) },
        singleLine = true,
        isError = error != null,
        supportingText = { Text(error ?: if (field == GeneratorField.TERMS) "2–100" else "") },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (wholeNumber) KeyboardType.Number else KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        textStyle = MaterialTheme.typography.titleMedium,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.semantics { contentDescription = field.description },
    )
}

@Composable
private fun GenerateResult(
    result: GeneratedSequence,
    settings: AppSettings,
    onCopy: () -> Unit,
    onSave: () -> Unit,
) {
    val d = settings.decimals
    ResultCard {
        SectionLabel("Generated sequence")
        Spacer(Modifier.height(8.dp))
        SequenceFlow(
            terms = result.terms.take(MAX_DISPLAYED_TERMS).map { NumberFormatter.format(it, d) },
            large = result.terms.size <= 12,
        )
        if (result.terms.size > MAX_DISPLAYED_TERMS) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Showing the first $MAX_DISPLAYED_TERMS of ${result.terms.size} terms. Copy includes all terms.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Row {
            Column(Modifier.weight(1f)) {
                SectionLabel("Rule")
                Spacer(Modifier.height(4.dp))
                Text(PatternText.rule(result.pattern, d), style = MaterialTheme.typography.titleMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                SectionLabel("Terms")
                Spacer(Modifier.height(4.dp))
                Text(result.terms.size.toString(), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (settings.showFormula) {
            Spacer(Modifier.height(12.dp))
            FormulaText(PatternText.formula(result.pattern, d))
        }
        ResultActions(onCopy, onSave)
    }
    if (settings.showSteps) {
        PatternText.steps(result.pattern, result.terms, d)?.let { steps ->
            Spacer(Modifier.height(16.dp))
            SectionLabel("Steps")
            Spacer(Modifier.height(8.dp))
            StepsView(steps, Modifier.padding(horizontal = 4.dp))
        }
    }
}

// endregion

// region Shared pieces

@Composable
private fun PrimaryActions(
    primaryLabel: String,
    primaryDescription: String,
    onPrimary: () -> Unit,
    onReset: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onPrimary,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .semantics { contentDescription = primaryDescription },
        ) {
            Text(primaryLabel.uppercase(), letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing)
        }
        OutlinedButton(
            onClick = onReset,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .height(52.dp)
                .semantics { contentDescription = "Reset calculator" },
        ) {
            Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Reset")
        }
    }
}

@Composable
private fun ResultCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun RuleRow(short: String?, full: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (short != null && short != full) {
            Text(short, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(full, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun Tag(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ResultActions(onCopy: () -> Unit, onSave: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(
            onClick = onCopy,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Copy sequence" },
        ) {
            Icon(painterResource(R.drawable.ic_copy), contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Copy")
        }
        FilledTonalButton(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Save result" },
        ) {
            Icon(painterResource(R.drawable.ic_bookmark), contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Save")
        }
    }
}

// endregion
