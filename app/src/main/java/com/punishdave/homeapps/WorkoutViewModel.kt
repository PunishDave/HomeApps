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

    fun syncFromApi() = viewModelScope.launch {
        val key = accessKey.value.trim()
        if (key.isEmpty()) {
            lastError.value = "Enter the access key first."
            lastSyncStatus.value = "Sync skipped: no access key."
            return@launch
        }
        // Placeholder until a real API is wired
        lastSyncStatus.value = "No workout sync API configured yet. Stored key."
    }
}
