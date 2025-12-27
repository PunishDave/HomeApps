package com.punishdave.homeapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MealPlannerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MealPlannerRepository(MealPlannerStore(app.applicationContext))

    val recipes = repo.recipesFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val currentWeek = repo.currentWeekFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val plannedWeek = repo.plannedWeekFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // simple UI flags
    val isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val lastError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    fun sync() = viewModelScope.launch {
        isSyncing.value = true
        lastError.value = null
        try {
            repo.sync()
        } catch (e: Exception) {
            lastError.value = e.message ?: "Sync failed"
        } finally {
            isSyncing.value = false
        }
    }

    fun generateWeek() = viewModelScope.launch {
        try {
            repo.generateRandomWeek()
        } catch (e: Exception) {
            lastError.value = e.message ?: "Generate failed"
        }
    }

    fun saveWeek() = viewModelScope.launch {
        val week = plannedWeek.value ?: return@launch
        try {
            repo.savePlannedWeekToServer(week)
        } catch (e: Exception) {
            lastError.value = e.message ?: "Save failed"
        }
    }
}
