package com.punishdave.homeapps

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class TodoRepository(
    private val store: ToDoStore
) {
    companion object {
        const val DEFAULT_TODO_BASE = "https://apm.d4c.myftpupload.com/index.php/wp-json/pd-todo/v1/"
    }

    private var cachedBase: String? = null
    private var cachedApi: TodoApi? = null

    data class PushResult(val created: List<TodoItem>, val failed: List<TodoItem>)

    fun tasksFlow() = store.tasksFlow
    fun accessKeyFlow() = store.accessKeyFlow
    fun categoryFlow() = store.categoryFlow
    fun habitFlow() = store.habitFlow
    fun categoryOptionsFlow() = store.categoryOptionsFlow
    fun habitOptionsFlow() = store.habitOptionsFlow
    fun baseUrlFlow() = store.baseUrlFlow

    suspend fun saveTasks(tasks: List<TodoItem>) = withContext(Dispatchers.IO) { store.saveTasks(tasks) }
    suspend fun clearTasks() = withContext(Dispatchers.IO) { store.clearTasks() }
    suspend fun saveAccessKey(key: String) = withContext(Dispatchers.IO) { store.saveAccessKey(key) }
    suspend fun saveCategory(cat: String) = withContext(Dispatchers.IO) { store.saveCategory(cat) }
    suspend fun saveHabit(habit: String) = withContext(Dispatchers.IO) { store.saveHabit(habit) }
    suspend fun saveCategoryOptions(categories: List<String>) = withContext(Dispatchers.IO) { store.saveCategoryOptions(categories) }
    suspend fun saveHabitOptions(habits: List<String>) = withContext(Dispatchers.IO) { store.saveHabitOptions(habits) }
    suspend fun saveBaseUrl(url: String) = withContext(Dispatchers.IO) { store.saveBaseUrl(url) }

    suspend fun fetchFromApi(key: String): List<TodoItem> {
        if (key.isBlank()) return emptyList()
        val api = api()
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
        val api = api()
        return withContext(Dispatchers.IO) {
            val categories = runCatching { api.listCategories(key = key) }.getOrElse { emptyList() }
            val habits = runCatching { api.listHabits(key = key) }.getOrElse { emptyList() }
            store.saveCategoryOptions(categories)
            store.saveHabitOptions(habits)
            categories to habits
        }
    }

    suspend fun pushItems(items: List<TodoItem>, key: String, category: String, habit: String): PushResult {
        if (key.isBlank() || items.isEmpty()) return PushResult(emptyList(), emptyList())
        val created = mutableListOf<TodoItem>()
        val failed = mutableListOf<TodoItem>()

        val categoryOptions = store.categoryOptionsFlow.firstOrNull().orEmpty()
        val habitOptions = store.habitOptionsFlow.firstOrNull().orEmpty()
        val effectiveCategory = if (category.isNotBlank()) category else categoryOptions.firstOrNull().orEmpty()
        val effectiveHabit = if (habit.isNotBlank()) habit else habitOptions.firstOrNull().orEmpty()
        for (item in items) {
            val remote = runCatching {
                createRemote(
                    title = item.title,
                    key = key,
                    category = effectiveCategory,
                    habit = effectiveHabit,
                    dueDate = item.dueDate.orEmpty()
                )
            }.getOrNull()

            if (remote == null) {
                failed += item
                continue
            }

            var finalItem = remote
            if (item.done && !remote.done) {
                val updated = runCatching { updateStatusRemote(remote.id, true, key) }.getOrNull()
                if (updated != null) {
                    finalItem = updated
                }
            }
            created += finalItem
        }
        return PushResult(created, failed)
    }

    suspend fun updateStatusRemote(id: String, done: Boolean, key: String): TodoItem? {
        val intId = id.toIntOrNull() ?: return null
        val status = if (done) "done" else "pending"
        val api = api()
        val remote = withContext(Dispatchers.IO) {
            api.updateItem(
                id = intId,
                key = key,
                body = mapOf("status" to status)
            )
        }
        return remote.toLocal()
    }

    suspend fun deleteRemote(id: String, key: String): Boolean {
        val intId = id.toIntOrNull() ?: return false
        val api = api()
        return runCatching {
            withContext(Dispatchers.IO) {
                api.deleteItem(
                    id = intId,
                    key = key
                )
            }
        }.isSuccess
    }

    suspend fun createRemote(title: String, key: String, category: String, habit: String, dueDate: String): TodoItem? {
        if (title.isBlank()) return null
        val api = api()
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

    private suspend fun api(): TodoApi {
        val base = store.baseUrlFlow.firstOrNull().orEmpty().ifBlank { DEFAULT_TODO_BASE }
        if (cachedApi == null || cachedBase != base) {
            cachedApi = Retrofit.Builder()
                .baseUrl(ensureTrailingSlash(base))
                .addConverterFactory(MoshiConverterFactory.create(Network.moshi))
                .client(Network.client)
                .build()
                .create(TodoApi::class.java)
            cachedBase = base
        }
        return cachedApi!!
    }

    private fun ensureTrailingSlash(base: String): String =
        if (base.endsWith("/")) base else "$base/"
}
