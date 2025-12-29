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
}
