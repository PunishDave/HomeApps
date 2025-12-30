package com.punishdave.homeapps

import com.squareup.moshi.JsonClass
import retrofit2.http.GET

interface WorkoutApi {
    @GET("days")
    suspend fun listDays(): List<WorkoutDay>
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
