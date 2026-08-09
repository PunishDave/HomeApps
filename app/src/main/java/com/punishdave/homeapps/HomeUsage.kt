package com.punishdave.homeapps

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.homeUsageDataStore by preferencesDataStore(name = "home_usage")

val homeSectionIds = listOf(
    "meal_planner",
    "have_we_got",
    "todo",
    "workout",
    "gamewithdave",
    "transmission",
    "wallfacer",
    "sophon",
    "droplet"
)

class HomeUsageStore(private val context: Context) {
    val counts = context.homeUsageDataStore.data.map { preferences ->
        homeSectionIds.associateWith { id -> preferences[intPreferencesKey("opens_$id")] ?: 0 }
    }

    suspend fun recordOpen(id: String) {
        if (id !in homeSectionIds) return
        val key = intPreferencesKey("opens_$id")
        context.homeUsageDataStore.edit { preferences ->
            preferences[key] = (preferences[key] ?: 0) + 1
        }
    }
}

class HomeUsageViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HomeUsageStore(app.applicationContext)
    val counts = store.counts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        homeSectionIds.associateWith { 0 }
    )

    fun recordOpen(id: String) = viewModelScope.launch { store.recordOpen(id) }
}
