package com.punishdave.homeapps

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionStateTest {
    @Test
    fun cachedDataRemainsVisibleWhenRefreshFails() {
        assertEquals(ServiceDisplayState.Cached, serviceDisplayState(false, false, true))
        assertEquals(ServiceDisplayState.Unavailable, serviceDisplayState(false, false, false))
    }

    @Test
    fun refreshingAndLiveTakePriorityOverOlderCache() {
        assertEquals(ServiceDisplayState.Refreshing, serviceDisplayState(true, false, true))
        assertEquals(ServiceDisplayState.Live, serviceDisplayState(false, true, true))
    }

    @Test
    fun sophonConnectionIdentifiesHomeAndTailscaleRoutes() {
        assertEquals("Home network", sophonConnectionLabel("http://192.168.0.234:8096"))
        assertEquals("Tailscale", sophonConnectionLabel("https://raspberrypi.tail276746.ts.net/"))
        assertEquals("Remote connection", sophonConnectionLabel("https://sophon.example.com"))
    }
}
