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
    fun categoryOptionsFlow() = store.categoryOptionsFlow
    fun habitOptionsFlow() = store.habitOptionsFlow

    suspend fun saveTasks(tasks: List<TodoItem>) = withContext(Dispatchers.IO) { store.saveTasks(tasks) }
    suspend fun saveAccessKey(key: String) = withContext(Dispatchers.IO) { store.saveAccessKey(key) }
    suspend fun saveCategory(cat: String) = withContext(Dispatchers.IO) { store.saveCategory(cat) }
    suspend fun saveHabit(habit: String) = withContext(Dispatchers.IO) { store.saveHabit(habit) }
    suspend fun saveCategoryOptions(categories: List<String>) = withContext(Dispatchers.IO) { store.saveCategoryOptions(categories) }
    suspend fun saveHabitOptions(habits: List<String>) = withContext(Dispatchers.IO) { store.saveHabitOptions(habits) }

    suspend fun fetchFromApi(key: String): List<TodoItem> {
        if (key.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            val perPage = 100
            val all = mutableListOf<TodoRemoteItem>()
            var page = 1
            while (true) {
                val batch = api.listItems(
                    key = key,
                    perPage = perPage,
                    page = page
                )
                all.addAll(batch)
                if (batch.size < perPage) break
                page += 1
            }
            all.map { it.toLocal() }
        }
    }

    suspend fun fetchMeta(key: String): Pair<List<String>, List<String>> {
        if (key.isBlank()) return emptyList<String>() to emptyList()
        return withContext(Dispatchers.IO) {
            val categories = runCatching { api.listCategories(key = key) }.getOrElse { emptyList() }
            val habits = runCatching { api.listHabits(key = key) }.getOrElse { emptyList() }
            store.saveCategoryOptions(categories)
            store.saveHabitOptions(habits)
            categories to habits
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
        return remote.toLocal()
    }

    suspend fun createRemote(title: String, key: String, category: String, habit: String, dueDate: String): TodoItem? {
        if (title.isBlank()) return null
        return withContext(Dispatchers.IO) {
            val remote = api.createItem(
                key = key,
                body = buildMap {
                    put("title", title)
                    if (category.isNotBlank()) put("category", category)
                    if (habit.isNotBlank()) put("habit", habit)
                    if (dueDate.isNotBlank()) put("due_date", dueDate)
                }
            )
            remote.toLocal()
        }
    }

    private fun TodoRemoteItem.toLocal(): TodoItem = TodoItem(
        id = id.toString(),
        title = title,
        done = status.equals("done", ignoreCase = true),
        dueDate = due_date
    )
}
