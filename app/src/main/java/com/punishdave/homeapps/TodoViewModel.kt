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
    val isSyncing = MutableStateFlow(false)
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
        val cat = category.value.trim()
        val hab = habit.value.trim()
        if (key.isEmpty()) {
            lastError.value = "Enter the access key first."
            lastSyncStatus.value = "Sync skipped: no access key."
            return@launch
        }
        isSyncing.value = true
        lastSyncStatus.value = "Syncing..."
        try {
            val localBefore = tasks.value
            val remoteInitial = repo.fetchFromApi(key)

            // Push new local items that do not have numeric IDs.
            val newLocal = localBefore.filter { item ->
                item.id.isBlank() || item.id.any { ch -> !ch.isDigit() }
            }
            val pushed = repo.pushItems(newLocal, key, cat, hab)
            val remoteAfterPush = if (pushed.created.isNotEmpty()) repo.fetchFromApi(key) else emptyList()

            // Decide final remote: prefer post-push fetch; if empty, fall back to initial.
            val remoteLatest = if (remoteAfterPush.isNotEmpty()) remoteAfterPush else remoteInitial

            val (cats, habits) = repo.fetchMeta(key)
            repo.clearTasks()
            val failedExtras = pushed.failed.filter { it.id.isBlank() || it.id.any { ch -> !ch.isDigit() } }

            // Preserve completion/due where ids match.
            val localMap = localBefore.associateBy { it.id }
            val mergedRemote = remoteLatest.map { remoteItem ->
                val localItem = localMap[remoteItem.id]
                if (localItem != null) {
                    remoteItem.copy(
                        done = localItem.done,
                        dueDate = remoteItem.dueDate ?: localItem.dueDate
                    )
                } else {
                    remoteItem
                }
            }

            repo.saveTasks(mergedRemote + failedExtras)
            if (cats.isNotEmpty() && category.value.isBlank()) {
                repo.saveCategory(cats.first())
            }
            if (habits.isNotEmpty() && habit.value.isBlank()) {
                repo.saveHabit(habits.first())
            }
            lastError.value = null
            val ids = remoteLatest.joinToString(",") { it.id }
            val failedCount = pushed.failed.size
            val failedMsg = if (failedCount > 0) " (failed to push $failedCount)" else ""
            lastSyncStatus.value = "Synced ${remoteLatest.size} items [ids: $ids]; pushed ${pushed.created.size}$failedMsg; categories ${cats.size}; habits ${habits.size}."
        } catch (e: Exception) {
            val msg = httpMessage(e, "Sync failed")
            lastError.value = msg
            lastSyncStatus.value = msg
        } finally {
            isSyncing.value = false
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
        val key = accessKey.value.trim()
        val updated = tasks.value.filterNot { it.id == id }

        if (key.isNotEmpty() && id.any { it.isDigit() }) {
            runCatching { repo.deleteRemote(id, key) }
                .onFailure { e -> lastError.value = httpMessage(e, "Delete failed") }
        }

        repo.saveTasks(updated)
    }

    fun clearLocalData() = viewModelScope.launch {
        repo.clearTasks()
        lastSyncStatus.value = "Local tasks cleared."
    }

    private fun mergeRemoteWithLocal(remote: List<TodoItem>, local: List<TodoItem>, failedPushes: List<TodoItem> = emptyList()): List<TodoItem> {
        val localMap = local.associateBy { it.id }

        val merged = remote.map { remoteItem ->
            val localItem = localMap[remoteItem.id]
            if (localItem != null) {
                remoteItem.copy(
                    done = localItem.done,
                    dueDate = remoteItem.dueDate ?: localItem.dueDate
                )
            } else {
                remoteItem
            }
        }.toMutableList()

        failedPushes.forEach { failed ->
            if (failed.id.isBlank() || failed.id.any { !it.isDigit() }) {
                merged.add(failed)
            }
        }

        return merged
    }

    private fun httpMessage(e: Throwable, fallback: String): String {
        return if (e is HttpException) {
            "HTTP ${e.code()}: ${e.message()}"
        } else {
            e.message ?: fallback
        }
    }
}
