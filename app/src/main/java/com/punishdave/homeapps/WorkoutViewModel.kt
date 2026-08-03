package com.punishdave.homeapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class WorkoutEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val workout: String,
    @Json(name = "day_key") val dayKey: String? = null,
    val weight: String? = null,
    val reps: Int? = null,
    val notes: String = "",
    val synced: Boolean = false
)

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(WorkoutStore(app.applicationContext))

    val entries = repo.entriesFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val accessKey = repo.accessKeyFlow().stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val days = repo.daysFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selectedDayKey = MutableStateFlow<String?>(null)
    val selectedDayDetail = MutableStateFlow<WorkoutDay?>(null)
    val latestEntries = MutableStateFlow<List<WorkoutLatestEntry>>(emptyList())

    val workoutText = MutableStateFlow("")
    val notesText = MutableStateFlow("")
    val dateText = MutableStateFlow(LocalDate.now().toString())
    val lastError = MutableStateFlow<String?>(null)
    val lastSyncStatus = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repo.daysFlow().collect { stored ->
                if (stored.isNotEmpty() && selectedDayKey.value == null) {
                    selectedDayKey.value = stored.first().day_key
                }
                selectedDayKey.value?.let { key ->
                    stored.firstOrNull { it.day_key == key }?.let { selectedDayDetail.value = it }
                }
            }
        }
    }

    fun addEntry() = viewModelScope.launch {
        val w = workoutText.value.trim()
        val d = dateText.value.trim().ifEmpty { LocalDate.now().toString() }
        val weight = notesText.value.trim().ifEmpty { null }

        if (w.isEmpty()) {
            lastError.value = "Add a workout name first."
            return@launch
        }
        if (weight.isNullOrEmpty()) {
            lastError.value = "Add a weight first."
            return@launch
        }

        val newEntry = WorkoutEntry(
            date = d,
            workout = w,
            weight = weight,
            reps = null,
            notes = weight,
            synced = false
        )

        val updated = listOf(newEntry) + entries.value.filterNot {
            it.workout.equals(w, ignoreCase = true) && it.date == d
        }
        repo.save(updated)

        workoutText.value = ""
        notesText.value = ""
        dateText.value = LocalDate.now().toString()
        lastError.value = null
    }

    fun deleteEntry(id: String) = viewModelScope.launch {
        val updated = entries.value.filterNot { it.id == id }
        repo.save(updated)
    }

    fun saveAccessKey(key: String) = viewModelScope.launch {
        repo.saveAccessKey(key.trim())
    }

    fun addEntryForMove(move: WorkoutMove, weightText: String, dayKey: String?) = viewModelScope.launch {
        val w = move.name.trim()
        val weight = weightText.trim()
        if (w.isEmpty()) {
            lastError.value = "Workout name missing."
            return@launch
        }
        if (weight.isEmpty()) {
            lastError.value = "Add a weight first."
            return@launch
        }

        val today = LocalDate.now().toString()
        val newEntry = WorkoutEntry(
            date = today,
            workout = w,
            dayKey = dayKey,
            weight = weight,
            reps = move.reps,
            notes = weight,
            synced = false
        )
        val updated = listOf(newEntry) + entries.value.filterNot {
            it.workout.equals(w, ignoreCase = true) && it.date == today
        }
        repo.save(updated)
        lastError.value = null
    }

    fun selectDay(key: String) = viewModelScope.launch {
        selectedDayKey.value = key
        days.value.firstOrNull { it.day_key == key }?.let { selectedDayDetail.value = it }
        refreshDayDetail(key)
    }

    fun syncFromApi() = viewModelScope.launch {
        val key = accessKey.value.trim().ifEmpty { null }
        if (key == null) {
            lastError.value = "Enter the workout access key in Settings, then tap Sync."
            lastSyncStatus.value = null
            return@launch
        }
        try {
            val fetched = repo.fetchDays(key).sortedBy { it.sort_order ?: Int.MAX_VALUE }
            repo.saveDays(fetched)
            if (selectedDayKey.value == null && fetched.isNotEmpty()) {
                val firstKey = fetched.first().day_key
                selectedDayKey.value = firstKey
            }
            selectedDayKey.value?.let { refreshDayDetail(it) }
            lastSyncStatus.value = "Synced ${fetched.size} days from server."
            lastError.value = null
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val msg = e.response()?.errorBody()?.string()?.take(300)
            lastError.value = "Sync failed (HTTP $code). ${msg ?: e.message()}"
            lastSyncStatus.value = null
        } catch (e: Exception) {
            lastError.value = "Sync failed: ${e.message ?: "unknown error"}"
            lastSyncStatus.value = null
        }
    }

    fun pushDay(dayKey: String) = viewModelScope.launch {
        val key = accessKey.value.trim().ifEmpty { null }
        val day = days.value.firstOrNull { it.day_key == dayKey } ?: return@launch
        if (key == null) {
            lastError.value = "Enter the workout access key in Settings, then tap Sync/Push."
            return@launch
        }
        try {
            repo.pushDay(day, key)
            lastSyncStatus.value = "Pushed ${day.label}."
            lastError.value = null
        } catch (e: Exception) {
            lastError.value = "Push failed: ${e.message}"
            lastSyncStatus.value = null
        }
    }

    fun pushEntriesForDay(dayKey: String?) = viewModelScope.launch {
        val key = accessKey.value.trim().ifEmpty { null }
        if (key == null) {
            lastError.value = "Enter the workout access key in Settings, then tap Sync/Push."
            return@launch
        }
        val filtered = if (dayKey == null) {
            entries.value
        } else {
            val dayWorkouts = days.value.firstOrNull { it.day_key == dayKey }?.workouts?.map { it.name.lowercase() }?.toSet()
            if (dayWorkouts.isNullOrEmpty()) entries.value else entries.value.filter { it.workout.lowercase() in dayWorkouts }
        }
        val unsynced = filtered.filterNot { it.synced }
        if (unsynced.isEmpty()) {
            lastError.value = "No new entries to push for this day."
            return@launch
        }
        try {
            val dayConfig = days.value.firstOrNull { it.day_key == dayKey }
            val payload = unsynced.map { entry ->
                val targetDayKey = entry.dayKey ?: dayKey
                val reps = entry.reps ?: dayConfig?.workouts?.firstOrNull { it.name.equals(entry.workout, ignoreCase = true) }?.reps
                val withDay = entry.copy(dayKey = targetDayKey, reps = reps)
                withDay.copy(synced = false)
            }
            repo.pushEntries(payload, key)
            // mark pushed entries as synced locally
            val updated = entries.value.map { entry ->
                if (payload.any { it.id == entry.id }) entry.copy(synced = true) else entry
            }
            repo.save(updated)
            if (dayKey != null) {
                refreshDayDetail(dayKey)
            }
            lastSyncStatus.value = "Pushed ${payload.size} entries."
            lastError.value = null
        } catch (e: Exception) {
            lastError.value = "Push failed: ${e.message}"
            lastSyncStatus.value = null
        }
    }

    private suspend fun refreshDayDetail(dayKey: String) {
        val key = accessKey.value.trim().ifEmpty { null } ?: return
        latestEntries.value = emptyList()
        try {
            val detail = repo.fetchDay(dayKey, key)
            selectedDayDetail.value = detail
            persistDayDetail(detail)
            val latest = repo.fetchLatest(dayKey, key)
            latestEntries.value = latest?.latest.orEmpty()
        } catch (e: Exception) {
            if (selectedDayDetail.value == null) {
                lastError.value = "Failed to load day: ${e.message ?: "unknown error"}"
            }
        }
    }

    private suspend fun persistDayDetail(detail: WorkoutDay) {
        val merged = days.value.toMutableList()
        val idx = merged.indexOfFirst { it.day_key == detail.day_key }
        if (idx >= 0) {
            merged[idx] = detail
        } else {
            merged.add(detail)
        }
        repo.saveDays(merged)
    }
}
