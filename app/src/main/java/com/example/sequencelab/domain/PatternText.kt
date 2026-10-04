package com.example.sequencelab.domain

import java.math.BigDecimal

data class FlowToken(val text: String, val isOperator: Boolean)

sealed interface StepExplanation {
    data class Flow(val tokens: List<FlowToken>) : StepExplanation
    data class Lines(val lines: List<String>) : StepExplanation
}

/** Human-readable names, rules, formulas and step explanations for patterns. */
object PatternText {
    private const val MAX_STEP_TERMS = 6

    fun name(pattern: SequencePattern?): String = pattern?.type?.displayName ?: NO_PATTERN

    const val NO_PATTERN = "No simple pattern detected"
    const val PLAUSIBLE = "Plausible simple pattern"
    const val MORE_TERMS = "Try entering more terms."

    fun rule(pattern: SequencePattern, decimals: Int): String {
        fun f(v: BigDecimal) = NumberFormatter.format(v, decimals)
        return when (pattern) {
            is SequencePattern.Constant -> "Add 0"
            is SequencePattern.Arithmetic ->
                if (pattern.difference.signum() < 0) "Subtract ${f(pattern.difference.abs())}"
                else "Add ${f(pattern.difference)}"
            is SequencePattern.Geometric ->
                pattern.divisor?.let { "Divide by ${f(it)}" } ?: "Multiply by ${f(pattern.ratio)}"
            SequencePattern.Fibonacci -> "Add the previous two terms"
            is SequencePattern.Squares -> "n²" + startNote(pattern.startN)
            is SequencePattern.Cubes -> "n³" + startNote(pattern.startN)
            is SequencePattern.Triangular -> "n(n + 1) ÷ 2" + startNote(pattern.startN)
            is SequencePattern.Alternating ->
                "${NumberFormatter.signed(pattern.first, decimals)}, " +
                    "${NumberFormatter.signed(pattern.second, decimals)} repeating"
            is SequencePattern.RepeatingDifference ->
                pattern.cycle.joinToString(", ") { NumberFormatter.signed(it, decimals) } + " repeating"
            is SequencePattern.SecondDifference -> "Second difference = ${f(pattern.secondDifference)}"
        }
    }

    /** Short headline for the result card, e.g. "+2", "×2", "÷3". */
    fun shortRule(pattern: SequencePattern, decimals: Int): String? = when (pattern) {
        is SequencePattern.Constant -> "+0"
        is SequencePattern.Arithmetic -> NumberFormatter.signed(pattern.difference, decimals)
        is SequencePattern.Geometric -> operator(pattern, decimals)
        is SequencePattern.Alternating, is SequencePattern.RepeatingDifference,
        is SequencePattern.SecondDifference, SequencePattern.Fibonacci -> null
        is SequencePattern.Squares -> "n²"
        is SequencePattern.Cubes -> "n³"
        is SequencePattern.Triangular -> "n(n + 1) ÷ 2"
    }

    fun formula(pattern: SequencePattern, decimals: Int): String = when (pattern) {
        is SequencePattern.Constant -> "aₙ = ${NumberFormatter.format(pattern.value, decimals)}"
        is SequencePattern.Arithmetic -> "aₙ = a₁ + (n − 1)d"
        is SequencePattern.Geometric -> "aₙ = a₁ × rⁿ⁻¹"
        SequencePattern.Fibonacci -> "aₙ = aₙ₋₁ + aₙ₋₂"
        is SequencePattern.Squares -> "aₙ = n²"
        is SequencePattern.Cubes -> "aₙ = n³"
        is SequencePattern.Triangular -> "aₙ = n(n + 1)/2"
        is SequencePattern.Alternating -> "aₙ₊₁ = aₙ + d₁, d₂, d₁, d₂, …"
        is SequencePattern.RepeatingDifference ->
            "aₙ₊₁ = aₙ + " + List(pattern.cycle.size) { "d${SUBSCRIPTS[it + 1]}" }.joinToString(", ") + ", …"
        is SequencePattern.SecondDifference -> "Δ²aₙ = ${NumberFormatter.format(pattern.secondDifference, decimals)}"
    }

    fun steps(pattern: SequencePattern, terms: List<BigDecimal>, decimals: Int): StepExplanation? {
        fun f(v: BigDecimal) = NumberFormatter.format(v, decimals)
        val shown = terms.take(MAX_STEP_TERMS)
        if (shown.size < 2) return null
        return when (pattern) {
            is SequencePattern.Constant -> null
            is SequencePattern.Arithmetic, is SequencePattern.Alternating,
            is SequencePattern.RepeatingDifference -> flow(shown, decimals) { a, b ->
                NumberFormatter.signed(b - a, decimals)
            }
            is SequencePattern.Geometric -> flow(shown, decimals) { _, _ -> operator(pattern, decimals) }
            SequencePattern.Fibonacci -> StepExplanation.Lines(
                (2 until shown.size).map { "${f(shown[it - 2])} + ${f(shown[it - 1])} = ${f(shown[it])}" },
            )
            is SequencePattern.Squares -> StepExplanation.Lines(
                shown.indices.map { "${pattern.startN + it}² = ${f(shown[it])}" },
            )
            is SequencePattern.Cubes -> StepExplanation.Lines(
                shown.indices.map { "${pattern.startN + it}³ = ${f(shown[it])}" },
            )
            is SequencePattern.Triangular -> StepExplanation.Lines(
                shown.indices.map {
                    val n = pattern.startN + it
                    "$n × ${n + 1} ÷ 2 = ${f(shown[it])}"
                },
            )
            is SequencePattern.SecondDifference -> {
                val first = SequenceLabCalculator.differences(terms)
                val second = SequenceLabCalculator.differences(first)
                StepExplanation.Lines(
                    listOf(
                        "Differences: " + first.joinToString(", ") { NumberFormatter.signed(it, decimals) },
                        "Second differences: " + second.joinToString(", ") { NumberFormatter.signed(it, decimals) },
                    ),
                )
            }
        }
    }

    fun observedDifferences(differences: List<BigDecimal>, decimals: Int): String =
        differences.joinToString(", ") { NumberFormatter.signed(it, decimals) }

    fun identifyCopyText(result: SequenceResult, decimals: Int): String = buildString {
        appendLine("Sequence:")
        appendLine(NumberFormatter.formatList(result.sequence, decimals))
        appendLine()
        appendLine("Pattern:")
        val pattern = result.pattern
        if (pattern == null) {
            appendLine(NO_PATTERN)
            if (result.differences.isNotEmpty()) {
                appendLine()
                appendLine("Observed differences:")
                append(observedDifferences(result.differences, decimals))
            }
        } else {
            appendLine(name(pattern) + if (result.confidence) "" else " ($PLAUSIBLE)")
            appendLine()
            appendLine("Rule:")
            appendLine(rule(pattern, decimals))
            appendLine()
            appendLine("Next:")
            append(NumberFormatter.formatList(result.nextTerms, decimals))
        }
    }.trimEnd()

    fun generateCopyText(result: GeneratedSequence, decimals: Int): String = buildString {
        appendLine("Sequence:")
        appendLine(NumberFormatter.formatList(result.terms, decimals))
        appendLine()
        appendLine("Rule:")
        append(rule(result.pattern, decimals))
    }

    private fun operator(pattern: SequencePattern.Geometric, decimals: Int) =
        pattern.divisor?.let { "÷${NumberFormatter.format(it, decimals)}" }
            ?: "×${NumberFormatter.format(pattern.ratio, decimals)}"

    private fun flow(
        terms: List<BigDecimal>,
        decimals: Int,
        op: (BigDecimal, BigDecimal) -> String,
    ): StepExplanation.Flow {
        val tokens = mutableListOf(FlowToken(NumberFormatter.format(terms[0], decimals), false))
        for (i in 1 until terms.size) {
            tokens += FlowToken(op(terms[i - 1], terms[i]), true)
            tokens += FlowToken(NumberFormatter.format(terms[i], decimals), false)
        }
        return StepExplanation.Flow(tokens)
    }

    private fun startNote(startN: Long) = if (startN == 1L) "" else ", starting at n = $startN"

    private const val SUBSCRIPTS = "₀₁₂₃₄₅₆₇₈₉"
}
