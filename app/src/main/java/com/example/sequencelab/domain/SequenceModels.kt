package com.example.sequencelab.domain

import java.math.BigDecimal

enum class SequenceMode {
    IDENTIFY,
    GENERATE,
}

enum class SequenceType(val displayName: String) {
    CONSTANT("Constant"),
    ARITHMETIC("Arithmetic"),
    GEOMETRIC("Geometric"),
    FIBONACCI("Fibonacci-like"),
    SQUARE("Square numbers"),
    CUBE("Cube numbers"),
    TRIANGULAR("Triangular numbers"),
    ALTERNATING("Alternating"),
    REPEATING_DIFFERENCE("Repeating difference"),
    SECOND_DIFFERENCE("Constant second difference"),
}

/** A recognized rule together with the parameters needed to describe and extend it. */
sealed interface SequencePattern {
    val type: SequenceType

    data class Constant(val value: BigDecimal) : SequencePattern {
        override val type get() = SequenceType.CONSTANT
    }

    data class Arithmetic(val difference: BigDecimal) : SequencePattern {
        override val type get() = SequenceType.ARITHMETIC
    }

    /**
     * When [divisor] is non-null the ratio is 1 ÷ divisor; terms are produced by dividing,
     * which keeps results like 81, 27, 9, 3 → 1 exact instead of multiplying by 0.333….
     */
    data class Geometric(val ratio: BigDecimal, val divisor: BigDecimal? = null) : SequencePattern {
        override val type get() = SequenceType.GEOMETRIC
    }

    data object Fibonacci : SequencePattern {
        override val type get() = SequenceType.FIBONACCI
    }

    /** [startN] is the n of the first term. */
    data class Squares(val startN: Long) : SequencePattern {
        override val type get() = SequenceType.SQUARE
    }

    data class Cubes(val startN: Long) : SequencePattern {
        override val type get() = SequenceType.CUBE
    }

    data class Triangular(val startN: Long) : SequencePattern {
        override val type get() = SequenceType.TRIANGULAR
    }

    data class Alternating(val first: BigDecimal, val second: BigDecimal) : SequencePattern {
        override val type get() = SequenceType.ALTERNATING
    }

    data class RepeatingDifference(val cycle: List<BigDecimal>) : SequencePattern {
        override val type get() = SequenceType.REPEATING_DIFFERENCE
    }

    data class SecondDifference(val secondDifference: BigDecimal) : SequencePattern {
        override val type get() = SequenceType.SECOND_DIFFERENCE
    }
}

/**
 * Outcome of identification. [pattern] is null when no supported simple pattern fits.
 * [confidence] is false when the pattern fits but too few terms were given to be sure.
 */
data class SequenceResult(
    val sequence: List<BigDecimal>,
    val pattern: SequencePattern?,
    val nextTerms: List<BigDecimal>,
    val confidence: Boolean,
    val differences: List<BigDecimal>,
) {
    val type: SequenceType? get() = pattern?.type
}

enum class GeneratorType(val displayName: String) {
    ARITHMETIC("Arithmetic"),
    GEOMETRIC("Geometric"),
    FIBONACCI("Fibonacci-like"),
    SQUARES("Squares"),
    CUBES("Cubes"),
    TRIANGULAR("Triangular"),
    ALTERNATING("Alternating"),
}

enum class GeneratorField(val label: String, val description: String) {
    START("Start", "Starting value"),
    DIFFERENCE("Difference", "Difference"),
    RATIO("Ratio", "Ratio"),
    FIRST("First value", "First value"),
    SECOND("Second value", "Second value"),
    START_N("Starting n", "Starting n"),
    FIRST_CHANGE("First change", "First change"),
    SECOND_CHANGE("Second change", "Second change"),
    TERMS("Terms", "Number of terms"),
}

fun GeneratorType.fields(): List<GeneratorField> = when (this) {
    GeneratorType.ARITHMETIC -> listOf(GeneratorField.START, GeneratorField.DIFFERENCE)
    GeneratorType.GEOMETRIC -> listOf(GeneratorField.START, GeneratorField.RATIO)
    GeneratorType.FIBONACCI -> listOf(GeneratorField.FIRST, GeneratorField.SECOND)
    GeneratorType.SQUARES, GeneratorType.CUBES, GeneratorType.TRIANGULAR -> listOf(GeneratorField.START_N)
    GeneratorType.ALTERNATING -> listOf(
        GeneratorField.START,
        GeneratorField.FIRST_CHANGE,
        GeneratorField.SECOND_CHANGE,
    )
} + GeneratorField.TERMS

/** Raw text of every generator field, kept for all types so switching type doesn't lose input. */
data class GeneratorInputs(
    val type: GeneratorType = GeneratorType.ARITHMETIC,
    val values: Map<GeneratorField, String> = DEFAULT_VALUES,
) {
    operator fun get(field: GeneratorField): String = values[field].orEmpty()

    fun with(field: GeneratorField, value: String) = copy(values = values + (field to value))

    companion object {
        val DEFAULT_VALUES = mapOf(
            GeneratorField.START to "1",
            GeneratorField.DIFFERENCE to "2",
            GeneratorField.RATIO to "2",
            GeneratorField.FIRST to "1",
            GeneratorField.SECOND to "1",
            GeneratorField.START_N to "1",
            GeneratorField.FIRST_CHANGE to "3",
            GeneratorField.SECOND_CHANGE to "-1",
            GeneratorField.TERMS to "6",
        )

        fun defaults(type: GeneratorType, terms: Int) =
            GeneratorInputs(type, DEFAULT_VALUES + (GeneratorField.TERMS to terms.toString()))
    }
}

data class GeneratedSequence(
    val inputs: GeneratorInputs,
    val pattern: SequencePattern,
    val terms: List<BigDecimal>,
)
