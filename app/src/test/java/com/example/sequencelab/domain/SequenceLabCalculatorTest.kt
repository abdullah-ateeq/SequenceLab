package com.example.sequencelab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SequenceLabCalculatorTest {

    private val calc = SequenceLabCalculator

    private fun nums(vararg values: String) = values.map { BigDecimal(it) }
    private fun identify(text: String, next: Int = 3): SequenceResult {
        val parsed = SequenceParser.parse(text) as ParseResult.Success
        return calc.identifySequence(parsed.values, next)
    }
    private fun List<BigDecimal>.fmt() = NumberFormatter.formatList(this, 3)
    private fun rule(result: SequenceResult) = PatternText.rule(result.pattern!!, 3)

    // 1. Arithmetic identification
    @Test fun arithmeticAscending() {
        val r = identify("2, 4, 6, 8")
        assertEquals(SequenceType.ARITHMETIC, r.type)
        assertEquals("Add 2", rule(r))
        assertEquals("+2", PatternText.shortRule(r.pattern!!, 3))
        assertEquals("10, 12, 14", r.nextTerms.fmt())
        assertTrue(r.confidence)
    }

    @Test fun arithmeticDescending() {
        val r = identify("20, 17, 14, 11")
        assertEquals(SequenceType.ARITHMETIC, r.type)
        assertEquals("Subtract 3", rule(r))
        assertEquals("8, 5, 2", r.nextTerms.fmt())
    }

    @Test fun constantSequence() {
        val r = identify("5, 5, 5, 5")
        assertEquals(SequenceType.CONSTANT, r.type)
        assertEquals("Add 0", rule(r))
        assertEquals("5, 5, 5", r.nextTerms.fmt())
    }

    @Test fun twoTermsArePlausibleOnly() {
        val r = identify("2, 4")
        assertEquals(SequenceType.ARITHMETIC, r.type)
        assertFalse(r.confidence)
    }

    // 2. Arithmetic generation
    @Test fun arithmeticGeneration() {
        assertEquals("5, 8, 11, 14, 17, 20", calc.generateArithmetic(BigDecimal(5), BigDecimal(3), 6).fmt())
    }

    @Test fun defaultGenerator() {
        val outcome = SequenceGenerator.generate(GeneratorInputs()) as GenerateOutcome.Success
        assertEquals("1, 3, 5, 7, 9, 11", outcome.result.terms.fmt())
    }

    // 3. Geometric identification
    @Test fun geometricMultiply() {
        val r = identify("2, 4, 8, 16")
        assertEquals(SequenceType.GEOMETRIC, r.type)
        assertEquals("Multiply by 2", rule(r))
        assertEquals("×2", PatternText.shortRule(r.pattern!!, 3))
        assertEquals("32, 64, 128", r.nextTerms.fmt())
    }

    @Test fun geometricDivide() {
        val r = identify("81, 27, 9, 3")
        assertEquals(SequenceType.GEOMETRIC, r.type)
        assertEquals("Divide by 3", rule(r))
        assertEquals("1, 0.333, 0.111", r.nextTerms.fmt())
    }

    @Test fun geometricNegativeRatio() {
        val r = identify("2, -2, 2, -2")
        assertEquals(SequenceType.GEOMETRIC, r.type)
        assertEquals("Multiply by -1", rule(r))
        assertEquals("2, -2, 2", r.nextTerms.fmt())
    }

    // 4. Geometric generation
    @Test fun geometricGeneration() {
        assertEquals("2, 4, 8, 16, 32, 64", calc.generateGeometric(BigDecimal(2), BigDecimal(2), 6).fmt())
    }

    // 18. Zero ratio
    @Test fun zeroRatio() {
        assertEquals("5, 0, 0, 0", calc.generateGeometric(BigDecimal(5), BigDecimal.ZERO, 4).fmt())
        val r = identify("5, 0, 0, 0")
        assertEquals(SequenceType.GEOMETRIC, r.type)
        assertEquals("Multiply by 0", rule(r))
        assertEquals("0, 0, 0", r.nextTerms.fmt())
    }

    @Test fun leadingZeroIsNotGeometric() {
        assertTrue(identify("0, 0, 5, 7").type != SequenceType.GEOMETRIC)
    }

    // 5. Fibonacci identification
    @Test fun fibonacciClassic() {
        val r = identify("1, 1, 2, 3, 5, 8")
        assertEquals(SequenceType.FIBONACCI, r.type)
        assertEquals("Add the previous two terms", rule(r))
        assertEquals("13, 21, 34", r.nextTerms.fmt())
    }

    @Test fun fibonacciLikeOtherStart() {
        val r = identify("2, 3, 5, 8, 13")
        assertEquals(SequenceType.FIBONACCI, r.type)
        assertEquals("21, 34, 55", r.nextTerms.fmt())
    }

    // 6. Fibonacci generation
    @Test fun fibonacciGeneration() {
        assertEquals("2, 3, 5, 8, 13, 21, 34", calc.generateFibonacci(BigDecimal(2), BigDecimal(3), 7).fmt())
        assertEquals("4, 7", calc.generateFibonacci(BigDecimal(4), BigDecimal(7), 2).fmt())
    }

    // 7. Square identification
    @Test fun squares() {
        val r = identify("1, 4, 9, 16, 25")
        assertEquals(SequenceType.SQUARE, r.type)
        assertEquals("n²", rule(r))
        assertEquals("36, 49, 64", r.nextTerms.fmt())
    }

    @Test fun squaresNotStartingAtOne() {
        val r = identify("9, 16, 25, 36")
        assertEquals(SequenceType.SQUARE, r.type)
        assertEquals(SequencePattern.Squares(3), r.pattern)
        assertEquals("49, 64, 81", r.nextTerms.fmt())
        assertEquals(SequenceType.SQUARE, identify("4, 9, 16, 25").type)
    }

    // 8. Square generation
    @Test fun squareGeneration() {
        assertEquals("1, 4, 9, 16, 25, 36", calc.generateSquares(1, 6).fmt())
        assertEquals("4, 9, 16", calc.generateSquares(2, 3).fmt())
    }

    // 9. Cube identification
    @Test fun cubes() {
        val r = identify("1, 8, 27, 64")
        assertEquals(SequenceType.CUBE, r.type)
        assertEquals("n³", rule(r))
        assertEquals("125, 216, 343", r.nextTerms.fmt())
        assertEquals(SequenceType.CUBE, identify("8, 27, 64, 125").type)
    }

    // 10. Cube generation
    @Test fun cubeGeneration() {
        assertEquals("1, 8, 27, 64, 125", calc.generateCubes(1, 5).fmt())
    }

    // 11. Triangular identification
    @Test fun triangular() {
        val r = identify("1, 3, 6, 10, 15")
        assertEquals(SequenceType.TRIANGULAR, r.type)
        assertEquals("n(n + 1) ÷ 2", rule(r))
        assertEquals("21, 28, 36", r.nextTerms.fmt())
    }

    // 12. Triangular generation
    @Test fun triangularGeneration() {
        assertEquals("1, 3, 6, 10, 15, 21", calc.generateTriangular(1, 6).fmt())
    }

    // 13. Alternating identification
    @Test fun alternating() {
        val r = identify("2, 5, 4, 7, 6, 9")
        assertEquals(SequenceType.ALTERNATING, r.type)
        assertEquals("+3, −1 repeating", rule(r))
        assertEquals("8, 11, 10", r.nextTerms.fmt())
    }

    @Test fun alternatingGeneration() {
        assertEquals(
            "2, 5, 4, 7, 6, 9, 8",
            calc.generateAlternating(BigDecimal(2), BigDecimal(3), BigDecimal(-1), 7).fmt(),
        )
    }

    // 14. Repeating difference
    @Test fun repeatingDifference() {
        val r = identify("2, 5, 9, 12, 16, 19")
        assertEquals(SequenceType.REPEATING_DIFFERENCE, r.type)
        assertEquals("+3, +4 repeating", rule(r))
        assertEquals("23, 26, 30", r.nextTerms.fmt())
    }

    @Test fun repeatingDifferenceCycleOfThree() {
        val r = identify("0, 1, 3, 6, 7, 9, 12")
        assertEquals(SequenceType.REPEATING_DIFFERENCE, r.type)
        assertEquals("+1, +2, +3 repeating", rule(r))
        assertEquals("13, 15, 18", r.nextTerms.fmt())
    }

    @Test fun repeatingDifferenceGeneration() {
        assertEquals(
            "0, 1, 3, 6, 7",
            calc.generateRepeatingDifference(BigDecimal.ZERO, nums("1", "2", "3"), 5).fmt(),
        )
    }

    // 15. Second difference
    @Test fun secondDifference() {
        val r = identify("2, 5, 10, 17, 26")
        assertEquals(SequenceType.SECOND_DIFFERENCE, r.type)
        assertEquals("Second difference = 2", rule(r))
        assertEquals("37, 50, 65", r.nextTerms.fmt())
    }

    @Test fun squaresPreferredOverSecondDifference() {
        assertEquals(SequenceType.SQUARE, identify("1, 4, 9, 16, 25").type)
    }

    @Test fun unknownPattern() {
        val r = identify("2, 5, 10, 21, 32")
        assertNull(r.pattern)
        assertTrue(r.nextTerms.isEmpty())
        assertEquals("+3, +5, +11, +11", PatternText.observedDifferences(r.differences, 3))
        assertTrue(calc.moreTermsMayHelp(r.sequence))
    }

    // 16. Negative numbers
    @Test fun negativeNumbers() {
        val r = identify("-10, -5, 0, 5, 10")
        assertEquals(SequenceType.ARITHMETIC, r.type)
        assertEquals("Add 5", rule(r))
        assertEquals("15, 20, 25", r.nextTerms.fmt())
        assertEquals("Subtract 5", rule(identify("5 0 -5 -10")))
    }

    // 17. Decimal numbers
    @Test fun decimals() {
        val r = identify("1.5, 2.0, 2.5, 3.0")
        assertEquals(SequenceType.ARITHMETIC, r.type)
        assertEquals("Add 0.5", rule(r))
        assertEquals("3.5, 4, 4.5", r.nextTerms.fmt())
        val g = identify("0.5, 1.5, 4.5, 13.5")
        assertEquals("Multiply by 3", rule(g))
    }

    // 22. Next-term generation count
    @Test fun nextTermCount() {
        assertEquals("25, 30, 35", identify("5, 10, 15, 20").nextTerms.fmt())
        assertEquals(5, identify("5, 10, 15, 20", 5).nextTerms.size)
        assertEquals(10, identify("5, 10, 15, 20", 10).nextTerms.size)
    }

    @Test fun generatorValidation() {
        val bad = GeneratorInputs().with(GeneratorField.TERMS, "1").with(GeneratorField.DIFFERENCE, "abc")
        val outcome = SequenceGenerator.generate(bad) as GenerateOutcome.Invalid
        assertEquals(setOf(GeneratorField.TERMS, GeneratorField.DIFFERENCE), outcome.errors.keys)
        val big = GeneratorInputs().with(GeneratorField.TERMS, "100")
        assertEquals(100, (SequenceGenerator.generate(big) as GenerateOutcome.Success).result.terms.size)
        assertTrue(SequenceGenerator.generate(GeneratorInputs().with(GeneratorField.TERMS, "101")) is GenerateOutcome.Invalid)
    }
}
