package com.punishdave.homeapps

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TodoRepository(
    private val store: ToDoStore,
    private val api: TodoApi = Network.todoApi
) {
    fun tasksFlow() = store.tasksFlow
    fun accessKeyFlow() = store.accessKeyFlow
    fun categoryFlow() = store.categoryFlow
    fun habitFlow() = store.habitFlow

    suspend fun saveTasks(tasks: List<TodoItem>) = withContext(Dispatchers.IO) { store.saveTasks(tasks) }
    suspend fun saveAccessKey(key: String) = withContext(Dispatchers.IO) { store.saveAccessKey(key) }
    suspend fun saveCategory(cat: String) = withContext(Dispatchers.IO) { store.saveCategory(cat) }
    suspend fun saveHabit(habit: String) = withContext(Dispatchers.IO) { store.saveHabit(habit) }

    suspend fun fetchFromApi(key: String): List<TodoItem> {
        if (key.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            val remote = api.listItems(key = key)
            remote.map {
                TodoItem(
                    id = it.id.toString(),
                    title = it.title,
                    done = it.status.equals("done", ignoreCase = true)
                )
            }
        }
    }

    suspend fun updateStatusRemote(id: String, done: Boolean, key: String): TodoItem? {
        val intId = id.toIntOrNull() ?: return null
        val status = if (done) "done" else "pending"
        val remote = withContext(Dispatchers.IO) {
            api.updateItem(
                id = intId,
                key = key,
                body = mapOf("status" to status)
            )
        }
        return TodoItem(
            id = remote.id.toString(),
            title = remote.title,
            done = remote.status.equals("done", ignoreCase = true)
        )
    }

    suspend fun createRemote(title: String, key: String, category: String, habit: String): TodoItem? {
        if (title.isBlank()) return null
        return withContext(Dispatchers.IO) {
            val remote = api.createItem(
                key = key,
                body = buildMap {
                    put("title", title)
                    if (category.isNotBlank()) put("category", category)
                    if (habit.isNotBlank()) put("habit", habit)
                }
            )
            TodoItem(
                id = remote.id.toString(),
                title = remote.title,
                done = remote.status.equals("done", ignoreCase = true)
            )
        }
    }
}
