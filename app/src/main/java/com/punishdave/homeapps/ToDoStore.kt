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
    private val KEY_CATEGORY_OPTIONS = stringPreferencesKey("todo_category_options")
    private val KEY_HABIT_OPTIONS = stringPreferencesKey("todo_habit_options")
    private val KEY_BASE_URL = stringPreferencesKey("todo_base_url")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, TodoItem::class.java)
    private val adapter = moshi.adapter<List<TodoItem>>(listType)
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

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

    val baseUrlFlow: Flow<String> = context.todoDataStore.data.map { prefs ->
        prefs[KEY_BASE_URL] ?: ""
    }

    val categoryOptionsFlow: Flow<List<String>> = context.todoDataStore.data.map { prefs ->
        val raw = prefs[KEY_CATEGORY_OPTIONS]
        if (raw.isNullOrEmpty()) return@map emptyList()
        runCatching { stringListAdapter.fromJson(raw) ?: emptyList() }.getOrElse { emptyList() }
    }

    val habitOptionsFlow: Flow<List<String>> = context.todoDataStore.data.map { prefs ->
        val raw = prefs[KEY_HABIT_OPTIONS]
        if (raw.isNullOrEmpty()) return@map emptyList()
        runCatching { stringListAdapter.fromJson(raw) ?: emptyList() }.getOrElse { emptyList() }
    }

    suspend fun saveTasks(tasks: List<TodoItem>) {
        context.todoDataStore.edit { it[KEY_TASKS] = adapter.toJson(tasks) }
    }

    suspend fun clearTasks() {
        context.todoDataStore.edit { it.remove(KEY_TASKS) }
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

    suspend fun saveCategoryOptions(categories: List<String>) {
        context.todoDataStore.edit { it[KEY_CATEGORY_OPTIONS] = stringListAdapter.toJson(categories) }
    }

    suspend fun saveHabitOptions(habits: List<String>) {
        context.todoDataStore.edit { it[KEY_HABIT_OPTIONS] = stringListAdapter.toJson(habits) }
    }

    suspend fun saveBaseUrl(url: String) {
        context.todoDataStore.edit { it[KEY_BASE_URL] = url }
    }
}
