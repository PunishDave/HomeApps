package com.punishdave.homeapps

class WorkoutRepository(private val store: WorkoutStore) {
    fun entriesFlow() = store.entriesFlow

    suspend fun save(entries: List<WorkoutEntry>) {
        store.saveEntries(entries)
    }
}
