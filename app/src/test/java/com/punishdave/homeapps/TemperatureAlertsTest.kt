package com.punishdave.homeapps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperatureAlertsTest {
    @Test
    fun reportsFriendlyNamesOutsideThresholds() {
        val alerts = temperatureAlerts(
            listOf(
                SophonDevice("garden-sensor-01", 5.0),
                SophonDevice("office-temp-reader", 26.5)
            ),
            low = 8.0,
            high = 25.0
        )

        assertEquals(2, alerts.size)
        assertTrue(alerts[0].message.startsWith("Garden is cold"))
        assertTrue(alerts[1].message.startsWith("Office is warm"))
    }

    @Test
    fun ignoresComfortableAndMissingReadings() {
        val alerts = temperatureAlerts(
            listOf(SophonDevice("office-temp-reader", 20.0), SophonDevice("unknown", null)),
            low = 8.0,
            high = 25.0
        )

        assertTrue(alerts.isEmpty())
    }

    @Test
    fun classifiesRecoveryAsNormal() {
        assertEquals("high", temperatureState(SophonDevice("office-temp-reader", 27.0), 8.0, 25.0).zone)
        assertEquals("normal", temperatureState(SophonDevice("office-temp-reader", 20.0), 8.0, 25.0).zone)
        assertEquals("low", temperatureState(SophonDevice("office-temp-reader", 4.0), 8.0, 25.0).zone)
    }
}
