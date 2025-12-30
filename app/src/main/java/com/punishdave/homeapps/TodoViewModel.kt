package com.punishdave.homeapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import retrofit2.HttpException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val done: Boolean = false,
    val dueDate: String? = null
)

class TodoViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TodoRepository(ToDoStore(app.applicationContext))

    val tasks = repo.tasksFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accessKey = repo.accessKeyFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val category = repo.categoryFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val habit = repo.habitFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val categoryOptions = repo.categoryOptionsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val habitOptions = repo.habitOptionsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val newTaskText = MutableStateFlow("")
    val dueDateText = MutableStateFlow(java.time.LocalDate.now().toString())
    val lastError = MutableStateFlow<String?>(null)
    val lastSyncStatus = MutableStateFlow<String?>(null)

    fun addTask() = viewModelScope.launch {
        val title = newTaskText.value.trim()
        if (title.isEmpty()) {
            lastError.value = "Enter a task name first."
            return@launch
        }

        val key = accessKey.value.trim()
        val cat = category.value.trim()
        val hab = habit.value.trim()
        val due = dueDateText.value.trim()

        if (key.isEmpty()) {
            val updated = listOf(
                TodoItem(
                    title = title,
                    dueDate = due.ifEmpty { null }
                )
            ) + tasks.value
            repo.saveTasks(updated)
            newTaskText.value = ""
            dueDateText.value = java.time.LocalDate.now().toString()
            return@launch
        }

        // Try remote create; fall back to local
        runCatching {
            val remote = repo.createRemote(title, key, cat, hab, due)
            val merged = if (remote != null) {
                listOf(remote) + tasks.value
            } else {
                listOf(
                    TodoItem(
                        title = title,
                        dueDate = due.ifEmpty { null }
                    )
                ) + tasks.value
            }
            repo.saveTasks(merged)
            newTaskText.value = ""
            dueDateText.value = java.time.LocalDate.now().toString()
        }.onFailure { e ->
            lastError.value = e.message ?: "Add failed"
            val updated = listOf(
                TodoItem(
                    title = title,
                    dueDate = due.ifEmpty { null }
                )
            ) + tasks.value
            repo.saveTasks(updated)
            newTaskText.value = ""
            dueDateText.value = java.time.LocalDate.now().toString()
        }
    }

    fun saveAccessKey(key: String) = viewModelScope.launch {
        repo.saveAccessKey(key.trim())
    }

    fun saveCategory(cat: String) = viewModelScope.launch {
        repo.saveCategory(cat.trim())
    }

    fun saveHabit(habit: String) = viewModelScope.launch {
        repo.saveHabit(habit.trim())
    }

    fun syncFromApi() = viewModelScope.launch {
        val key = accessKey.value.trim()
        if (key.isEmpty()) {
            lastError.value = "Enter the access key first."
            lastSyncStatus.value = "Sync skipped: no access key."
            return@launch
        }
        try {
            val remote = repo.fetchFromApi(key)
            val (cats, habits) = repo.fetchMeta(key)
            val local = tasks.value
            val merged = mergeRemoteWithLocal(remote, local)
            repo.saveTasks(merged)
            if (cats.isNotEmpty() && category.value.isBlank()) {
                repo.saveCategory(cats.first())
            }
            if (habits.isNotEmpty() && habit.value.isBlank()) {
                repo.saveHabit(habits.first())
            }
            lastError.value = null
            lastSyncStatus.value = "Synced ${remote.size} items; categories ${cats.size}; habits ${habits.size}."
        } catch (e: Exception) {
            val msg = httpMessage(e, "Sync failed")
            lastError.value = msg
            lastSyncStatus.value = msg
        }
    }

    fun toggleTask(id: String) = viewModelScope.launch {
        val key = accessKey.value.trim()
        val updated = tasks.value.map {
            if (it.id == id) it.copy(done = !it.done) else it
        }

        if (key.isEmpty()) {
            // Local only
            repo.saveTasks(updated)
            return@launch
        }

        runCatching {
            val target = updated.firstOrNull { it.id == id } ?: return@runCatching
            val remote = repo.updateStatusRemote(id, target.done, key)
            val merged = if (remote != null) {
                updated.map { if (it.id == id) remote else it }
            } else {
                updated
            }
            repo.saveTasks(merged)
        }.onFailure { e ->
            lastError.value = httpMessage(e, "Update failed")
            // fallback to optimistic local save
            repo.saveTasks(updated)
        }
    }

    fun deleteTask(id: String) = viewModelScope.launch {
        val updated = tasks.value.filterNot { it.id == id }
        repo.saveTasks(updated)
    }

    private fun mergeRemoteWithLocal(remote: List<TodoItem>, local: List<TodoItem>): List<TodoItem> {
        if (remote.isEmpty()) return local

        val merged = mutableMapOf<String, TodoItem>()

        // Start with remote as source of truth
        remote.forEach { merged[it.id] = it }

        // Overlay local state (preserve done state and keep locals that aren't returned)
        local.forEach { localItem ->
            val remoteItem = merged[localItem.id]
            merged[localItem.id] = when {
                remoteItem != null -> remoteItem.copy(
                    done = localItem.done,
                    dueDate = remoteItem.dueDate ?: localItem.dueDate
                )
                else -> localItem
            }
        }

        return merged.values.toList()
    }

    private fun httpMessage(e: Throwable, fallback: String): String {
        return if (e is HttpException) {
            "HTTP ${e.code()}: ${e.message()}"
        } else {
            e.message ?: fallback
        }
    }
}
