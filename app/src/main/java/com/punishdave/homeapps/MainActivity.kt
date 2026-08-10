package com.punishdave.homeapps

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel

private sealed class Route(val id: String) {
    data object Home : Route("home")

    data object MealPlannerMenu : Route("meal_planner_menu")
    data object MealPlannerCurrent : Route("meal_planner_current")
    data object MealPlannerPlan : Route("meal_planner_plan")
    data object MealPlannerShoppingList : Route("meal_planner_shopping_list")

    data object HaveWeGot : Route("have_we_got")
    data object Todo : Route("todo")
    data object WorkoutLog : Route("workout_log")
    data object GameWithDave : Route("gamewithdave")
    data object Transmission : Route("transmission")
    data object Wallfacer : Route("wallfacer")
    data object Sophon : Route("sophon")
    data object Droplet : Route("droplet")
    data object Settings : Route("settings")
}

private const val TRANSMISSION_URL = "http://192.168.0.234:9091/"
private const val WALLFACER_URL = "http://192.168.0.116:8080"
private const val SOPHON_URL = "http://192.168.0.234:8096"
private const val DROPLET_URL = "http://192.168.0.234:8095"

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createHomeNotificationChannel(this)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFE66A64),
                    onPrimary = Color(0xFF1C1C1C),
                    secondary = Color(0xFFE66A64),
                    background = Color(0xFF1C1C1C),
                    surface = Color(0xFF242424),
                    surfaceVariant = Color(0xFF2B2B2B),
                    outline = Color(0xFF4A4A4A)
                )
            ) {
                val navController = rememberNavController()
                val rootSettingsVm: AppSettingsViewModel = viewModel()
                val protectSettings by rootSettingsVm.biometricEnabled.collectAsState()

                NavHost(
                    navController = navController,
                    startDestination = intent.getStringExtra("homeapps_route")
                        ?.takeIf { requested -> requested in setOf(Route.GameWithDave.id, Route.Sophon.id) }
                        ?: Route.Home.id
                ) {
                    composable(Route.Home.id) {
                        HomeAppsScreen(
                            onOpenMealPlanner = { navController.navigate(Route.MealPlannerMenu.id) },
                            onOpenHaveWeGot = { navController.navigate(Route.HaveWeGot.id) },
                            onOpenTodo = { navController.navigate(Route.Todo.id) },
                            onOpenWorkoutLog = { navController.navigate(Route.WorkoutLog.id) },
                            onOpenGameWithDave = { navController.navigate(Route.GameWithDave.id) },
                            onOpenTransmission = { navController.navigate(Route.Transmission.id) },
                            onOpenWallfacer = { navController.navigate(Route.Wallfacer.id) },
                            onOpenSophon = { navController.navigate(Route.Sophon.id) },
                            onOpenDroplet = { navController.navigate(Route.Droplet.id) },
                            onOpenSettings = {
                                openProtectedSettings(protectSettings) { navController.navigate(Route.Settings.id) }
                            }
                        )
                    }

                    composable(Route.MealPlannerMenu.id) {
                        MealPlannerMenuScreen(
                            onBack = { navController.popBackStack() },
                            onOpenCurrentWeek = { navController.navigate(Route.MealPlannerCurrent.id) },
                            onOpenPlanWeek = { navController.navigate(Route.MealPlannerPlan.id) },
                            onOpenShoppingList = { navController.navigate(Route.MealPlannerShoppingList.id) },
                            onSync = { /* handled inside screen */ }
                        )
                    }

                    composable(Route.MealPlannerCurrent.id) {
                        MealPlannerCurrentWeekScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Route.MealPlannerPlan.id) {
                        MealPlannerPlanWeekScreen(
                            onBack = { navController.popBackStack() },
                            onGenerate = {},
                            onSave = {}
                        )
                    }

                    composable(Route.MealPlannerShoppingList.id) {
                        MealPlannerShoppingListScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Route.HaveWeGot.id) {
                        HaveWeGotScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Route.Todo.id) {
                        TodoScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Route.WorkoutLog.id) {
                        WorkoutScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Route.GameWithDave.id) {
                        GameWithDaveScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Route.Transmission.id) {
                        val settingsVm: AppSettingsViewModel = viewModel()
                        val username by settingsVm.transmissionUsername.collectAsState()
                        val password by settingsVm.transmissionPassword.collectAsState()
                        WebAppScreen("Transmission", TRANSMISSION_URL, username, password) { navController.popBackStack() }
                    }
                    composable(Route.Wallfacer.id) {
                        WebAppScreen("Wallfacer", WALLFACER_URL) { navController.popBackStack() }
                    }
                    composable(Route.Sophon.id) {
                        val settingsVm: AppSettingsViewModel = viewModel()
                        val sophonUrl by settingsVm.sophonUrl.collectAsState()
                        if (sophonUrl.isBlank()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFE66A64))
                            }
                        } else {
                            WebAppScreen("Sophon", sophonUrl) { navController.popBackStack() }
                        }
                    }
                    composable(Route.Droplet.id) {
                        WebAppScreen("Droplet", DROPLET_URL) { navController.popBackStack() }
                    }
                    composable(Route.Settings.id) {
                        AppSettingsScreen { navController.popBackStack() }
                    }
                }
            }
        }
    }

    private fun openProtectedSettings(enabled: Boolean, onAuthenticated: () -> Unit) {
        if (!enabled) {
            onAuthenticated()
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onAuthenticated()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Open HomeApps Settings")
            .setSubtitle("Confirm your identity to view saved connections")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF2A2A2A)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/* -----------------------
   Home screen
------------------------ */
