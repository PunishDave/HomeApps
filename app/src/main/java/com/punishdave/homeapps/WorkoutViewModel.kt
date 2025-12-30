package com.punishdave.homeapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class WorkoutEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val workout: String,
    val notes: String = ""
)

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(WorkoutStore(app.applicationContext))

    val entries = repo.entriesFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accessKey = repo.accessKeyFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val days = MutableStateFlow<List<WorkoutDay>>(emptyList())
    val selectedDayKey = MutableStateFlow<String?>(null)

    val workoutText = MutableStateFlow("")
    val notesText = MutableStateFlow("")
    val dateText = MutableStateFlow(LocalDate.now().toString())
    val lastError = MutableStateFlow<String?>(null)
    val lastSyncStatus = MutableStateFlow<String?>(null)

    fun addEntry() = viewModelScope.launch {
        val w = workoutText.value.trim()
        val d = dateText.value.trim().ifEmpty { LocalDate.now().toString() }

        if (w.isEmpty()) {
            lastError.value = "Add a workout name first."
            return@launch
        }

        val newEntry = WorkoutEntry(
            date = d,
            workout = w,
            notes = notesText.value.trim()
        )

        val updated = listOf(newEntry) + entries.value
        repo.save(updated)

        workoutText.value = ""
        notesText.value = ""
        dateText.value = LocalDate.now().toString()
    }

    fun deleteEntry(id: String) = viewModelScope.launch {
        val updated = entries.value.filterNot { it.id == id }
        repo.save(updated)
    }

    fun saveAccessKey(key: String) = viewModelScope.launch {
        repo.saveAccessKey(key.trim())
    }

    fun selectDay(key: String) {
        selectedDayKey.value = key
    }

    fun syncFromApi() = viewModelScope.launch {
        val key = accessKey.value.trim().ifEmpty { null }
        if (key == null) {
            lastError.value = "Enter the workout access key in Settings, then tap Sync."
            lastSyncStatus.value = null
            return@launch
        }
        try {
            val fetched = repo.fetchDays(key)
            days.value = fetched.sortedBy { it.sort_order ?: Int.MAX_VALUE }
            if (selectedDayKey.value == null && fetched.isNotEmpty()) {
                selectedDayKey.value = fetched.first().day_key
            }
            lastSyncStatus.value = "Synced ${fetched.size} days from server."
            lastError.value = null
        } catch (e: Exception) {
            lastError.value = "Sync failed: ${e.message ?: "unknown error"}"
            lastSyncStatus.value = null
        }
    }
}
