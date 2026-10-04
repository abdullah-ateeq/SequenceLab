package com.example.sequencelab.domain

import java.math.BigDecimal
import java.math.MathContext

/**
 * Pure, UI-independent sequence math. Input values are exact decimals, so addition,
 * subtraction and multiplication stay exact and comparisons use [BigDecimal.compareTo].
 */
object SequenceLabCalculator {
    val MATH_CONTEXT: MathContext = MathContext.DECIMAL128

    const val MIN_GENERATED_TERMS = 2
    const val MAX_GENERATED_TERMS = 100

    /** Values beyond this are not tested against squares/cubes/triangular numbers. */
    private val MAX_NAMED_VALUE = BigDecimal("1e17")
    private const val CONFIDENT_TERMS = 4
    private const val MORE_TERMS_HELPFUL_BELOW = 6

    private class Detection(val pattern: SequencePattern, val confident: Boolean)

    // region Identification

    fun identifySequence(values: List<BigDecimal>, nextCount: Int = 3): SequenceResult {
        require(values.size >= SequenceParser.MIN_TERMS) { "At least two values are required" }
        val diffs = differences(values)
        val detection = detectConstant(values)
            ?: detectArithmetic(values, diffs)
            ?: detectGeometric(values)
            ?: detectFibonacci(values)
            ?: detectSquares(values)
            ?: detectCubes(values)
            ?: detectTriangular(values)
            ?: detectDifferenceCycle(diffs)
            ?: detectSecondDifference(values, diffs)
            ?: return SequenceResult(values, null, emptyList(), false, diffs)
        return SequenceResult(
            sequence = values,
            pattern = detection.pattern,
            nextTerms = extend(values, detection.pattern, nextCount),
            confidence = detection.confident,
            differences = diffs,
        )
    }

    /** True when an unidentified sequence is short enough that more terms could help. */
    fun moreTermsMayHelp(values: List<BigDecimal>) = values.size < MORE_TERMS_HELPFUL_BELOW

    fun differences(values: List<BigDecimal>): List<BigDecimal> =
        values.zipWithNext { a, b -> b - a }

    private fun detectConstant(values: List<BigDecimal>): Detection? {
        if (values.any { !it.eq(values[0]) }) return null
        return Detection(SequencePattern.Constant(values[0]), values.size >= 3)
    }

    private fun detectArithmetic(values: List<BigDecimal>, diffs: List<BigDecimal>): Detection? {
        if (diffs.any { !it.eq(diffs[0]) }) return null
        return Detection(SequencePattern.Arithmetic(diffs[0]), values.size >= 3)
    }

    private fun detectGeometric(values: List<BigDecimal>): Detection? {
        if (values.size < 3) return null
        val a0 = values[0]
        val a1 = values[1]
        if (a0.signum() == 0) return null
        // Cross-multiplication avoids dividing: a(i+1) / a(i) = a1 / a0  ⇔  a(i+1)·a0 = a(i)·a1.
        for (i in 1 until values.size - 1) {
            if (!(values[i + 1] * a0).eq(values[i] * a1)) return null
        }
        val exactRatio = exactDivide(a1, a0)
        val ratio = exactRatio ?: a1.divide(a0, MATH_CONTEXT)
        val pattern = if (a1.signum() != 0 && ratio.abs() < BigDecimal.ONE) {
            val divisor = exactDivide(a0, a1)
            if (divisor != null && divisor.isInteger()) {
                SequencePattern.Geometric(ratio, divisor.stripTrailingZeros())
            } else {
                SequencePattern.Geometric(ratio)
            }
        } else {
            SequencePattern.Geometric(ratio)
        }
        return Detection(pattern, values.size >= CONFIDENT_TERMS)
    }

    private fun detectFibonacci(values: List<BigDecimal>): Detection? {
        if (values.size < 3) return null
        for (i in 2 until values.size) {
            if (!values[i].eq(values[i - 1] + values[i - 2])) return null
        }
        return Detection(SequencePattern.Fibonacci, values.size >= 5)
    }

    private fun detectSquares(values: List<BigDecimal>): Detection? =
        detectConsecutive(values, ::squareRootOf)?.let {
            Detection(SequencePattern.Squares(it), values.size >= CONFIDENT_TERMS)
        }

    private fun detectCubes(values: List<BigDecimal>): Detection? =
        detectConsecutive(values, ::cubeRootOf)?.let {
            Detection(SequencePattern.Cubes(it), values.size >= CONFIDENT_TERMS)
        }

    private fun detectTriangular(values: List<BigDecimal>): Detection? =
        detectConsecutive(values, ::triangularIndexOf)?.let {
            Detection(SequencePattern.Triangular(it), values.size >= CONFIDENT_TERMS)
        }

    /**
     * Checks that every value maps (via [indexOf]) to consecutive n values.
     * Returns the n of the first term.
     */
    private fun detectConsecutive(values: List<BigDecimal>, indexOf: (Long) -> Long?): Long? {
        if (values.size < 3) return null
        var start: Long? = null
        values.forEachIndexed { i, value ->
            val integer = value.toLongIfInteger() ?: return null
            val n = indexOf(integer) ?: return null
            if (start == null) start = n else if (n != start!! + i) return null
        }
        return start
    }

    /**
     * Detects short difference cycles (length 2 or 3) that repeat at least twice.
     * A 2-cycle whose steps go in opposite directions is reported as alternating.
     */
    private fun detectDifferenceCycle(diffs: List<BigDecimal>): Detection? {
        for (length in 2..3) {
            if (diffs.size < length * 2) continue
            val cycle = diffs.take(length)
            if (cycle.all { it.eq(cycle[0]) }) continue
            if (diffs.indices.any { !diffs[it].eq(cycle[it % length]) }) continue
            val pattern = if (length == 2 && cycle[0].signum() * cycle[1].signum() < 0) {
                SequencePattern.Alternating(cycle[0], cycle[1])
            } else {
                SequencePattern.RepeatingDifference(cycle)
            }
            return Detection(pattern, true)
        }
        return null
    }

    private fun detectSecondDifference(values: List<BigDecimal>, diffs: List<BigDecimal>): Detection? {
        if (values.size < 4) return null
        val second = differences(diffs)
        if (second.any { !it.eq(second[0]) } || second[0].signum() == 0) return null
        return Detection(SequencePattern.SecondDifference(second[0]), values.size >= 5)
    }

    // endregion

    // region Next terms

    /** Returns the [count] terms that follow [values] under [pattern]. */
    fun extend(values: List<BigDecimal>, pattern: SequencePattern, count: Int): List<BigDecimal> {
        val terms = values.toMutableList()
        repeat(count) { terms.add(nextTerm(terms, pattern)) }
        return terms.subList(values.size, terms.size).toList()
    }

    private fun nextTerm(terms: List<BigDecimal>, pattern: SequencePattern): BigDecimal {
        val index = terms.size
        val last = terms.last()
        return when (pattern) {
            is SequencePattern.Constant -> pattern.value
            is SequencePattern.Arithmetic -> last + pattern.difference
            is SequencePattern.Geometric -> pattern.divisor?.let { last.divide(it, MATH_CONTEXT) }
                ?: last.multiply(pattern.ratio, MATH_CONTEXT)
            SequencePattern.Fibonacci -> last + terms[index - 2]
            is SequencePattern.Squares -> nthPower(pattern.startN + index, 2)
            is SequencePattern.Cubes -> nthPower(pattern.startN + index, 3)
            is SequencePattern.Triangular -> triangular(pattern.startN + index)
            is SequencePattern.Alternating ->
                last + if ((index - 1) % 2 == 0) pattern.first else pattern.second
            is SequencePattern.RepeatingDifference -> last + pattern.cycle[(index - 1) % pattern.cycle.size]
            is SequencePattern.SecondDifference ->
                last + (last - terms[index - 2]) + pattern.secondDifference
        }
    }

    // endregion

    // region Generation

    fun generateArithmetic(start: BigDecimal, difference: BigDecimal, count: Int): List<BigDecimal> =
        List(count) { start + difference * BigDecimal(it) }

    /** Multiplies only, so a ratio of 0 is valid (5, 0, 0, 0). */
    fun generateGeometric(start: BigDecimal, ratio: BigDecimal, count: Int): List<BigDecimal> {
        val terms = ArrayList<BigDecimal>(count)
        var current = start
        repeat(count) {
            terms.add(current)
            current = current.multiply(ratio, MATH_CONTEXT)
        }
        return terms
    }

    fun generateFibonacci(first: BigDecimal, second: BigDecimal, count: Int): List<BigDecimal> {
        val terms = mutableListOf(first, second)
        while (terms.size < count) terms.add(terms[terms.size - 1] + terms[terms.size - 2])
        return terms.take(count)
    }

    fun generateSquares(startN: Long, count: Int): List<BigDecimal> =
        List(count) { nthPower(startN + it, 2) }

    fun generateCubes(startN: Long, count: Int): List<BigDecimal> =
        List(count) { nthPower(startN + it, 3) }

    fun generateTriangular(startN: Long, count: Int): List<BigDecimal> =
        List(count) { triangular(startN + it) }

    fun generateAlternating(
        start: BigDecimal,
        firstChange: BigDecimal,
        secondChange: BigDecimal,
        count: Int,
    ): List<BigDecimal> = generateRepeatingDifference(start, listOf(firstChange, secondChange), count)

    fun generateRepeatingDifference(start: BigDecimal, cycle: List<BigDecimal>, count: Int): List<BigDecimal> {
        require(cycle.isNotEmpty())
        val terms = ArrayList<BigDecimal>(count)
        var current = start
        repeat(count) {
            terms.add(current)
            current += cycle[it % cycle.size]
        }
        return terms
    }

    // endregion

    // region Helpers

    private fun BigDecimal.eq(other: BigDecimal) = compareTo(other) == 0

    private fun BigDecimal.isInteger() = signum() == 0 || stripTrailingZeros().scale() <= 0

    private fun BigDecimal.toLongIfInteger(): Long? =
        if (isInteger() && abs() <= MAX_NAMED_VALUE) longValueExact() else null

    private fun exactDivide(a: BigDecimal, b: BigDecimal): BigDecimal? = try {
        a.divide(b).takeIf { it.scale() <= 20 }
    } catch (_: ArithmeticException) {
        null
    }

    private fun nthPower(n: Long, power: Int): BigDecimal = BigDecimal.valueOf(n).pow(power)

    private fun triangular(n: Long): BigDecimal =
        BigDecimal.valueOf(n) * BigDecimal.valueOf(n + 1) / BigDecimal(2)

    private fun isqrt(value: Long): Long {
        var r = Math.sqrt(value.toDouble()).toLong()
        while (r * r > value) r--
        while ((r + 1) * (r + 1) <= value) r++
        return r
    }

    private fun squareRootOf(value: Long): Long? {
        if (value < 0) return null
        val root = isqrt(value)
        return root.takeIf { it * it == value }
    }

    private fun cubeRootOf(value: Long): Long? {
        val estimate = Math.round(Math.cbrt(value.toDouble()))
        return (estimate - 1..estimate + 1).firstOrNull { it * it * it == value }
    }

    /** n such that n(n+1)/2 = value, for n ≥ 0. */
    private fun triangularIndexOf(value: Long): Long? {
        if (value < 0) return null
        val discriminant = 8 * value + 1
        val root = isqrt(discriminant)
        if (root * root != discriminant) return null
        return (root - 1) / 2
    }

    // endregion
}
