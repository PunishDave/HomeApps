package com.punishdave.homeapps

class TodoRepository(
    private val store: ToDoStore,
    private val api: TodoApi = Network.todoApi
) {
    fun tasksFlow() = store.tasksFlow
    fun accessKeyFlow() = store.accessKeyFlow

    suspend fun saveTasks(tasks: List<TodoItem>) = store.saveTasks(tasks)
    suspend fun saveAccessKey(key: String) = store.saveAccessKey(key)

    suspend fun fetchFromApi(key: String): List<TodoItem> {
        if (key.isBlank()) return emptyList()
        val remote = api.listItems(key = key)
        return remote.map {
            TodoItem(
                id = it.id.toString(),
                title = it.title,
                done = it.status.equals("done", ignoreCase = true)
            )
        }
    }

    suspend fun updateStatusRemote(id: String, done: Boolean, key: String): TodoItem? {
        val intId = id.toIntOrNull() ?: return null
        val status = if (done) "done" else "pending"
        val remote = api.updateItem(
            id = intId,
            key = key,
            body = mapOf("status" to status)
        )
        return TodoItem(
            id = remote.id.toString(),
            title = remote.title,
            done = remote.status.equals("done", ignoreCase = true)
        )
    }
}
