package com.punishdave.homeapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

private val gameWithDaveDateFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.UK)
    .withResolverStyle(ResolverStyle.STRICT)

class GameWithDaveViewModel(app: Application) : AndroidViewModel(app) {
    private val store = GameWithDaveStore(app.applicationContext)
    private val repository = GameWithDaveRepository()
    val accessKey = store.accessKeyFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val username = store.usernameFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    private val _dashboard = MutableStateFlow(GameWithDaveDashboard())
    val dashboard: StateFlow<GameWithDaveDashboard> = _dashboard
    val isLoading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        isLoading.value = true
        message.value = null
        try {
            val key = storedAccessKey()
            _dashboard.value = repository.dashboard(key ?: error("Add the GameWithDave access key in Settings."))
        } catch (error: Exception) {
            message.value = error.message ?: "Unable to load GameWithDave"
        } finally {
            isLoading.value = false
        }
    }

    fun saveAvailability(startDate: String, endDate: String, status: String) = viewModelScope.launch {
        runUpdate {
            val key = storedAccessKey()
            val role = store.usernameFlow.first().trim().ifEmpty { throw IllegalStateException("Add your GameWithDave user in Settings.") }
            val secret = store.passwordFlow.first().ifEmpty { throw IllegalStateException("Add your GameWithDave password in Settings.") }
            val apiStartDate = LocalDate.parse(startDate, gameWithDaveDateFormatter).toString()
            val apiEndDate = LocalDate.parse(endDate, gameWithDaveDateFormatter).toString()
            repository.saveAvailability(
                key ?: error("Add the GameWithDave access key in Settings."),
                GameWithDaveAvailabilityRequest(role, apiStartDate, apiEndDate, status, secret)
            )
        }
    }

    fun updateNight(night: GameWithDaveNight, action: String) = viewModelScope.launch {
        runUpdate {
            val key = storedAccessKey()
            val secret = store.passwordFlow.first()
            repository.updateNight(key ?: error("Add the GameWithDave access key in Settings."), night, action, secret)
            "Game night updated"
        }
    }

    private suspend fun runUpdate(block: suspend () -> String) {
        isLoading.value = true
        message.value = null
        try {
            message.value = block()
            val key = storedAccessKey()
            _dashboard.value = repository.dashboard(key ?: error("Add the GameWithDave access key in Settings."))
        } catch (error: Exception) {
            message.value = error.message ?: "Update failed"
        } finally {
            isLoading.value = false
        }
    }

    private suspend fun storedAccessKey(): String? =
        store.accessKeyFlow.first().trim().takeIf { it.isNotEmpty() }
}
