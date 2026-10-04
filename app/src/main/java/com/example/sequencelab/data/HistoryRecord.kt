package com.example.sequencelab.data

import com.example.sequencelab.domain.GeneratorField
import com.example.sequencelab.domain.GeneratorInputs
import com.example.sequencelab.domain.GeneratorType
import com.example.sequencelab.domain.NumberFormatter
import com.example.sequencelab.domain.SequenceMode
import com.example.sequencelab.domain.SequenceType
import java.math.BigDecimal

/**
 * A saved result. For IDENTIFY, [sequence] is the original input; for GENERATE it is the
 * generated sequence and [generatorInputs] holds the settings needed to recalculate it.
 */
data class HistoryRecord(
    val id: Long,
    val timestamp: Long,
    val mode: SequenceMode,
    val patternType: SequenceType?,
    val title: String,
    val rule: String,
    val sequence: List<BigDecimal>,
    val nextTerms: List<BigDecimal>,
    val generatorInputs: GeneratorInputs?,
)

/**
 * Compact text encoding for storing history in a single Preferences string.
 * Records are separated by RS (U+001E) and fields by US (U+001F); both are stripped from
 * free text, and numeric fields contain only number characters, so no escaping is needed.
 */
object HistoryCodec {
    private const val VERSION = "1"
    private const val RECORD_SEP = '\u001E'
    private const val FIELD_SEP = '\u001F'
    private const val FIELD_COUNT = 10
    private val numericChars = Regex("[^0-9.+\\-]")

    fun encode(records: List<HistoryRecord>): String =
        records.joinToString(RECORD_SEP.toString()) { encodeRecord(it) }

    /** Skips any record that cannot be read rather than failing the whole list. */
    fun decode(text: String?): List<HistoryRecord> {
        if (text.isNullOrEmpty()) return emptyList()
        return text.split(RECORD_SEP).mapNotNull { decodeRecord(it) }
    }

    private fun encodeRecord(r: HistoryRecord): String = listOf(
        VERSION,
        r.id.toString(),
        r.timestamp.toString(),
        r.mode.name,
        r.patternType?.name.orEmpty(),
        clean(r.title),
        clean(r.rule),
        r.sequence.joinToString(",") { NumberFormatter.plain(it) },
        r.nextTerms.joinToString(",") { NumberFormatter.plain(it) },
        r.generatorInputs?.let(::encodeInputs).orEmpty(),
    ).joinToString(FIELD_SEP.toString())

    private fun decodeRecord(text: String): HistoryRecord? = runCatching {
        val f = text.split(FIELD_SEP)
        if (f.size != FIELD_COUNT || f[0] != VERSION) return null
        val mode = SequenceMode.valueOf(f[3])
        HistoryRecord(
            id = f[1].toLong(),
            timestamp = f[2].toLong(),
            mode = mode,
            patternType = f[4].takeIf { it.isNotEmpty() }?.let { SequenceType.valueOf(it) },
            title = f[5],
            rule = f[6],
            sequence = decodeNumbers(f[7]),
            nextTerms = decodeNumbers(f[8]),
            generatorInputs = f[9].takeIf { it.isNotEmpty() }?.let(::decodeInputs),
        ).takeIf { it.sequence.isNotEmpty() && (mode == SequenceMode.IDENTIFY || it.generatorInputs != null) }
    }.getOrNull()

    private fun encodeInputs(inputs: GeneratorInputs): String =
        (listOf(inputs.type.name) + GeneratorField.entries.map { it.name + "=" + inputs[it].replace(numericChars, "") })
            .joinToString(";")

    private fun decodeInputs(text: String): GeneratorInputs {
        val parts = text.split(';')
        val type = GeneratorType.valueOf(parts[0])
        val values = parts.drop(1).associate { entry ->
            val (key, value) = entry.split('=', limit = 2)
            GeneratorField.valueOf(key) to value
        }
        return GeneratorInputs(type, GeneratorInputs.DEFAULT_VALUES + values)
    }

    private fun decodeNumbers(text: String): List<BigDecimal> =
        if (text.isEmpty()) emptyList() else text.split(',').map { BigDecimal(it) }

    private fun clean(text: String) = text.replace(RECORD_SEP.toString(), "").replace(FIELD_SEP.toString(), "")
}
