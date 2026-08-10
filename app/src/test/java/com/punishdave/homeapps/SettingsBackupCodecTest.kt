package com.punishdave.homeapps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SettingsBackupCodecTest {
    private val settings = SettingsBackup(
        mealKey = "meal-secret",
        todoKey = "todo-secret",
        gameWithDaveUsername = "dave",
        gameWithDavePassword = "password",
        sophonUrl = "https://raspberrypi.example.ts.net/",
        notificationsEnabled = true,
        automaticRefreshEnabled = true,
        backgroundRefreshEnabled = true,
        refreshIntervalMinutes = 60,
        unmeteredOnly = true
    )

    @Test
    fun roundTripPreservesAllSettingsWithoutExposingSecrets() {
        val encoded = SettingsBackupCodec.encode(settings, "correct horse battery staple")

        assertNotEquals(true, encoded.contains("meal-secret"))
        assertNotEquals(true, encoded.contains("password"))
        assertEquals(settings, SettingsBackupCodec.decode(encoded, "correct horse battery staple"))
    }

    @Test
    fun wrongPasswordCannotRestoreSettings() {
        val encoded = SettingsBackupCodec.encode(settings, "correct horse battery staple")

        assertThrows(Exception::class.java) {
            SettingsBackupCodec.decode(encoded, "incorrect password")
        }
    }

    @Test
    fun shortPasswordsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SettingsBackupCodec.encode(settings, "short")
        }
    }
}
