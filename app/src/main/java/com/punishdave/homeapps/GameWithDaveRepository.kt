package com.punishdave.homeapps

class GameWithDaveRepository(private val api: GameWithDaveApi = Network.gameWithDaveApi) {
    suspend fun dashboard(key: String): GameWithDaveDashboard =
        api.dashboard(key, "Bearer $key", key)

    suspend fun saveAvailability(key: String, request: GameWithDaveAvailabilityRequest): String =
        api.saveAvailability(key, "Bearer $key", key, request).message

    suspend fun updateNight(key: String, night: GameWithDaveNight, action: String, password: String) {
        api.updateNight(
            night.date,
            night.team,
            key,
            "Bearer $key",
            key,
            GameWithDaveNightUpdateRequest(action, password)
        )
    }
}
