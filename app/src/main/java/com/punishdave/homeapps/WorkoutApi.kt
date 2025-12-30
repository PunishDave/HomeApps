package com.punishdave.homeapps

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface WorkoutApi {
    @GET("days")
    suspend fun listDays(
        @Header("X-PD-SWL-Key") key: String? = null,
        @Query("pd_swl_key") keyQuery: String? = key
    ): List<WorkoutDay>
}

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
    val type: String? = null
)
