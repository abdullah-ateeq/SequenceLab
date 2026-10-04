package com.example.sequencelab.domain

import java.math.BigDecimal

sealed interface GenerateOutcome {
    data class Success(val result: GeneratedSequence) : GenerateOutcome
    data class Invalid(val errors: Map<GeneratorField, String>) : GenerateOutcome
}

/** Validates generator inputs and runs the matching [SequenceLabCalculator] generator. */
object SequenceGenerator {
    private const val MAX_START_N = 1_000_000L

    fun generate(inputs: GeneratorInputs): GenerateOutcome {
        val errors = mutableMapOf<GeneratorField, String>()
        val numbers = mutableMapOf<GeneratorField, BigDecimal>()
        var startN = 0L
        var count = 0

        for (field in inputs.type.fields()) {
            val text = inputs[field].trim()
            when (field) {
                GeneratorField.TERMS -> {
                    val terms = text.toIntOrNull()
                    if (terms == null ||
                        terms !in SequenceLabCalculator.MIN_GENERATED_TERMS..SequenceLabCalculator.MAX_GENERATED_TERMS
                    ) {
                        errors[field] = "Enter 2–100"
                    } else {
                        count = terms
                    }
                }
                GeneratorField.START_N -> {
                    val n = text.toLongOrNull()
                    if (n == null || n !in 0..MAX_START_N) errors[field] = "Whole number ≥ 0" else startN = n
                }
                else -> {
                    val value = SequenceParser.parseNumber(text)
                    if (value == null) errors[field] = "Enter a number" else numbers[field] = value
                }
            }
        }
        if (errors.isNotEmpty()) return GenerateOutcome.Invalid(errors)

        fun n(field: GeneratorField) = numbers.getValue(field)
        val calc = SequenceLabCalculator
        val (pattern, terms) = when (inputs.type) {
            GeneratorType.ARITHMETIC -> SequencePattern.Arithmetic(n(GeneratorField.DIFFERENCE)) to
                calc.generateArithmetic(n(GeneratorField.START), n(GeneratorField.DIFFERENCE), count)
            GeneratorType.GEOMETRIC -> SequencePattern.Geometric(n(GeneratorField.RATIO)) to
                calc.generateGeometric(n(GeneratorField.START), n(GeneratorField.RATIO), count)
            GeneratorType.FIBONACCI -> SequencePattern.Fibonacci to
                calc.generateFibonacci(n(GeneratorField.FIRST), n(GeneratorField.SECOND), count)
            GeneratorType.SQUARES -> SequencePattern.Squares(startN) to calc.generateSquares(startN, count)
            GeneratorType.CUBES -> SequencePattern.Cubes(startN) to calc.generateCubes(startN, count)
            GeneratorType.TRIANGULAR -> SequencePattern.Triangular(startN) to calc.generateTriangular(startN, count)
            GeneratorType.ALTERNATING -> SequencePattern.Alternating(
                n(GeneratorField.FIRST_CHANGE),
                n(GeneratorField.SECOND_CHANGE),
            ) to calc.generateAlternating(
                n(GeneratorField.START),
                n(GeneratorField.FIRST_CHANGE),
                n(GeneratorField.SECOND_CHANGE),
                count,
            )
        }
        return GenerateOutcome.Success(GeneratedSequence(inputs, pattern, terms))
    }
}
