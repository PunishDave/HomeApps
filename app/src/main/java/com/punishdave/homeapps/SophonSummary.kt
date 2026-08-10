package com.punishdave.homeapps

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

@JsonClass(generateAdapter = true)
data class SophonSummaryResponse(
    val devices: List<SophonDevice> = emptyList(),
    val summary: SophonCounts = SophonCounts()
)

@JsonClass(generateAdapter = true)
data class SophonDevice(
    val device_id: String,
    val temperature_c: Double? = null,
    val battery_percent: Int? = null,
    val status: String = "Unknown",
    val last_seen: String? = null
)

@JsonClass(generateAdapter = true)
data class SophonCounts(val total: Int = 0, val online: Int = 0, val stale: Int = 0)

private val Context.sophonDataStore by preferencesDataStore(name = "sophon_summary_store")

class SophonSummaryStore(private val context: Context) {
    private val summaryKey = stringPreferencesKey("summary_json")
    val summaryFlow = context.sophonDataStore.data.map { preferences ->
        preferences[summaryKey]?.let { json ->
            runCatching { Network.moshi.adapter(SophonSummaryResponse::class.java).fromJson(json) }.getOrNull()
        }
    }

    suspend fun save(value: SophonSummaryResponse) {
        context.sophonDataStore.edit {
            it[summaryKey] = Network.moshi.adapter(SophonSummaryResponse::class.java).toJson(value)
        }
    }
}

class SophonSummaryViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SophonSummaryRepository(NetworkSophonSummarySource())
    private val store = SophonSummaryStore(app.applicationContext)
    val summary = store.summaryFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val error = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)

    fun refresh(url: String) = viewModelScope.launch {
        loading.value = true
        error.value = null
        try {
            store.save(repository.fetch(url))
        } catch (exception: Exception) {
            error.value = exception.message ?: "Sophon unavailable"
        } finally {
            loading.value = false
        }
    }
}

fun interface SophonSummarySource {
    suspend fun fetch(url: String): SophonSummaryResponse
}

class SophonSummaryRepository(private val source: SophonSummarySource) {
    suspend fun fetch(url: String): SophonSummaryResponse {
        require(url.isNotBlank()) { "Sophon URL is missing" }
        return source.fetch(url.trim().trimEnd('/') + "/api/summary")
    }
}

class NetworkSophonSummarySource : SophonSummarySource {
    override suspend fun fetch(url: String): SophonSummaryResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        Network.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            Network.moshi.adapter(SophonSummaryResponse::class.java)
                .fromJson(response.body?.string().orEmpty()) ?: error("Empty Sophon response")
        }
    }
}
