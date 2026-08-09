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
    private val username = stringPreferencesKey("gamewithdave_username")
    private val password = stringPreferencesKey("gamewithdave_password")

    val accessKeyFlow: Flow<String> = context.gameWithDaveDataStore.data.map { CredentialCipher.decrypt(it[accessKey] ?: "") }
    val usernameFlow: Flow<String> = context.gameWithDaveDataStore.data.map { CredentialCipher.decrypt(it[username] ?: "") }
    val passwordFlow: Flow<String> = context.gameWithDaveDataStore.data.map { CredentialCipher.decrypt(it[password] ?: "") }

    suspend fun saveAccessKey(key: String) {
        context.gameWithDaveDataStore.edit { it[accessKey] = CredentialCipher.encrypt(key) }
    }

    suspend fun saveCredentials(user: String, secret: String) {
        context.gameWithDaveDataStore.edit {
            it[username] = CredentialCipher.encrypt(user)
            it[password] = CredentialCipher.encrypt(secret)
        }
    }
}
