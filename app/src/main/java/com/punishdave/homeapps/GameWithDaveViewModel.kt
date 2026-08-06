package com.punishdave.homeapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameWithDaveViewModel(app: Application) : AndroidViewModel(app) {
    private val store = GameWithDaveStore(app.applicationContext)
    val accessKey = store.accessKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    private val _dashboard = MutableStateFlow(GameWithDaveDashboard())
    val dashboard: StateFlow<GameWithDaveDashboard> = _dashboard
    val isLoading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        isLoading.value = true
        message.value = null
        try {
            val key = accessKey.value.trim().takeIf { it.isNotEmpty() }
            _dashboard.value = Network.gameWithDaveApi.dashboard(key, key?.let { "Bearer $it" })
        } catch (error: Exception) {
            message.value = error.message ?: "Unable to load GameWithDave"
        } finally {
            isLoading.value = false
        }
    }

    fun saveAvailability(role: String, startDate: String, endDate: String, status: String) = viewModelScope.launch {
        runUpdate {
            val key = accessKey.value.trim().takeIf { it.isNotEmpty() }
            Network.gameWithDaveApi.saveAvailability(
                key,
                key?.let { "Bearer $it" },
                key,
                GameWithDaveAvailabilityRequest(role, startDate, endDate, status)
            ).message
        }
    }

    fun updateNight(night: GameWithDaveNight, action: String) = viewModelScope.launch {
        runUpdate {
            val key = accessKey.value.trim().takeIf { it.isNotEmpty() }
            Network.gameWithDaveApi.updateNight(
                night.date,
                night.team,
                key,
                key?.let { "Bearer $it" },
                key,
                GameWithDaveNightUpdateRequest(action)
            )
            "Game night updated"
        }
    }

    private suspend fun runUpdate(block: suspend () -> String) {
        isLoading.value = true
        message.value = null
        try {
            message.value = block()
            val key = accessKey.value.trim().takeIf { it.isNotEmpty() }
            _dashboard.value = Network.gameWithDaveApi.dashboard(key, key?.let { "Bearer $it" })
        } catch (error: Exception) {
            message.value = error.message ?: "Update failed"
        } finally {
            isLoading.value = false
        }
    }
}
