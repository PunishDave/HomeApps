package com.punishdave.homeapps

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.concurrent.TimeUnit

private val Context.refreshSettingsDataStore by preferencesDataStore(name = "refresh_settings")

data class RefreshSettings(
    val enabled: Boolean = false,
    val backgroundEnabled: Boolean = true,
    val intervalMinutes: Int = 30,
    val unmeteredOnly: Boolean = false
)

data class RefreshRunInfo(
    val completedAt: Long? = null,
    val mode: String? = null,
    val successful: Set<String> = emptySet(),
    val failed: Set<String> = emptySet(),
    val nextExpectedAt: Long? = null
)

class RefreshSettingsStore(private val context: Context) {
    private val enabledKey = booleanPreferencesKey("automatic_refresh_enabled")
    private val backgroundKey = booleanPreferencesKey("background_refresh_enabled")
    private val intervalKey = intPreferencesKey("refresh_interval_minutes")
    private val unmeteredKey = booleanPreferencesKey("refresh_unmetered_only")
    private val completedAtKey = longPreferencesKey("refresh_completed_at")
    private val modeKey = stringPreferencesKey("refresh_mode")
    private val successfulKey = stringPreferencesKey("refresh_successful")
    private val failedKey = stringPreferencesKey("refresh_failed")
    private val nextExpectedKey = longPreferencesKey("refresh_next_expected")

    val settings = context.refreshSettingsDataStore.data.map {
        RefreshSettings(
            enabled = it[enabledKey] ?: false,
            backgroundEnabled = it[backgroundKey] ?: true,
            intervalMinutes = normalizeRefreshInterval(it[intervalKey] ?: 30),
            unmeteredOnly = it[unmeteredKey] ?: false
        )
    }

    val history = context.refreshSettingsDataStore.data.map {
        RefreshRunInfo(
            completedAt = it[completedAtKey],
            mode = it[modeKey],
            successful = decodeServiceIds(it[successfulKey]),
            failed = decodeServiceIds(it[failedKey]),
            nextExpectedAt = it[nextExpectedKey]
        )
    }

    suspend fun save(value: RefreshSettings) {
        context.refreshSettingsDataStore.edit {
            it[enabledKey] = value.enabled
            it[backgroundKey] = value.backgroundEnabled
            it[intervalKey] = normalizeRefreshInterval(value.intervalMinutes)
            it[unmeteredKey] = value.unmeteredOnly
            if (value.enabled && value.backgroundEnabled) {
                it[nextExpectedKey] = Instant.now().toEpochMilli() + normalizeRefreshInterval(value.intervalMinutes) * 60_000L
            } else {
                it.remove(nextExpectedKey)
            }
        }
        RefreshScheduler.apply(context, value)
    }

    suspend fun record(mode: String, successful: Set<String>, failed: Set<String>) {
        val current = settings.first()
        context.refreshSettingsDataStore.edit {
            it[completedAtKey] = Instant.now().toEpochMilli()
            it[modeKey] = mode
            it[successfulKey] = successful.sorted().joinToString(",")
            it[failedKey] = failed.sorted().joinToString(",")
            if (mode == "Background" && current.enabled && current.backgroundEnabled) {
                it[nextExpectedKey] = Instant.now().toEpochMilli() + current.intervalMinutes * 60_000L
            }
        }
    }
}

private fun decodeServiceIds(value: String?): Set<String> = value.orEmpty().split(',').filter { it.isNotBlank() }.toSet()

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
            Constraints.Builder().setRequiredNetworkType(
                if (settings.unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            ).build()
        ).build()
        manager.enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
