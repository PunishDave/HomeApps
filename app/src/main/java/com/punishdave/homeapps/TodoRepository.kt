package com.punishdave.homeapps

class TodoRepository(private val store: ToDoStore) {
    fun tasksFlow() = store.tasksFlow

    suspend fun saveTasks(tasks: List<TodoItem>) {
        store.saveTasks(tasks)
    }
}
