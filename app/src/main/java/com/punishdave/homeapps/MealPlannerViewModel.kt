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
        val current = shoppingList.value?.shopping_list.orEmpty()
        repo.saveLocalShoppingList(weekStart, current + input)
        newShoppingItem.value = ""
    }

    fun removeShoppingItem(index: Int) = viewModelScope.launch {
        val current = shoppingList.value?.shopping_list.orEmpty()
        if (index !in current.indices) return@launch
        val weekStart = shoppingWeekStart()
        val updated = current.toMutableList().apply { removeAt(index) }
        repo.saveLocalShoppingList(weekStart, updated)
    }

    fun syncShoppingList() = viewModelScope.launch {
        isSyncing.value = true
        lastError.value = null
        shoppingSyncStatus.value = "Syncing shopping list..."
        try {
            val weekStart = shoppingWeekStart()
            val hasLocalList = shoppingList.value != null
            val items = shoppingList.value?.shopping_list.orEmpty()
            val key = accessKey.value.trim()
            if (key.isEmpty()) {
                val remote = repo.fetchShoppingList(weekStart)
                val merged = mergeLists(remote.shopping_list, items)
                repo.saveLocalShoppingList(remote.week_start, merged)
                shoppingSyncStatus.value = "Fetched list for ${remote.week_start}. Add the access key to push changes."
                return@launch
            }

            if (items.isNotEmpty() || hasLocalList) {
                val saved = repo.pushShoppingList(weekStart, items, key)
                shoppingSyncStatus.value = "Saved ${saved.shopping_list.size} items for ${saved.week_start}."
            } else {
                val remote = repo.fetchShoppingList(weekStart)
                shoppingSyncStatus.value = "Fetched list for ${remote.week_start} (${remote.shopping_list.size} items)."
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

    private fun shoppingWeekStart(): String {
        shoppingList.value?.week_start?.let { return it }
        currentWeek.value?.week_start?.let { return it }
        plannedWeek.value?.week_start?.let { return it }
        return repo.computeWeekStartIso(LocalDate.now(), currentStartDay())
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
}
