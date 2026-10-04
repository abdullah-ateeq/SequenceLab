package com.example.sequencelab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class ParserAndFormatterTest {

    private fun values(text: String) = (SequenceParser.parse(text) as ParseResult.Success).values

    @Test fun acceptsCommasSpacesAndNewLines() {
        val expected = listOf("2", "4", "6", "8").map { BigDecimal(it) }
        assertEquals(expected, values("2, 4, 6, 8"))
        assertEquals(expected, values("2 4 6 8"))
        assertEquals(expected, values("2\n4\n6\n8"))
        assertEquals(expected, values("  2,4 ,\n 6;8  "))
    }

    @Test fun acceptsNegativeZeroAndDecimals() {
        assertEquals(listOf("-5", "0", "1.5", "2.75", "0.5").map { BigDecimal(it) }, values("-5, 0, 1.5, 2.75, .5"))
        assertEquals(BigDecimal("-3"), values("−3, 1")[0])
    }

    // 19. Empty input
    @Test fun emptyInput() {
        assertEquals(ParseResult.Error(SequenceParser.TOO_FEW), SequenceParser.parse(""))
        assertEquals(ParseResult.Error(SequenceParser.TOO_FEW), SequenceParser.parse("  , \n"))
    }

    // 20. Too few terms
    @Test fun tooFew() {
        assertEquals(ParseResult.Error("Enter at least two numbers."), SequenceParser.parse("7"))
    }

    // 21. Too many terms
    @Test fun tooMany() {
        assertEquals(ParseResult.Error("Maximum 50 numbers."), SequenceParser.parse((1..51).joinToString(", ")))
        assertEquals(50, values((1..50).joinToString(", ")).size)
    }

    @Test fun rejectsInvalidTokens() {
        assertEquals(ParseResult.Error("\"abc\" is not a valid number."), SequenceParser.parse("1, abc, 3"))
        assertNull(SequenceParser.parseNumber("1.2.3"))
        assertNull(SequenceParser.parseNumber("1e5"))
        assertNull(SequenceParser.parseNumber("-"))
    }

    // 23. Number formatting
    @Test fun formatting() {
        assertEquals("2", NumberFormatter.format(BigDecimal("2.00"), 3))
        assertEquals("2.5", NumberFormatter.format(BigDecimal("2.50"), 3))
        assertEquals("3.125", NumberFormatter.format(BigDecimal("3.125"), 3))
        assertEquals("2", NumberFormatter.format(BigDecimal("2.0000000001"), 6))
        assertEquals("0.333", NumberFormatter.format(BigDecimal("0.33333333"), 3))
        assertEquals("0.3", NumberFormatter.format(BigDecimal("0.33333333"), 1))
        assertEquals("-7.25", NumberFormatter.format(BigDecimal("-7.25"), 2))
        assertEquals("100", NumberFormatter.format(BigDecimal("100"), 3))
        assertEquals("0", NumberFormatter.format(BigDecimal("0.000"), 3))
    }

    @Test fun scientificNotation() {
        assertEquals("1.23 × 10¹²", NumberFormatter.format(BigDecimal("1230000000000"), 3))
        assertEquals("1 × 10¹²", NumberFormatter.format(BigDecimal("999999999999.9999"), 3))
        assertEquals("1.5 × 10⁻⁵", NumberFormatter.format(BigDecimal("0.000015"), 3))
        assertEquals("-2 × 10¹⁵", NumberFormatter.format(BigDecimal("-2e15"), 3))
        assertEquals("999999999999", NumberFormatter.format(BigDecimal("999999999999"), 3))
    }

    @Test fun signed() {
        assertEquals("+3", NumberFormatter.signed(BigDecimal(3), 3))
        assertEquals("−1", NumberFormatter.signed(BigDecimal(-1), 3))
    }
}
