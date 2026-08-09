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
    private val KEY_DAYS = stringPreferencesKey("workout_days_json")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, WorkoutEntry::class.java)
    private val adapter = moshi.adapter<List<WorkoutEntry>>(listType)
    private val daysType = Types.newParameterizedType(List::class.java, WorkoutDay::class.java)
    private val daysAdapter = moshi.adapter<List<WorkoutDay>>(daysType)

    val entriesFlow: Flow<List<WorkoutEntry>> = context.workoutDataStore.data.map { prefs ->
        val raw = prefs[KEY_ENTRIES]
        if (raw.isNullOrEmpty()) return@map emptyList()
        runCatching { adapter.fromJson(raw) ?: emptyList() }.getOrElse { emptyList() }
    }

    val accessKeyFlow: Flow<String> = context.workoutDataStore.data.map { prefs ->
        CredentialCipher.decrypt(prefs[KEY_ACCESS] ?: "")
    }

    val daysFlow: Flow<List<WorkoutDay>> = context.workoutDataStore.data.map { prefs ->
        val raw = prefs[KEY_DAYS]
        if (raw.isNullOrEmpty()) return@map emptyList()
        runCatching { daysAdapter.fromJson(raw) ?: emptyList() }.getOrElse { emptyList() }
    }

    suspend fun saveEntries(entries: List<WorkoutEntry>) {
        context.workoutDataStore.edit { it[KEY_ENTRIES] = adapter.toJson(entries) }
    }

    suspend fun saveAccessKey(key: String) {
        context.workoutDataStore.edit { it[KEY_ACCESS] = CredentialCipher.encrypt(key) }
    }

    suspend fun saveDays(days: List<WorkoutDay>) {
        context.workoutDataStore.edit { it[KEY_DAYS] = daysAdapter.toJson(days) }
    }
}
