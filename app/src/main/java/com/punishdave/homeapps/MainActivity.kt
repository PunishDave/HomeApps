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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.statusBarsPadding

private sealed class Route(val id: String) {
    data object Home : Route("home")

    data object MealPlannerMenu : Route("meal_planner_menu")
    data object MealPlannerCurrent : Route("meal_planner_current")
    data object MealPlannerPlan : Route("meal_planner_plan")

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

                    composable(Route.HaveWeGot.id) { PlaceholderScreen("Have We Got") }
                    composable(Route.Todo.id) { PlaceholderScreen("To-Do") }
                    composable(Route.WorkoutLog.id) { PlaceholderScreen("Workout Log") }
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
    val bg = Color(0xFF2A2A2A)
    val accent = Color(0xFFB00020)

    val cards = listOf(
        HomeCard(
            title = "Meal Planner",
            desc = "Plan the week's meals and your shopping list.",
            icon = { Icon(Icons.Filled.Restaurant, contentDescription = null) },
            onClick = onOpenMealPlanner
        ),
        HomeCard(
            title = "Have We Got",
            desc = "Check what's available at a glance.",
            icon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
            onClick = onOpenHaveWeGot
        ),
        HomeCard(
            title = "To-Do",
            desc = "Track and manage your tasks with categories, habits, and recurrence.",
            icon = { Icon(Icons.Filled.Checklist, contentDescription = null) },
            onClick = onOpenTodo
        ),
        HomeCard(
            title = "Workout Log",
            desc = "Simple 5-day log to recall your last weight and reps by workout.",
            icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
            onClick = onOpenWorkoutLog
        )
    )

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()   // <-- adds safe top inset
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(
                text = "Home Apps",
                color = accent,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Handy home tools in one place. Pick an app to jump straight in.",
                color = Color(0xFFBDBDBD),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

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
                desc = card.desc,
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
    desc: String,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides accent) {
                icon()
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = Color(0xFFB0B0B0),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class HomeCard(
    val title: String,
    val desc: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)
