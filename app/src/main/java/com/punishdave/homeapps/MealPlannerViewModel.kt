package com.punishdave.homeapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class MealPlannerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MealPlannerRepository(MealPlannerStore(app.applicationContext))

    val recipes = repo.recipesFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val currentWeek = repo.currentWeekFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val plannedWeek = repo.plannedWeekFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val shoppingList = repo.shoppingListFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val accessKey = repo.accessKeyFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    // simple UI flags
    val isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val lastError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val shoppingSyncStatus = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val newShoppingItem = kotlinx.coroutines.flow.MutableStateFlow("")

    fun sync() = viewModelScope.launch {
        isSyncing.value = true
        lastError.value = null
        try {
            // If a planned week exists, push it first
            plannedWeek.value?.let { week ->
                val key = accessKey.value
                if (key.isBlank()) {
                    lastError.value = "Enter the access key to save weeks."
                } else {
                    repo.savePlannedWeekToServer(week, key)
                    repo.clearPlannedWeek()
                }
            }
            repo.sync()
        } catch (e: Exception) {
            lastError.value = e.message ?: "Sync failed"
        } finally {
            isSyncing.value = false
        }
    }

    fun savePlannedWeekLocal(week: WeekResponse) = viewModelScope.launch {
        try {
            repo.saveLocalPlannedWeek(week)
        } catch (e: Exception) {
            lastError.value = e.message ?: "Save failed"
        }
    }

    fun generateWeek() = viewModelScope.launch {
        try {
            repo.generateRandomWeek(currentStartDay())
        } catch (e: Exception) {
            lastError.value = e.message ?: "Generate failed"
        }
    }

    fun saveWeek() = viewModelScope.launch {
        if (plannedWeek.value?.id != null) return@launch
        val week = plannedWeek.value ?: return@launch
        try {
            val key = accessKey.value
            if (key.isBlank()) {
                lastError.value = "Enter the access key to save weeks."
                return@launch
            }
            repo.savePlannedWeekToServer(week, key)
        } catch (e: Exception) {
            lastError.value = e.message ?: "Save failed"
        }
    }

    fun saveAccessKey(key: String) = viewModelScope.launch {
        repo.saveAccessKey(key.trim())
    }

    fun addShoppingItem() = viewModelScope.launch {
        val input = newShoppingItem.value.trim()
        if (input.isEmpty()) return@launch
        val weekStart = shoppingWeekStart()
        val current = shoppingListForWeek(weekStart)
        repo.saveLocalShoppingList(weekStart, current + input)
        newShoppingItem.value = ""
    }

    fun removeShoppingItem(index: Int) = viewModelScope.launch {
        val weekStart = shoppingWeekStart()
        val current = shoppingListForWeek(weekStart)
        if (index !in current.indices) return@launch
        val updated = current.toMutableList().apply { removeAt(index) }
        repo.saveLocalShoppingList(weekStart, updated)
    }

    fun syncShoppingList() = viewModelScope.launch {
        isSyncing.value = true
        lastError.value = null
        shoppingSyncStatus.value = "Syncing shopping list..."
        try {
            val targetWeek = shoppingWeekStart()
            val key = accessKey.value.trim()

            // Fetch the list for the target week (server may canonicalize the start day).
            val remote = repo.fetchShoppingList(targetWeek)
            val canonicalWeek = remote.week_start

            // Only merge local items that match the canonical week to avoid bringing stale weeks back.
            val local = shoppingListForWeek(canonicalWeek)
            val merged = mergeLists(remote.shopping_list, local)
            repo.saveLocalShoppingList(canonicalWeek, merged)
            shoppingSyncStatus.value = "Fetched list for $canonicalWeek (${remote.shopping_list.size} remote, ${merged.size} merged)."

            // If we have an access key, push the merged list back up.
            if (key.isNotEmpty()) {
                val saved = repo.pushShoppingList(canonicalWeek, merged, key)
                shoppingSyncStatus.value = "Saved ${saved.shopping_list.size} items for ${saved.week_start}."
            } else {
                shoppingSyncStatus.value = "Fetched list for $canonicalWeek. Add the access key to push changes."
            }
        } catch (e: Exception) {
            lastError.value = e.message ?: "Sync failed"
            shoppingSyncStatus.value = lastError.value
        } finally {
            isSyncing.value = false
        }
    }

    private fun currentStartDay(): DayOfWeek {
        plannedWeek.value?.week_start?.let { return LocalDate.parse(it).dayOfWeek }
        currentWeek.value?.week_start?.let { return LocalDate.parse(it).dayOfWeek }
        return DayOfWeek.SATURDAY
    }

    fun shoppingWeekStart(): String {
        // Prefer explicitly planned upcoming week if present.
        plannedWeek.value?.week_start?.let { return it }

        // Default: always use next week's start date based on the configured start day.
        val startDay = currentStartDay()
        return repo.computeWeekStartIso(LocalDate.now().plusDays(7), startDay)
    }

    private fun mergeLists(remote: List<String>, local: List<String>): List<String> {
        if (local.isEmpty()) return remote
        val trimmedRemote = remote.map { it.trim() }
        val trimmedLocal = local.map { it.trim() }
        val merged = trimmedRemote.toMutableList()
        trimmedLocal.forEach { item ->
            if (item.isNotEmpty() && !merged.contains(item)) merged += item
        }
        return merged
    }

    private fun shoppingListForWeek(weekStart: String): List<String> {
        val list = shoppingList.value
        if (list?.week_start == weekStart) return list.shopping_list
        return emptyList()
    }
}
