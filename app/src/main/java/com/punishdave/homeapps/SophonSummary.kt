package com.punishdave.homeapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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

class SophonSummaryViewModel : ViewModel() {
    private val repository = SophonSummaryRepository(NetworkSophonSummarySource())
    val summary = MutableStateFlow<SophonSummaryResponse?>(null)
    val error = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)

    fun refresh(url: String) = viewModelScope.launch {
        loading.value = true
        error.value = null
        try {
            summary.value = repository.fetch(url)
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
