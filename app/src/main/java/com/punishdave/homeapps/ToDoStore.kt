package com.punishdave.homeapps

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

private val Context.todoDataStore by preferencesDataStore(name = "todo_store")

class ToDoStore(private val context: Context) {
    private val KEY_TASKS = stringPreferencesKey("todo_tasks_json")

    private val moshi: Moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, TodoItem::class.java)
    private val adapter = moshi.adapter<List<TodoItem>>(listType)

    val tasksFlow: Flow<List<TodoItem>> = context.todoDataStore.data.map { prefs ->
        prefs[KEY_TASKS]?.let { adapter.fromJson(it) } ?: emptyList()
    }

    suspend fun saveTasks(tasks: List<TodoItem>) {
        context.todoDataStore.edit { it[KEY_TASKS] = adapter.toJson(tasks) }
    }
}
