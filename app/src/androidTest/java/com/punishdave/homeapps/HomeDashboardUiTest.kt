package com.punishdave.homeapps

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class HomeDashboardUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun todayOverviewShowsTasksAndFriendlySensors() {
        compose.setContent {
            MaterialTheme {
                TodayOverview(
                    taskCount = 2,
                    meal = "Curry",
                    workoutDone = true,
                    gameNight = null,
                    sophonDevices = listOf(
                        SophonDevice("garden-sensor-01", 14.2),
                        SophonDevice("office-temp-reader", 21.7)
                    )
                )
            }
        }

        compose.onNodeWithText("2 due").assertIsDisplayed()
        compose.onNodeWithText("Curry").assertIsDisplayed()
        compose.onNodeWithText("Garden").assertIsDisplayed()
        compose.onNodeWithText("14.2°C").assertIsDisplayed()
        compose.onNodeWithText("Office").assertIsDisplayed()
    }

    @Test
    fun calendarKeepsNavigatedMonthWhenDashboardDataChanges() {
        var replaceDays: (() -> Unit)? = null
        compose.setContent {
            var selectedMonth by remember { mutableStateOf("2026-08") }
            var days by remember { mutableStateOf(emptyList<GameWithDaveDay>()) }
            replaceDays = { days = listOf(GameWithDaveDay("2026-09-01", "Tuesday 1 September 2026")) }
            MaterialTheme {
                CompactCalendar(days, selectedMonth) { selectedMonth = it }
            }
        }

        compose.onNodeWithContentDescription("Next month").performClick()
        compose.onNodeWithText("September").assertIsDisplayed()
        compose.runOnIdle { replaceDays?.invoke() }
        compose.onNodeWithText("September").assertIsDisplayed()
    }
}
