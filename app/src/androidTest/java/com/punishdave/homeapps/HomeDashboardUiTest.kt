package com.punishdave.homeapps

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsOn
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class HomeDashboardUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

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

    @Test
    fun cachedCardAndSettingsToggleExposeTheirState() {
        compose.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    HomeAppCard(
                        title = "To-Do",
                        accent = androidx.compose.ui.graphics.Color.Red,
                        icon = { Text("T") },
                        onClick = {},
                        healthy = false,
                        updatedAt = 1_786_300_800_000,
                        modifier = Modifier.fillMaxWidth().height(72.dp)
                    )
                    SettingsToggle("Temperature alerts", "Notify outside range", true) {}
                }
            }
        }

        compose.onNodeWithText("To-Do").assertIsDisplayed()
        compose.onNodeWithText("Cached", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Temperature alerts").assertIsDisplayed()
        compose.onNode(androidx.compose.ui.test.SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.ToggleableState, androidx.compose.ui.state.ToggleableState.On)).assertIsOn()
    }

    @Test
    fun gameStatusDropdownOffersAllChoices() {
        compose.setContent { MaterialTheme { StatusDropdown("yes") {} } }

        compose.onNodeWithText("Available").performClick()
        compose.onNodeWithText("Tentative").assertIsDisplayed()
        compose.onNodeWithText("Not available").assertIsDisplayed()
    }
}
