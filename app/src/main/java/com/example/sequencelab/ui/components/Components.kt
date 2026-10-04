package com.example.sequencelab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sequencelab.R
import com.example.sequencelab.domain.StepExplanation
import com.example.sequencelab.ui.theme.FormulaStyle

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Sequence as a wrapping "a → b → c" flow. [highlighted] terms (next terms) are drawn as accent pills. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SequenceFlow(
    terms: List<String>,
    modifier: Modifier = Modifier,
    highlighted: List<String> = emptyList(),
    large: Boolean = false,
) {
    val style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium
    val all = terms.map { it to false } + highlighted.map { it to true }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        all.forEachIndexed { index, (term, isNext) ->
            if (index > 0) {
                Text(
                    "→",
                    style = style,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
            if (isNext) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.semantics { contentDescription = "Next term $term" },
                ) {
                    Text(
                        term,
                        style = style,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            } else {
                Text(term, style = style, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun FormulaText(formula: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.semantics { contentDescription = "Formula: $formula" },
    ) {
        Text(
            formula,
            style = FormulaStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepsView(steps: StepExplanation, modifier: Modifier = Modifier) {
    when (steps) {
        is StepExplanation.Flow -> FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            steps.tokens.forEachIndexed { index, token ->
                if (index > 0) Text("→", color = MaterialTheme.colorScheme.outline)
                if (token.isOperator) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            token.text,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                } else {
                    Text(token.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is StepExplanation.Lines -> Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            steps.lines.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** Read-only field that opens a [DropdownMenu]; avoids the experimental exposed-dropdown APIs. */
@Composable
fun <T> DropdownSelector(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    stateDescription = optionLabel(selected)
                    role = Role.DropdownList
                },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            ) {
                Text(
                    optionLabel(selected),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Icon(painterResource(R.drawable.ic_arrow_drop_down), contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    trailingIcon = if (option == selected) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = "Selected", Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
