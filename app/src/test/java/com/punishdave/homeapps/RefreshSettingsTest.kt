package com.punishdave.homeapps

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshSettingsTest {
    @Test
    fun unsupportedIntervalsAreNormalizedToNearestSafeOption() {
        assertEquals(15, normalizeRefreshInterval(1))
        assertEquals(30, normalizeRefreshInterval(29))
        assertEquals(60, normalizeRefreshInterval(55))
    }

    @Test
    fun intervalLabelsAreConciseAndReadable() {
        assertEquals("Every 15 minutes", refreshIntervalLabel(15))
        assertEquals("Every hour", refreshIntervalLabel(60))
        assertEquals("Every 6 hours", refreshIntervalLabel(360))
    }

    @Test
    fun refreshDefaultsAllowBackgroundWithoutEnablingPolling() {
        assertEquals(false, RefreshSettings().enabled)
        assertEquals(true, RefreshSettings().backgroundEnabled)
        assertEquals(false, RefreshSettings().unmeteredOnly)
    }
}
