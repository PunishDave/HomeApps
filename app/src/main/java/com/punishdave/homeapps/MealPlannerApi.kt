package com.punishdave.homeapps

import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface MealPlannerApi {
    @GET("weeks")
    suspend fun getWeek(
        @Query("week_start") weekStart: String,
        @Query("_ts") cacheBuster: Long? = null
    ): WeekResponse

    @GET("random-week")
    suspend fun getRandomWeek(
        @Query("_ts") cacheBuster: Long? = null
    ): List<Recipe>

    @POST("weeks")
    suspend fun postWeek(
        @Body body: WeekPostRequest,
        @Query("_ts") cacheBuster: Long? = null
    ): WeekResponse

    @GET("recipes")
    suspend fun getRecipes(
        @Query("_ts") cacheBuster: Long? = null
    ): List<Recipe>
}

@JsonClass(generateAdapter = true)
data class Recipe(
    val id: Int,
    val title: String,
    val ingredients: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WeekResponse(
    val id: Int? = null,
    val week_start: String,
    val meals: List<Recipe> // <-- IMPORTANT: API returns an array
)

/**
 * Keep POST compatible with your example:
 * "meals": { "0": {...}, "1": {...}, ... }
 */
@JsonClass(generateAdapter = true)
data class WeekPostRequest(
    val week_start: String,
    val meals: Map<String, Recipe>
)
