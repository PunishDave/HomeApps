package com.punishdave.homeapps

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "meal_planner_store")

class MealPlannerStore(private val context: Context) {
    private val KEY_RECIPES = stringPreferencesKey("recipes_json")
    private val KEY_CURRENT_WEEK = stringPreferencesKey("current_week_json")
    private val KEY_PLANNED_WEEK = stringPreferencesKey("planned_week_json")
    private val KEY_SHOPPING_LIST = stringPreferencesKey("shopping_list_json")
    private val KEY_ACCESS_KEY = stringPreferencesKey("meal_planner_access_key")

    val recipesFlow: Flow<List<Recipe>> = context.dataStore.data.map { prefs ->
        prefs[KEY_RECIPES]?.let { Network.recipesAdapter.fromJson(it) } ?: emptyList()
    }

    val currentWeekFlow: Flow<WeekResponse?> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENT_WEEK]?.let { Network.weekAdapter.fromJson(it) }
    }

    val plannedWeekFlow: Flow<WeekResponse?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PLANNED_WEEK]?.let { Network.weekAdapter.fromJson(it) }
    }

    val shoppingListFlow: Flow<ShoppingListResponse?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOPPING_LIST]?.let { Network.shoppingListAdapter.fromJson(it) }
    }

    val accessKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        CredentialCipher.decrypt(prefs[KEY_ACCESS_KEY] ?: "")
    }

    suspend fun saveRecipes(recipes: List<Recipe>) {
        context.dataStore.edit { it[KEY_RECIPES] = Network.recipesAdapter.toJson(recipes) }
    }

    suspend fun saveCurrentWeek(week: WeekResponse) {
        context.dataStore.edit { it[KEY_CURRENT_WEEK] = Network.weekAdapter.toJson(week) }
    }

    suspend fun clearCurrentWeek() {
        context.dataStore.edit { it.remove(KEY_CURRENT_WEEK) }
    }

    suspend fun savePlannedWeek(week: WeekResponse) {
        context.dataStore.edit { it[KEY_PLANNED_WEEK] = Network.weekAdapter.toJson(week) }
    }

    suspend fun clearPlannedWeek() {
        context.dataStore.edit { it.remove(KEY_PLANNED_WEEK) }
    }

    suspend fun saveShoppingList(list: ShoppingListResponse) {
        context.dataStore.edit { it[KEY_SHOPPING_LIST] = Network.shoppingListAdapter.toJson(list) }
    }

    suspend fun clearShoppingList() {
        context.dataStore.edit { it.remove(KEY_SHOPPING_LIST) }
    }

    suspend fun saveAccessKey(key: String) {
        context.dataStore.edit { it[KEY_ACCESS_KEY] = CredentialCipher.encrypt(key) }
    }
}
