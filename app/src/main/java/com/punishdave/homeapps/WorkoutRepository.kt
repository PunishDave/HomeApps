package com.punishdave.homeapps

class WorkoutRepository(private val store: WorkoutStore) {
    fun entriesFlow() = store.entriesFlow
    fun accessKeyFlow() = store.accessKeyFlow
    fun daysFlow() = store.daysFlow

    suspend fun save(entries: List<WorkoutEntry>) {
        store.saveEntries(entries)
    }

    suspend fun saveAccessKey(key: String) {
        store.saveAccessKey(key)
    }

    suspend fun saveDays(days: List<WorkoutDay>) {
        store.saveDays(days)
    }

    suspend fun fetchDay(dayKey: String, key: String?): WorkoutDay {
        val bearer = key?.let { "Bearer $it" }
        return Network.workoutApi.getDay(
            dayKey = dayKey,
            key = key,
            altHeader = key,
            bearer = bearer,
            keyQuery = key,
            altQuery = key,
            plainKey = key,
            accessKey = key,
            cacheBuster = System.currentTimeMillis()
        )
    }

    suspend fun fetchLatest(dayKey: String, key: String?): WorkoutLatestResponse? {
        val bearer = key?.let { "Bearer $it" }
        return runCatching {
            Network.workoutApi.getDayLatest(
                dayKey = dayKey,
                key = key,
                altHeader = key,
                bearer = bearer,
                keyQuery = key,
                altQuery = key,
                plainKey = key,
                accessKey = key,
                cacheBuster = System.currentTimeMillis()
            )
        }.getOrNull()
    }

    suspend fun fetchDays(key: String?): List<WorkoutDay> {
        val bearer = key?.let { "Bearer $it" }
        return Network.workoutApi.listDays(
            key = key,
            altHeader = key,
            bearer = bearer,
            keyQuery = key,
            altQuery = key,
            plainKey = key,
            accessKey = key,
            cacheBuster = System.currentTimeMillis()
        )
    }

    suspend fun pushDay(day: WorkoutDay, key: String?): WorkoutDay {
        val bearer = key?.let { "Bearer $it" }
        val body = WorkoutDayPost(
            label = day.label,
            icon = day.icon,
            sort_order = day.sort_order,
            workouts = day.workouts.map {
                WorkoutMove(
                    name = it.name,
                    type = it.type,
                    reps = it.reps
                )
            }
        )
        return Network.workoutApi.upsertDay(
            dayKey = day.day_key,
            key = key,
            altHeader = key,
            bearer = bearer,
            keyQuery = key,
            altQuery = key,
            plainKey = key,
            accessKey = key,
            body = body
        )
    }

    suspend fun pushEntries(entries: List<WorkoutEntry>, key: String?) {
        val bearer = key?.let { "Bearer $it" }
        val payload = entries.map {
            WorkoutEntryPayload(
                workout = it.workout,
                weight = it.weight?.takeIf { w -> w.isNotBlank() } ?: it.notes.takeIf { notes -> notes.isNotBlank() },
                reps = it.reps,
                performed_on = it.date,
                notes = it.notes.takeIf { notes -> notes.isNotBlank() },
                day_key = it.dayKey
            )
        }
        Network.workoutApi.pushEntries(
            key = key,
            altHeader = key,
            bearer = bearer,
            keyQuery = key,
            altQuery = key,
            plainKey = key,
            accessKey = key,
            body = payload
        )
    }
}
