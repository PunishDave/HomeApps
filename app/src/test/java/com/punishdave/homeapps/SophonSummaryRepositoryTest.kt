package com.punishdave.homeapps

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SophonSummaryRepositoryTest {
    @Test
    fun normalizesBaseUrlAndReturnsFakeData() = runBlocking {
        var requestedUrl = ""
        val fake = SophonSummarySource { url ->
            requestedUrl = url
            SophonSummaryResponse(devices = listOf(SophonDevice("office-temp-reader", 20.5)))
        }

        val result = SophonSummaryRepository(fake).fetch(" https://sophon.example/ ")

        assertEquals("https://sophon.example/api/summary", requestedUrl)
        assertEquals(20.5, result.devices.single().temperature_c ?: 0.0, 0.0)
    }

    @Test
    fun rejectsMissingUrlWithoutCallingSource() = runBlocking {
        var called = false
        val repository = SophonSummaryRepository { called = true; SophonSummaryResponse() }

        val error = runCatching { repository.fetch("  ") }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(!called)
    }
}
