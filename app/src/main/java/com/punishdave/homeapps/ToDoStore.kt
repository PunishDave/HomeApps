package com.punishdave.homeapps

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private val Context.todoDataStore by preferencesDataStore(name = "todo_store")

class ToDoStore(private val context: Context) {
    private val KEY_TASKS = stringPreferencesKey("todo_tasks_json")
    private val KEY_ACCESS = stringPreferencesKey("todo_access_key")
    private val KEY_CATEGORY = stringPreferencesKey("todo_category")
    private val KEY_HABIT = stringPreferencesKey("todo_habit")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, TodoItem::class.java)
    private val adapter = moshi.adapter<List<TodoItem>>(listType)

    val tasksFlow: Flow<List<TodoItem>> = context.todoDataStore.data.map { prefs ->
        val raw = prefs[KEY_TASKS]
        if (raw.isNullOrEmpty()) return@map emptyList()
        runCatching { adapter.fromJson(raw) ?: emptyList() }.getOrElse { emptyList() }
    }

    val accessKeyFlow: Flow<String> = context.todoDataStore.data.map { prefs ->
        prefs[KEY_ACCESS] ?: ""
    }

    val categoryFlow: Flow<String> = context.todoDataStore.data.map { prefs ->
        prefs[KEY_CATEGORY] ?: ""
    }

    val habitFlow: Flow<String> = context.todoDataStore.data.map { prefs ->
        prefs[KEY_HABIT] ?: ""
    }

    suspend fun saveTasks(tasks: List<TodoItem>) {
        context.todoDataStore.edit { it[KEY_TASKS] = adapter.toJson(tasks) }
    }

    suspend fun saveAccessKey(key: String) {
        context.todoDataStore.edit { it[KEY_ACCESS] = key }
    }

    suspend fun saveCategory(cat: String) {
        context.todoDataStore.edit { it[KEY_CATEGORY] = cat }
    }

    suspend fun saveHabit(habit: String) {
        context.todoDataStore.edit { it[KEY_HABIT] = habit }
    }
}
