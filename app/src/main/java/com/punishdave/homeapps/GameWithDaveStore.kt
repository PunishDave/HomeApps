package com.punishdave.homeapps

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameWithDaveDataStore by preferencesDataStore(name = "gamewithdave_store")

class GameWithDaveStore(private val context: Context) {
    private val accessKey = stringPreferencesKey("gamewithdave_access_key")

    val accessKeyFlow: Flow<String> = context.gameWithDaveDataStore.data.map { it[accessKey] ?: "" }

    suspend fun saveAccessKey(key: String) {
        context.gameWithDaveDataStore.edit { it[accessKey] = key }
    }
}
