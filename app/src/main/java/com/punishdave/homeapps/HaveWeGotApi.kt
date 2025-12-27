package com.punishdave.homeapps

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface HaveWeGotApi {
    @GET("summary")
    suspend fun getSummary(): HaveWeGotSummary

    @GET("items")
    suspend fun listItems(
        @Query("type") type: String? = null,     // "film", "tvshow", or null for all
        @Query("status") status: String? = null, // optional status filter
        @Query("search") search: String? = null, // partial name search
        @Query("order") order: String? = null,   // e.g., "last_access_desc"
        @Query("limit") limit: Int? = 200
    ): List<HaveWeGotItem>
}

@JsonClass(generateAdapter = true)
data class HaveWeGotSummary(
    val total: Int = 0,
    val by_type: Map<String, Int> = emptyMap(),
    val by_status: Map<String, Int> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class HaveWeGotItem(
    val id: Int,
    val item_type: String,
    val status: String,
    val name: String,
    val last_access: String?,
    val created_at: String,
    val updated_at: String
)
