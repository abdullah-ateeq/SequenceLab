package com.example.sequencelab.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.sequencelab.domain.GeneratorField
import com.example.sequencelab.domain.GeneratorInputs
import com.example.sequencelab.domain.GeneratorType
import com.example.sequencelab.domain.SequenceMode
import com.example.sequencelab.domain.SequenceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal

class DataPersistenceTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun identifyRecord(id: Long) = HistoryRecord(
        id = id,
        timestamp = 1_700_000_000_000 + id,
        mode = SequenceMode.IDENTIFY,
        patternType = SequenceType.ARITHMETIC,
        title = "Arithmetic",
        rule = "Add 2",
        sequence = listOf("2", "4", "6", "8").map { BigDecimal(it) },
        nextTerms = listOf("10", "12", "14").map { BigDecimal(it) },
        generatorInputs = null,
    )

    private val generatedRecord = HistoryRecord(
        id = 99,
        timestamp = 1_700_000_000_500,
        mode = SequenceMode.GENERATE,
        patternType = SequenceType.GEOMETRIC,
        title = "Geometric",
        rule = "Multiply by -0.5",
        sequence = listOf("2", "-1", "0.5", "-0.25").map { BigDecimal(it) },
        nextTerms = emptyList(),
        generatorInputs = GeneratorInputs.defaults(GeneratorType.GEOMETRIC, 4)
            .with(GeneratorField.START, "2")
            .with(GeneratorField.RATIO, "-0.5"),
    )

    /** Runs [block] with a DataStore on [file], closing it afterwards so the file can be reopened. */
    private fun <T> withStore(file: File, block: suspend (DataStore<Preferences>) -> T): T {
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            return runBlocking { block(store) }
        } finally {
            runBlocking { job.cancelAndJoin() }
        }
    }

    // 24. History serialization
    @Test fun historyCodecRoundTrip() {
        val records = listOf(generatedRecord, identifyRecord(1))
        assertEquals(records, HistoryCodec.decode(HistoryCodec.encode(records)))
    }

    @Test fun historyCodecSkipsCorruptRecords() {
        val encoded = HistoryCodec.encode(listOf(identifyRecord(1))) + "\u001Egarbage"
        assertEquals(listOf(identifyRecord(1)), HistoryCodec.decode(encoded))
        assertTrue(HistoryCodec.decode(null).isEmpty())
    }

    @Test fun historyIsCappedAtFiftyAndSurvivesReopen() {
        val file = File(tmp.root, "history.preferences_pb")
        withStore(file) { store ->
            val repo = HistoryRepository(store)
            (1L..51L).forEach { repo.add(identifyRecord(it)) }
            val saved = repo.records.first()
            assertEquals(50, saved.size)
            assertEquals(51L, saved.first().id)
            assertTrue(saved.none { it.id == 1L })
        }
        withStore(file) { store ->
            val repo = HistoryRepository(store)
            assertEquals(50, repo.records.first().size)
            repo.delete(51L)
            assertEquals(49, repo.records.first().size)
            repo.clear()
            assertTrue(repo.records.first().isEmpty())
        }
    }

    // 25. Settings persistence
    @Test fun settingsPersist() {
        val file = File(tmp.root, "settings.preferences_pb")
        val changed = AppSettings(
            theme = ThemeMode.DARK,
            dynamicColor = true,
            defaultMode = SequenceMode.GENERATE,
            defaultGenerator = GeneratorType.FIBONACCI,
            defaultTerms = 10,
            decimals = 6,
            nextTerms = 5,
            haptics = false,
            showFormula = false,
            showSteps = false,
        )
        withStore(file) { store ->
            val repo = SettingsRepository(store)
            assertEquals(AppSettings(), repo.settings.first())
            repo.update { changed }
        }
        withStore(file) { store ->
            val repo = SettingsRepository(store)
            assertEquals(changed, repo.settings.first())
            repo.reset()
            assertEquals(AppSettings(), repo.settings.first())
        }
    }
}
