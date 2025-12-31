package com.punishdave.homeapps

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import com.squareup.moshi.Json

interface WorkoutApi {
    @GET("days")
    suspend fun listDays(
        @Header("X-PD-SWL-Key") key: String? = null,
        @Header("X-PDSWL-Key") altHeader: String? = null,
        @Header("Authorization") bearer: String? = null,
        @Query("pd_swl_key") keyQuery: String? = key,
        @Query("pdswl_key") altQuery: String? = key,
        @Query("key") plainKey: String? = key,
        @Query("access_key") accessKey: String? = key,
        @Query("_ts") cacheBuster: Long? = null
    ): List<WorkoutDay>

    @GET("days/{dayKey}")
    suspend fun getDay(
        @Path("dayKey") dayKey: String,
        @Header("X-PD-SWL-Key") key: String? = null,
        @Header("X-PDSWL-Key") altHeader: String? = null,
        @Header("Authorization") bearer: String? = null,
        @Query("pd_swl_key") keyQuery: String? = key,
        @Query("pdswl_key") altQuery: String? = key,
        @Query("key") plainKey: String? = key,
        @Query("access_key") accessKey: String? = key,
        @Query("_ts") cacheBuster: Long? = null
    ): WorkoutDay

    @GET("days/{dayKey}/latest")
    suspend fun getDayLatest(
        @Path("dayKey") dayKey: String,
        @Header("X-PD-SWL-Key") key: String? = null,
        @Header("X-PDSWL-Key") altHeader: String? = null,
        @Header("Authorization") bearer: String? = null,
        @Query("pd_swl_key") keyQuery: String? = key,
        @Query("pdswl_key") altQuery: String? = key,
        @Query("key") plainKey: String? = key,
        @Query("access_key") accessKey: String? = key,
        @Query("_ts") cacheBuster: Long? = null
    ): WorkoutLatestResponse

    @POST("days/{dayKey}")
    suspend fun upsertDay(
        @Path("dayKey") dayKey: String,
        @Header("X-PD-SWL-Key") key: String? = null,
        @Header("X-PDSWL-Key") altHeader: String? = null,
        @Header("Authorization") bearer: String? = null,
        @Query("pd_swl_key") keyQuery: String? = key,
        @Query("pdswl_key") altQuery: String? = key,
        @Query("key") plainKey: String? = key,
        @Query("access_key") accessKey: String? = key,
        @Body body: WorkoutDayPost
    ): WorkoutDay

    @POST("entries/push")
    suspend fun pushEntries(
        @Header("X-PD-SWL-Key") key: String? = null,
        @Header("X-PDSWL-Key") altHeader: String? = null,
        @Header("Authorization") bearer: String? = null,
        @Query("pd_swl_key") keyQuery: String? = key,
        @Query("pdswl_key") altQuery: String? = key,
        @Query("key") plainKey: String? = key,
        @Query("access_key") accessKey: String? = key,
        @Body body: List<WorkoutEntryPayload>
    )
}

@JsonClass(generateAdapter = true)
data class WorkoutDayPost(
    val label: String,
    val icon: String? = null,
    val sort_order: Int? = null,
    val workouts: List<WorkoutMove>
)

@JsonClass(generateAdapter = true)
data class WorkoutEntryPayload(
    val workout: String,
    val weight: String? = null,
    val reps: Int? = null,
    val performed_on: String,
    val notes: String? = null,
    @Json(name = "day_key") val day_key: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkoutDay(
    val id: Int,
    val day_key: String,
    val label: String,
    val icon: String? = null,
    val workouts: List<WorkoutMove> = emptyList(),
    val sort_order: Int? = null
)

@JsonClass(generateAdapter = true)
data class WorkoutMove(
    val name: String,
    val type: String? = null,
    val reps: Int? = null,
    val last_weight: String? = null,
    val last_reps: Int? = null,
    val last_performed_on: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkoutLatestEntry(
    val id: Int,
    @Json(name = "day_key") val dayKey: String? = null,
    val workout: String,
    val weight: String? = null,
    val reps: Int? = null,
    @Json(name = "performed_on") val performedOn: String? = null,
    val notes: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkoutLatestResponse(
    val day: WorkoutDay? = null,
    val latest: List<WorkoutLatestEntry> = emptyList()
)
