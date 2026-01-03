package com.punishdave.homeapps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

private sealed class Route(val id: String) {
    data object Home : Route("home")

    data object MealPlannerMenu : Route("meal_planner_menu")
    data object MealPlannerCurrent : Route("meal_planner_current")
    data object MealPlannerPlan : Route("meal_planner_plan")
    data object MealPlannerShoppingList : Route("meal_planner_shopping_list")

    data object HaveWeGot : Route("have_we_got")
    data object Todo : Route("todo")
    data object WorkoutLog : Route("workout_log")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Route.Home.id
                ) {
                    composable(Route.Home.id) {
                        HomeAppsScreen(
                            onOpenMealPlanner = { navController.navigate(Route.MealPlannerMenu.id) },
                            onOpenHaveWeGot = { navController.navigate(Route.HaveWeGot.id) },
                            onOpenTodo = { navController.navigate(Route.Todo.id) },
                            onOpenWorkoutLog = { navController.navigate(Route.WorkoutLog.id) }
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
                }
            }
        }
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

@Composable
fun HomeAppsScreen(
    onOpenMealPlanner: () -> Unit,
    onOpenHaveWeGot: () -> Unit,
    onOpenTodo: () -> Unit,
    onOpenWorkoutLog: () -> Unit
) {
    val mealVm: MealPlannerViewModel = viewModel()
    val haveVm: HaveWeGotViewModel = viewModel()
    val todoVm: TodoViewModel = viewModel()

    val bg = Color(0xFF2A2A2A)
    val accent = Color(0xFFB00020)
    var syncingAll by rememberSaveable { mutableStateOf(false) }
    var syncAllMsg by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val cards = listOf(
        HomeCard(
            title = "Meal Planner",
            icon = { Icon(Icons.Filled.Restaurant, contentDescription = null, modifier = Modifier.size(44.dp)) },
            onClick = onOpenMealPlanner
        ),
        HomeCard(
            title = "Have We Got",
            icon = { Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(44.dp)) },
            onClick = onOpenHaveWeGot
        ),
        HomeCard(
            title = "To-Do",
            icon = { Icon(Icons.Filled.Checklist, contentDescription = null, modifier = Modifier.size(44.dp)) },
            onClick = onOpenTodo
        ),
        HomeCard(
            title = "Workout Log",
            icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.size(44.dp)) },
            onClick = onOpenWorkoutLog
        )
    )

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()   // <-- adds safe top inset
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(
                text = "Home Apps",
                color = accent,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(28.dp))

            FilledTonalButton(
                onClick = {
                    if (syncingAll) return@FilledTonalButton
                    syncingAll = true
                    syncAllMsg = "Syncing all apps..."
                    scope.launch {
                        try {
                            val jobs = mutableListOf<Job>()
                            jobs += mealVm.sync()
                            jobs += haveVm.refresh()
                            if (todoVm.accessKey.value.isNotBlank()) {
                                jobs += todoVm.syncFromApi()
                            }
                            jobs.joinAll()
                            syncAllMsg = "Sync complete."
                        } catch (_: Exception) {
                            syncAllMsg = "Sync started; check individual apps for status."
                        } finally {
                            delay(1200)
                            syncingAll = false
                        }
                    }
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accent.copy(alpha = 0.6f),
                    contentColor = Color.White
                ),
                enabled = !syncingAll
            ) {
                Icon(Icons.Filled.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (syncingAll) "Syncing..." else "Sync All")
            }

            syncAllMsg?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (syncingAll) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp),
                            color = accent,
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        text = msg,
                        color = Color(0xFFBDBDBD),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HomeAppsGrid(cards = cards, accent = accent)
        }
    }
}

@Composable
private fun HomeAppsGrid(cards: List<HomeCard>, accent: Color) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(cards) { card ->
            AppCard(
                title = card.title,
                accent = accent,
                icon = card.icon,
                onClick = card.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
            )
        }
    }
}

@Composable
private fun AppCard(
    title: String,
    accent: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF0F0F0F)
    val shape = RoundedCornerShape(14.dp)

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = shape,
        color = cardBg,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(LocalContentColor provides accent) {
                icon()
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private data class HomeCard(
    val title: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)
