package com.punishdave.homeapps

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

private val Context.reliabilityDataStore by preferencesDataStore(name = "home_reliability")

class HomeReliabilityStore(private val context: Context) {
    val lastUpdated = context.reliabilityDataStore.data.map { preferences ->
        homeSectionIds.mapNotNull { id ->
            preferences[longPreferencesKey("updated_$id")]?.let { id to it }
        }.toMap()
    }

    suspend fun markUpdated(ids: Set<String>, epochMillis: Long = Instant.now().toEpochMilli()) {
        context.reliabilityDataStore.edit { preferences ->
            ids.forEach { id -> preferences[longPreferencesKey("updated_$id")] = epochMillis }
        }
    }

    suspend fun claimNotification(type: String, fingerprint: String): Boolean {
        val key = stringPreferencesKey("notification_$type")
        if (context.reliabilityDataStore.data.first()[key] == fingerprint) return false
        context.reliabilityDataStore.edit { it[key] = fingerprint }
        return true
    }

    suspend fun updateTemperatureZone(deviceId: String, zone: String): Boolean {
        val key = stringPreferencesKey("temperature_zone_$deviceId")
        val previous = context.reliabilityDataStore.data.first()[key]
        context.reliabilityDataStore.edit { it[key] = zone }
        return zone != "normal" && zone != previous
    }
}

class HomeReliabilityViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HomeReliabilityStore(app.applicationContext)
    val lastUpdated = store.lastUpdated.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun markUpdated(ids: Set<String>) = viewModelScope.launch { store.markUpdated(ids) }
    suspend fun claimNotification(type: String, fingerprint: String) = store.claimNotification(type, fingerprint)
    suspend fun updateTemperatureZone(deviceId: String, zone: String) = store.updateTemperatureZone(deviceId, zone)
}

data class TemperatureAlert(val fingerprint: String, val message: String)
data class TemperatureState(val deviceId: String, val zone: String, val message: String?)

fun temperatureState(device: SophonDevice, low: Double, high: Double): TemperatureState {
    val temperature = device.temperature_c
    val name = friendlySensorName(device.device_id)
    return when {
        temperature == null -> TemperatureState(device.device_id, "unknown", null)
        temperature >= high -> TemperatureState(device.device_id, "high", "$name is warm at %.1f°C".format(temperature))
        temperature <= low -> TemperatureState(device.device_id, "low", "$name is cold at %.1f°C".format(temperature))
        else -> TemperatureState(device.device_id, "normal", null)
    }
}

fun temperatureAlerts(devices: List<SophonDevice>, low: Double, high: Double): List<TemperatureAlert> =
    devices.mapNotNull { device ->
        val state = temperatureState(device, low, high)
        state.message?.let { TemperatureAlert("${state.deviceId}:${state.zone}", it) }
    }
