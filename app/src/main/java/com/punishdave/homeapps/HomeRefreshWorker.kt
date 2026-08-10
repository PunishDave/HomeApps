package com.punishdave.homeapps

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout

class HomeRefreshWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {

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
        Result.success()
    }.getOrElse { Result.retry() }

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
