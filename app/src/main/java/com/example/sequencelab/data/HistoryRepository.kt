package com.example.sequencelab.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "history")

/** Newest-first history, capped at [MAX_RECORDS]; saving beyond the cap drops the oldest. */
class HistoryRepository(private val dataStore: DataStore<Preferences>) {

    val records: Flow<List<HistoryRecord>> = dataStore.data.map { HistoryCodec.decode(it[KEY]) }

    suspend fun add(record: HistoryRecord) = modify { listOf(record) + it }

    suspend fun delete(id: Long) = modify { list -> list.filterNot { it.id == id } }

    suspend fun clear() {
        dataStore.edit { it.remove(KEY) }
    }

    private suspend fun modify(transform: (List<HistoryRecord>) -> List<HistoryRecord>) {
        dataStore.edit { prefs ->
            prefs[KEY] = HistoryCodec.encode(transform(HistoryCodec.decode(prefs[KEY])).take(MAX_RECORDS))
        }
    }

    companion object {
        const val MAX_RECORDS = 50
        private val KEY = stringPreferencesKey("records")
    }
}
