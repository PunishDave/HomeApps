package com.punishdave.homeapps

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.workoutDataStore by preferencesDataStore(name = "workout_store")

class WorkoutStore(private val context: Context) {
    private val KEY_ENTRIES = stringPreferencesKey("workout_entries_json")
    private val KEY_ACCESS = stringPreferencesKey("workout_access_key")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, WorkoutEntry::class.java)
    private val adapter = moshi.adapter<List<WorkoutEntry>>(listType)

    val entriesFlow: Flow<List<WorkoutEntry>> = context.workoutDataStore.data.map { prefs ->
        val raw = prefs[KEY_ENTRIES]
        if (raw.isNullOrEmpty()) return@map emptyList()
        runCatching { adapter.fromJson(raw) ?: emptyList() }.getOrElse { emptyList() }
    }

    val accessKeyFlow: Flow<String> = context.workoutDataStore.data.map { prefs ->
        prefs[KEY_ACCESS] ?: ""
    }

    suspend fun saveEntries(entries: List<WorkoutEntry>) {
        context.workoutDataStore.edit { it[KEY_ENTRIES] = adapter.toJson(entries) }
    }

    suspend fun saveAccessKey(key: String) {
        context.workoutDataStore.edit { it[KEY_ACCESS] = key }
    }
}
