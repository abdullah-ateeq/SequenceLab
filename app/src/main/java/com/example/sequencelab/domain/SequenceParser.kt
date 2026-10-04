package com.example.sequencelab.domain

import java.math.BigDecimal

sealed interface ParseResult {
    data class Success(val values: List<BigDecimal>) : ParseResult
    data class Error(val message: String) : ParseResult
}

object SequenceParser {
    const val MIN_TERMS = 2
    const val MAX_TERMS = 50
    private const val MAX_TOKEN_LENGTH = 30

    const val TOO_FEW = "Enter at least two numbers."
    const val TOO_MANY = "Maximum 50 numbers."

    private val separators = Regex("[,;\\s]+")
    private val numberPattern = Regex("^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)$")

    /** Accepts numbers separated by commas, semicolons, spaces or new lines. */
    fun parse(input: String): ParseResult {
        val tokens = input.split(separators).filter { it.isNotEmpty() }
        if (tokens.size < MIN_TERMS) {
            tokens.firstOrNull()?.let { if (parseNumber(it) == null) return invalid(it) }
            return ParseResult.Error(TOO_FEW)
        }
        if (tokens.size > MAX_TERMS) return ParseResult.Error(TOO_MANY)
        val values = tokens.map { parseNumber(it) ?: return invalid(it) }
        return ParseResult.Success(values)
    }

    /** Parses one plain decimal number; returns null for anything else. */
    fun parseNumber(text: String): BigDecimal? {
        val normalized = text.trim().replace('−', '-')
        if (normalized.length > MAX_TOKEN_LENGTH || !numberPattern.matches(normalized)) return null
        return normalized.removePrefix("+").toBigDecimalOrNull()
    }

    private fun invalid(token: String): ParseResult.Error {
        val shown = if (token.length > 12) token.take(12) + "…" else token
        return ParseResult.Error("\"$shown\" is not a valid number.")
    }
}
