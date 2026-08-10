package com.punishdave.homeapps

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GameWithDaveRepositoryTest {
    @Test
    fun appliesAccessKeyToHeadersAndQuery() = runBlocking {
        val fake = FakeGameWithDaveApi()
        val repository = GameWithDaveRepository(fake)

        repository.dashboard("secret")

        assertEquals("secret", fake.key)
        assertEquals("Bearer secret", fake.bearer)
        assertEquals("secret", fake.queryKey)
    }

    @Test
    fun forwardsAvailabilityIdentityAndPassword() = runBlocking {
        val fake = FakeGameWithDaveApi()
        val request = GameWithDaveAvailabilityRequest("dave", "2026-08-10", "2026-08-12", "yes", "password")

        val message = GameWithDaveRepository(fake).saveAvailability("key", request)

        assertEquals("saved", message)
        assertEquals(request, fake.availability)
    }
}

private class FakeGameWithDaveApi : GameWithDaveApi {
    var key: String? = null
    var bearer: String? = null
    var queryKey: String? = null
    var availability: GameWithDaveAvailabilityRequest? = null

    override suspend fun dashboard(key: String?, bearer: String?, accessKey: String?, cacheBuster: Long): GameWithDaveDashboard {
        this.key = key
        this.bearer = bearer
        this.queryKey = accessKey
        return GameWithDaveDashboard()
    }

    override suspend fun saveAvailability(key: String?, bearer: String?, accessKey: String?, body: GameWithDaveAvailabilityRequest): GameWithDaveUpdateResponse {
        availability = body
        return GameWithDaveUpdateResponse(true, "saved")
    }

    override suspend fun updateNight(date: String, team: String, key: String?, bearer: String?, accessKey: String?, body: GameWithDaveNightUpdateRequest): GameWithDaveNight =
        GameWithDaveNight(date, team, team, "locked")
}
