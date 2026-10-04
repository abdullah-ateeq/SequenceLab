package com.example.sequencelab.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Single place where numbers are turned into display text. */
object NumberFormatter {
    private val SCIENTIFIC_UPPER = BigDecimal("1e12")
    private const val MAX_MANTISSA_DECIMALS = 4
    private const val SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    /**
     * Rounds to [decimals] places and drops trailing zeros (2.50 → 2.5, 2.00 → 2).
     * Very large values, or non-zero values that would round to 0, use scientific notation.
     */
    fun format(value: BigDecimal, decimals: Int): String {
        if (value.signum() == 0) return "0"
        val rounded = value.setScale(decimals, RoundingMode.HALF_UP)
        if (rounded.abs() >= SCIENTIFIC_UPPER || rounded.signum() == 0) {
            return scientific(value, minOf(decimals, MAX_MANTISSA_DECIMALS))
        }
        return rounded.stripTrailingZeros().toPlainString()
    }

    fun formatList(values: List<BigDecimal>, decimals: Int, separator: String = ", "): String =
        values.joinToString(separator) { format(it, decimals) }

    /** "+3" / "−1": used for differences and step operators. */
    fun signed(value: BigDecimal, decimals: Int): String {
        val text = format(value.abs(), decimals)
        return if (value.signum() < 0) "−$text" else "+$text"
    }

    /** Lossless text for storage. */
    fun plain(value: BigDecimal): String =
        if (value.signum() == 0) "0" else value.stripTrailingZeros().toPlainString()

    private fun scientific(value: BigDecimal, mantissaDecimals: Int): String {
        val abs = value.abs()
        var exponent = abs.precision() - abs.scale() - 1
        var mantissa = abs.movePointLeft(exponent).setScale(mantissaDecimals, RoundingMode.HALF_UP)
        if (mantissa >= BigDecimal.TEN) {
            exponent += 1
            mantissa = abs.movePointLeft(exponent).setScale(mantissaDecimals, RoundingMode.HALF_UP)
        }
        val sign = if (value.signum() < 0) "-" else ""
        return "$sign${mantissa.stripTrailingZeros().toPlainString()} × 10${superscript(exponent)}"
    }

    private fun superscript(exponent: Int): String {
        val digits = Math.abs(exponent).toString().map { SUPERSCRIPTS[it - '0'] }.joinToString("")
        return if (exponent < 0) "⁻$digits" else digits
    }
}
