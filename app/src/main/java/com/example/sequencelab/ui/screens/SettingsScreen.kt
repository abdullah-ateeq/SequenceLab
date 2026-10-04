package com.example.sequencelab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.sequencelab.data.AppSettings
import com.example.sequencelab.data.ThemeMode
import com.example.sequencelab.domain.GeneratorType
import com.example.sequencelab.domain.SequenceMode
import com.example.sequencelab.ui.SequenceLabViewModel
import com.example.sequencelab.ui.components.DropdownSelector
import com.example.sequencelab.ui.components.SectionLabel
import com.example.sequencelab.ui.theme.dynamicColorSupported

private enum class ConfirmAction { RESET_SETTINGS, CLEAR_HISTORY }

@Composable
fun SettingsScreen(viewModel: SequenceLabViewModel, settings: AppSettings) {
    val update = viewModel::updateSettings
    var confirm by rememberSaveable { mutableStateOf<ConfirmAction?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(Modifier.widthIn(max = 640.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(16.dp))

            Section("Appearance")
            Labeled("Theme") {
                Segmented(ThemeMode.entries, settings.theme, { it.label }) { t -> update { it.copy(theme = t) } }
            }
            SwitchRow(
                title = "Dynamic color",
                subtitle = if (dynamicColorSupported) "Use colors from your wallpaper" else "Requires Android 12 or later",
                checked = settings.dynamicColor && dynamicColorSupported,
                enabled = dynamicColorSupported,
            ) { v -> update { it.copy(dynamicColor = v) } }

            Section("Calculator")
            Labeled("Default calculator mode") {
                Segmented(SequenceMode.entries, settings.defaultMode, { if (it == SequenceMode.IDENTIFY) "Identify" else "Generate" }) { m ->
                    update { it.copy(defaultMode = m) }
                }
            }
            Labeled("Default generator") {
                DropdownSelector(
                    label = "Default generator",
                    selected = settings.defaultGenerator,
                    options = GeneratorType.entries,
                    optionLabel = { it.displayName },
                    onSelect = { g -> update { it.copy(defaultGenerator = g) } },
                )
            }
            Labeled("Default terms") {
                Chips(AppSettings.DEFAULT_TERM_OPTIONS, settings.defaultTerms) { n -> update { it.copy(defaultTerms = n) } }
            }
            Labeled("Decimal places") {
                Chips(AppSettings.DECIMAL_OPTIONS, settings.decimals) { n -> update { it.copy(decimals = n) } }
            }
            Labeled("Next terms") {
                Chips(AppSettings.NEXT_TERM_OPTIONS, settings.nextTerms) { n -> update { it.copy(nextTerms = n) } }
            }

            Section("Display & feedback")
            SwitchRow("Haptic feedback", "Light feedback on actions", settings.haptics) { v ->
                update { it.copy(haptics = v) }
            }
            SwitchRow("Show formula", "General formula for the pattern", settings.showFormula) { v ->
                update { it.copy(showFormula = v) }
            }
            SwitchRow("Show step explanation", "How each term follows from the rule", settings.showSteps) { v ->
                update { it.copy(showSteps = v) }
            }

            Section("Data")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                OutlinedButton(onClick = { confirm = ConfirmAction.RESET_SETTINGS }, modifier = Modifier.weight(1f)) {
                    Text("Reset settings")
                }
                OutlinedButton(onClick = { confirm = ConfirmAction.CLEAR_HISTORY }, modifier = Modifier.weight(1f)) {
                    Text("Clear history")
                }
            }
        }
    }

    confirm?.let { action ->
        val (title, text) = when (action) {
            ConfirmAction.RESET_SETTINGS -> "Reset settings?" to "All settings return to their defaults. History is kept."
            ConfirmAction.CLEAR_HISTORY -> "Clear history?" to "All saved results will be removed. This cannot be undone."
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    when (action) {
                        ConfirmAction.RESET_SETTINGS -> viewModel.resetSettings()
                        ConfirmAction.CLEAR_HISTORY -> viewModel.clearHistory()
                    }
                }) { Text(if (action == ConfirmAction.RESET_SETTINGS) "Reset" else "Clear") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(20.dp))
    SectionLabel(title, Modifier.semantics { heading() })
    Spacer(Modifier.height(4.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Labeled(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label(option)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.toString()) },
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
