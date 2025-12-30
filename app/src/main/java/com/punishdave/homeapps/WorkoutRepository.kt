package com.punishdave.homeapps

class WorkoutRepository(private val store: WorkoutStore) {
    fun entriesFlow() = store.entriesFlow
    fun accessKeyFlow() = store.accessKeyFlow

    suspend fun save(entries: List<WorkoutEntry>) {
        store.saveEntries(entries)
    }

    suspend fun saveAccessKey(key: String) {
        store.saveAccessKey(key)
    }

    suspend fun fetchDays(): List<WorkoutDay> {
        return Network.workoutApi.listDays()
    }
}
