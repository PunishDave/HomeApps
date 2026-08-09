package com.punishdave.homeapps

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GameWithDaveApi {
    @GET("dashboard")
    suspend fun dashboard(
        @Header("X-GWD-Key") key: String? = null,
        @Header("Authorization") bearer: String? = null,
        @Query("access_key") accessKey: String? = key,
        @Query("_ts") cacheBuster: Long = System.currentTimeMillis()
    ): GameWithDaveDashboard

    @POST("availability")
    suspend fun saveAvailability(
        @Header("X-GWD-Key") key: String? = null,
        @Header("Authorization") bearer: String? = null,
        @Query("access_key") accessKey: String? = key,
        @Body body: GameWithDaveAvailabilityRequest
    ): GameWithDaveUpdateResponse

    @POST("game-nights/{date}/{team}")
    suspend fun updateNight(
        @Path("date") date: String,
        @Path("team") team: String,
        @Header("X-GWD-Key") key: String? = null,
        @Header("Authorization") bearer: String? = null,
        @Query("access_key") accessKey: String? = key,
        @Body body: GameWithDaveNightUpdateRequest
    ): GameWithDaveNight
}

@JsonClass(generateAdapter = true)
data class GameWithDaveDashboard(
    val users: List<GameWithDaveUser> = emptyList(),
    val teams: List<GameWithDaveTeam> = emptyList(),
    val days: List<GameWithDaveDay> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GameWithDaveUser(val role: String, val initials: String)

@JsonClass(generateAdapter = true)
data class GameWithDaveTeam(val slug: String, val name: String, val color: String? = null)

@JsonClass(generateAdapter = true)
data class GameWithDaveDay(
    val date: String,
    val display_date: String,
    val availability: List<GameWithDaveAvailability> = emptyList(),
    val game_nights: List<GameWithDaveNight> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GameWithDaveAvailability(val user_role: String, val initials: String, val status: String)

@JsonClass(generateAdapter = true)
data class GameWithDaveNight(
    val date: String,
    val team: String,
    val team_label: String,
    val status: String
)

@JsonClass(generateAdapter = true)
data class GameWithDaveAvailabilityRequest(
    val user_role: String,
    val start_date: String,
    val end_date: String,
    val status: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class GameWithDaveNightUpdateRequest(val action: String, val password: String)

@JsonClass(generateAdapter = true)
data class GameWithDaveUpdateResponse(val updated: Boolean, val message: String)
