package com.punishdave.homeapps

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.refreshSettingsDataStore by preferencesDataStore(name = "refresh_settings")

data class RefreshSettings(
    val enabled: Boolean = false,
    val backgroundEnabled: Boolean = true,
    val intervalMinutes: Int = 30
)

class RefreshSettingsStore(private val context: Context) {
    private val enabledKey = booleanPreferencesKey("automatic_refresh_enabled")
    private val backgroundKey = booleanPreferencesKey("background_refresh_enabled")
    private val intervalKey = intPreferencesKey("refresh_interval_minutes")

    val settings = context.refreshSettingsDataStore.data.map {
        RefreshSettings(
            enabled = it[enabledKey] ?: false,
            backgroundEnabled = it[backgroundKey] ?: true,
            intervalMinutes = normalizeRefreshInterval(it[intervalKey] ?: 30)
        )
    }

    suspend fun save(value: RefreshSettings) {
        context.refreshSettingsDataStore.edit {
            it[enabledKey] = value.enabled
            it[backgroundKey] = value.backgroundEnabled
            it[intervalKey] = normalizeRefreshInterval(value.intervalMinutes)
        }
        RefreshScheduler.apply(context, value)
    }
}

val refreshIntervalOptions = listOf(15, 30, 60, 120, 360, 720)

fun normalizeRefreshInterval(minutes: Int): Int =
    refreshIntervalOptions.minByOrNull { kotlin.math.abs(it - minutes) } ?: 30

object RefreshScheduler {
    internal const val WorkName = "homeapps_periodic_refresh"

    fun apply(context: Context, settings: RefreshSettings) {
        val manager = WorkManager.getInstance(context)
        if (!settings.enabled || !settings.backgroundEnabled) {
            manager.cancelUniqueWork(WorkName)
            return
        }
        val request = PeriodicWorkRequestBuilder<HomeRefreshWorker>(
            normalizeRefreshInterval(settings.intervalMinutes).toLong(),
            TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        ).build()
        manager.enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
