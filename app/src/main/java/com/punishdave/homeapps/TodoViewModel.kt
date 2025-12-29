package com.punishdave.homeapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val done: Boolean = false
)

class TodoViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TodoRepository(ToDoStore(app.applicationContext))

    val tasks = repo.tasksFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accessKey = repo.accessKeyFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val newTaskText = MutableStateFlow("")
    val lastError = MutableStateFlow<String?>(null)

    fun addTask() = viewModelScope.launch {
        val title = newTaskText.value.trim()
        if (title.isEmpty()) {
            lastError.value = "Enter a task name first."
            return@launch
        }

        val updated = listOf(TodoItem(title = title)) + tasks.value
        repo.saveTasks(updated)
        newTaskText.value = ""
    }

    fun saveAccessKey(key: String) = viewModelScope.launch {
        repo.saveAccessKey(key.trim())
    }

    fun syncFromApi() = viewModelScope.launch {
        val key = accessKey.value.trim()
        if (key.isEmpty()) {
            lastError.value = "Enter the access key first."
            return@launch
        }
        try {
            val remote = repo.fetchFromApi(key)
            repo.saveTasks(remote)
        } catch (e: Exception) {
            lastError.value = e.message ?: "Sync failed"
        }
    }

    fun toggleTask(id: String) = viewModelScope.launch {
        val updated = tasks.value.map {
            if (it.id == id) it.copy(done = !it.done) else it
        }
        repo.saveTasks(updated)
    }

    fun deleteTask(id: String) = viewModelScope.launch {
        val updated = tasks.value.filterNot { it.id == id }
        repo.saveTasks(updated)
    }
}
