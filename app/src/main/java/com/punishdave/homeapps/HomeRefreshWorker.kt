package com.punishdave.homeapps

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout

class HomeRefreshWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {

    private val attemptedServices = setOf("meal_planner", "todo", "workout", "gamewithdave", "sophon")

    override suspend fun doWork(): Result = runCatching {
        val successful = withTimeout(90_000) {
            supervisorScope {
                listOf(
                    "meal_planner" to async { refreshMealPlanner() },
                    "todo" to async { refreshTodo() },
                    "workout" to async { refreshWorkout() },
                    "gamewithdave" to async { refreshGameWithDave() },
                    "sophon" to async { refreshSophon() }
                ).map { (id, task) -> async { id to runCatching { task.await() }.getOrDefault(false) } }
                    .awaitAll().filter { it.second }.map { it.first }.toSet()
            }
        }
        if (successful.isNotEmpty()) HomeReliabilityStore(applicationContext).markUpdated(successful)
        RefreshSettingsStore(applicationContext).record("Background", successful, attemptedServices - successful)
        sendBackgroundAlerts(successful)
        Result.success()
    }.getOrElse {
        RefreshSettingsStore(applicationContext).record("Background", emptySet(), attemptedServices)
        Result.retry()
    }

    private suspend fun sendBackgroundAlerts(successful: Set<String>) {
        val preferences = applicationContext.webSettingsDataStore.data.first()
        if (preferences[booleanPreferencesKey("notifications_enabled")] != true) return
        createHomeNotificationChannel(applicationContext)
        val reliability = HomeReliabilityStore(applicationContext)
        if ("gamewithdave" in successful && preferences[booleanPreferencesKey("game_notifications_enabled")] != false) {
            GameWithDaveStore(applicationContext).dashboardFlow.first().days
                .firstOrNull { day -> day.game_nights.any { it.status != "removed" } }
                ?.let { day ->
                    val active = day.game_nights.filter { it.status != "removed" }
                    val fingerprint = day.date + ":" + active.joinToString { "${it.team}:${it.status}" }
                    if (reliability.claimNotification("game", fingerprint)) {
                        notifyGameNight(applicationContext, "Next game night: ${day.display_date}")
                    }
                }
        }
        if ("sophon" in successful && preferences[booleanPreferencesKey("temperature_notifications_enabled")] == true) {
            val low = preferences[stringPreferencesKey("temperature_low_threshold")]?.toDoubleOrNull() ?: 8.0
            val high = preferences[stringPreferencesKey("temperature_high_threshold")]?.toDoubleOrNull() ?: 25.0
            SophonSummaryStore(applicationContext).summaryFlow.first()?.devices.orEmpty().forEachIndexed { index, device ->
                val state = temperatureState(device, low, high)
                if (reliability.updateTemperatureZone(state.deviceId, state.zone) && state.message != null) {
                    notifyHome(applicationContext, 4200 + index, "Sophon temperature", state.message, "sophon")
                }
            }
        }
    }

    private suspend fun refreshMealPlanner(): Boolean {
        MealPlannerRepository(MealPlannerStore(applicationContext)).refreshInBackground()
        return true
    }

    private suspend fun refreshTodo(): Boolean {
        val store = ToDoStore(applicationContext)
        val repository = TodoRepository(store)
        val key = store.accessKeyFlow.first().trim()
        if (key.isEmpty()) return false
        val local = store.tasksFlow.first()
        val category = store.categoryFlow.first()
        val habit = store.habitFlow.first()
        val unsynced = local.filter { item -> item.id.isBlank() || item.id.any { !it.isDigit() } }
        val pushed = repository.pushItems(unsynced, key, category, habit)
        val remote = repository.fetchFromApi(key).items
        repository.fetchMeta(key)
        store.saveTasks(remote + pushed.failed)
        return true
    }

    private suspend fun refreshWorkout(): Boolean {
        val store = WorkoutStore(applicationContext)
        val key = store.accessKeyFlow.first().trim()
        if (key.isEmpty()) return false
        val repository = WorkoutRepository(store)
        store.saveDays(repository.fetchDays(key).sortedBy { it.sort_order ?: Int.MAX_VALUE })
        return true
    }

    private suspend fun refreshGameWithDave(): Boolean {
        val store = GameWithDaveStore(applicationContext)
        val key = store.accessKeyFlow.first().trim()
        if (key.isEmpty()) return false
        store.saveDashboard(GameWithDaveRepository().dashboard(key))
        return true
    }

    private suspend fun refreshSophon(): Boolean {
        val url = applicationContext.webSettingsDataStore.data.first()[sophonUrlPreferenceKey]
            ?: "http://192.168.0.234:8096"
        val summary = SophonSummaryRepository(NetworkSophonSummarySource()).fetch(url)
        SophonSummaryStore(applicationContext).save(summary)
        return true
    }
}
