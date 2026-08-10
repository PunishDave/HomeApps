package com.punishdave.homeapps

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun HomeAppsScreen(
    onOpenMealPlanner: () -> Unit,
    onOpenHaveWeGot: () -> Unit,
    onOpenTodo: () -> Unit,
    onOpenWorkoutLog: () -> Unit,
    onOpenGameWithDave: () -> Unit,
    onOpenTransmission: () -> Unit,
    onOpenWallfacer: () -> Unit,
    onOpenSophon: () -> Unit,
    onOpenDroplet: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val mealVm: MealPlannerViewModel = viewModel()
    val haveVm: HaveWeGotViewModel = viewModel()
    val todoVm: TodoViewModel = viewModel()
    val workoutVm: WorkoutViewModel = viewModel()
    val gameWithDaveVm: GameWithDaveViewModel = viewModel()
    val usageVm: HomeUsageViewModel = viewModel()
    val sophonVm: SophonSummaryViewModel = viewModel()
    val settingsVm: AppSettingsViewModel = viewModel()
    val reliabilityVm: HomeReliabilityViewModel = viewModel()

    val tasks by todoVm.tasks.collectAsState()
    val currentWeek by mealVm.currentWeek.collectAsState()
    val workoutEntries by workoutVm.entries.collectAsState()
    val gameWithDaveDashboard by gameWithDaveVm.dashboard.collectAsState()
    val usageCounts by usageVm.counts.collectAsState()
    val sophonSummary by sophonVm.summary.collectAsState()
    val sophonUrl by settingsVm.sophonUrl.collectAsState()
    val notificationsEnabled by settingsVm.notificationsEnabled.collectAsState()
    val gameNotificationsEnabled by settingsVm.gameNotificationsEnabled.collectAsState()
    val temperatureNotificationsEnabled by settingsVm.temperatureNotificationsEnabled.collectAsState()
    val lowTemperature by settingsVm.lowTemperature.collectAsState()
    val highTemperature by settingsVm.highTemperature.collectAsState()
    val refreshSettings by settingsVm.refreshSettings.collectAsState()
    val lastUpdated by reliabilityVm.lastUpdated.collectAsState()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val bg = Color(0xFF1C1C1C)
    val accent = Color(0xFFE66A64)
    var syncingAll by rememberSaveable { mutableStateOf(false) }
    var syncAllMsg by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshedServices by remember { mutableStateOf<Set<String>>(emptySet()) }
    var failedServices by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val performSyncAll: (Boolean) -> Unit = { automatic ->
        if (!syncingAll) {
            syncingAll = true
            syncAllMsg = "Refreshing home apps..."
            scope.launch {
                try {
                    val jobs = linkedMapOf(
                        "meal_planner" to mealVm.sync(),
                        "have_we_got" to haveVm.refresh(),
                        "todo" to todoVm.syncFromApi(),
                        "workout" to workoutVm.syncFromApi(),
                        "gamewithdave" to gameWithDaveVm.refresh(),
                        "sophon" to sophonVm.refresh(sophonUrl)
                    )
                    val timedOut = jobs.map { (id, job) ->
                        async {
                            val finished = withTimeoutOrNull(20_000) { job.join(); true } ?: false
                            if (!finished) job.cancel()
                            id to !finished
                        }
                    }.awaitAll().filter { it.second }.map { it.first }.toSet()
                    val failures = buildSet {
                        addAll(timedOut)
                        if (mealVm.lastError.value != null) add("meal_planner")
                        if (haveVm.error.value != null) add("have_we_got")
                        if (todoVm.lastError.value != null) add("todo")
                        if (workoutVm.lastError.value != null) add("workout")
                        if (gameWithDaveVm.message.value != null) add("gamewithdave")
                        if (sophonVm.error.value != null) add("sophon")
                    }
                    failedServices = failures
                    refreshedServices = setOf("meal_planner", "have_we_got", "todo", "workout", "gamewithdave", "sophon")
                    val successful = refreshedServices - failures
                    reliabilityVm.markUpdated(successful)
                    if (automatic) settingsVm.recordAutomaticRefresh(successful, failures)
                    syncAllMsg = if (failures.isEmpty()) "Up to date" else "${failures.size} service${if (failures.size == 1) "" else "s"} need attention"
                    if (notificationsEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        if (gameNotificationsEnabled) gameWithDaveVm.dashboard.value.days.firstOrNull { day -> day.game_nights.any { it.status != "removed" } }?.let { day ->
                            val fingerprint = day.date + ":" + day.game_nights.filter { it.status != "removed" }.joinToString { "${it.team}:${it.status}" }
                            if (reliabilityVm.claimNotification("game", fingerprint)) notifyGameNight(context, "Next game night: ${day.display_date}")
                        }
                        if (temperatureNotificationsEnabled) {
                            val low = lowTemperature.toDoubleOrNull() ?: 8.0
                            val high = highTemperature.toDoubleOrNull() ?: 25.0
                            sophonVm.summary.value?.devices.orEmpty().forEachIndexed { index, device ->
                                val state = temperatureState(device, low, high)
                                if (reliabilityVm.updateTemperatureZone(state.deviceId, state.zone) && state.message != null) {
                                    notifyHome(context, 4200 + index, "Sophon temperature", state.message, "sophon")
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    syncAllMsg = "Refresh finished with an error"
                } finally {
                    syncingAll = false
                }
            }
        }
    }
    val syncAll: () -> Unit = { performSyncAll(false) }
    val latestSyncAll by rememberUpdatedState(performSyncAll)
    LaunchedEffect(refreshSettings.enabled, refreshSettings.intervalMinutes) {
        if (!refreshSettings.enabled) return@LaunchedEffect
        while (true) {
            delay(normalizeRefreshInterval(refreshSettings.intervalMinutes) * 60_000L)
            latestSyncAll(true)
        }
    }
    val pullRefreshState = rememberPullRefreshState(syncingAll, syncAll)

    fun tracked(id: String, open: () -> Unit): () -> Unit = {
        usageVm.recordOpen(id)
        open()
    }

    fun serviceStatus(id: String): Boolean? = when {
        id in failedServices -> false
        id in refreshedServices -> true
        else -> null
    }

    val cards = listOf(
        HomeCard(
            id = "meal_planner",
            healthy = serviceStatus("meal_planner"),
            refreshing = syncingAll,
            updatedAt = lastUpdated["meal_planner"],
            title = "Meal Planner",
            onRetry = syncAll,
            icon = { Icon(Icons.Filled.Restaurant, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("meal_planner", onOpenMealPlanner)
        ),
        HomeCard(
            id = "have_we_got",
            healthy = serviceStatus("have_we_got"),
            refreshing = syncingAll,
            updatedAt = lastUpdated["have_we_got"],
            title = "Have We Got",
            onRetry = syncAll,
            icon = { Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("have_we_got", onOpenHaveWeGot)
        ),
        HomeCard(
            id = "todo",
            healthy = serviceStatus("todo"),
            refreshing = syncingAll,
            updatedAt = lastUpdated["todo"],
            title = "To-Do",
            onRetry = syncAll,
            icon = { Icon(Icons.Filled.Checklist, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("todo", onOpenTodo)
        ),
        HomeCard(
            id = "workout",
            healthy = serviceStatus("workout"),
            refreshing = syncingAll,
            updatedAt = lastUpdated["workout"],
            title = "Workout Log",
            onRetry = syncAll,
            icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("workout", onOpenWorkoutLog)
        ),
        HomeCard(
            id = "gamewithdave",
            healthy = serviceStatus("gamewithdave"),
            refreshing = syncingAll,
            updatedAt = lastUpdated["gamewithdave"],
            title = "GameWithDave",
            onRetry = syncAll,
            icon = { Icon(Icons.Filled.EventAvailable, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("gamewithdave", onOpenGameWithDave)
        ),
        HomeCard(
            id = "transmission",
            title = "Transmission",
            icon = { Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("transmission", onOpenTransmission)
        ),
        HomeCard(
            id = "wallfacer",
            title = "Wallfacer",
            icon = { Icon(Icons.Filled.Wallpaper, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("wallfacer", onOpenWallfacer)
        ),
        HomeCard(
            id = "sophon",
            healthy = serviceStatus("sophon"),
            refreshing = syncingAll,
            updatedAt = lastUpdated["sophon"],
            title = "Sophon",
            onRetry = syncAll,
            icon = { Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("sophon", onOpenSophon)
        ),
        HomeCard(
            id = "droplet",
            title = "Droplet",
            icon = { Icon(Icons.Filled.CloudQueue, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("droplet", onOpenDroplet)
        )
    )
    val rankedIds = rankHomeSectionIds(cards.map { it.id }, usageCounts)
    val rankedCards = rankedIds.mapNotNull { id -> cards.firstOrNull { it.id == id } }

    val today = LocalDate.now()
    val todayTasks = tasks.count { !it.done && parseFlexibleDate(it.dueDate) == today }
    val tonightMeal = currentWeek?.let { week ->
        val offset = runCatching { ChronoUnit.DAYS.between(LocalDate.parse(week.week_start), today).toInt() }.getOrNull()
        offset?.takeIf { it in week.meals.indices }?.let { week.meals[it].title }
    }
    val workoutDone = workoutEntries.any { it.date == today.toString() }
    val nextGameNight = gameWithDaveDashboard.days
        .asSequence()
        .flatMap { day -> day.game_nights.asSequence().filter { it.status != "removed" }.map { day to it } }
        .firstOrNull()

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text("Home", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = syncAllMsg ?: "Pull down to refresh",
                    color = Color(0xFF8D8D8D),
                    fontSize = 12.sp
                )
                if (syncingAll) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = accent,
                        trackColor = Color(0xFF2B2B2B)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                TodayOverview(
                    taskCount = todayTasks,
                    meal = tonightMeal,
                    workoutDone = workoutDone,
                    gameNight = nextGameNight?.let { (day, night) -> "${day.display_date}: ${night.team_label}" },
                    sophonDevices = sophonSummary?.devices.orEmpty(),
                    onTodo = tracked("todo", onOpenTodo),
                    onMeal = tracked("meal_planner", onOpenMealPlanner),
                    onWorkout = tracked("workout", onOpenWorkoutLog),
                    onGameWithDave = tracked("gamewithdave", onOpenGameWithDave),
                    onSophon = tracked("sophon", onOpenSophon)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Apps", color = Color(0xFFE66A64), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                HomeAppsGrid(cards = rankedCards, accent = accent, modifier = Modifier.weight(1f))
                Spacer(Modifier.height(12.dp))
                HomeAppCard(
                    title = "Settings",
                    accent = accent,
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                )
            }
            PullRefreshIndicator(
                refreshing = syncingAll,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
                backgroundColor = Color(0xFF161616),
                contentColor = accent
            )
        }
    }
}

@Composable
private fun HomeAppsGrid(cards: List<HomeCard>, accent: Color, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(cards) { card ->
            HomeAppCard(
                title = card.title,
                accent = accent,
                healthy = card.healthy,
                refreshing = card.refreshing,
                updatedAt = card.updatedAt,
                icon = card.icon,
                onClick = card.onClick,
                onRetry = card.onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            )
        }
    }
}

@Composable
fun HomeAppCard(
    title: String,
    accent: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    healthy: Boolean? = null,
    updatedAt: Long? = null,
    refreshing: Boolean = false,
    onRetry: (() -> Unit)? = null
) {
    val cardBg = Color(0xFF222222)
    val shape = RoundedCornerShape(10.dp)

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = shape,
        color = cardBg,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalContentColor provides accent) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { icon() }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    val displayState = serviceDisplayState(refreshing, healthy, updatedAt != null)
                    if (displayState != ServiceDisplayState.NotChecked) {
                        Text(
                            text = when (displayState) {
                                ServiceDisplayState.Refreshing -> "Refreshing..."
                                ServiceDisplayState.Live -> "Live ${updatedAt?.let(::formatRefreshTime).orEmpty()}"
                                ServiceDisplayState.Cached -> "Cached ${updatedAt?.let(::formatRefreshTime).orEmpty()}"
                                ServiceDisplayState.Unavailable -> "Unavailable"
                                ServiceDisplayState.NotChecked -> ""
                            }.trim(),
                            color = Color(0xFF777777),
                            fontSize = 10.sp
                        )
                    }
                }
                if (healthy == false && onRetry != null) {
                    IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Retry $title", tint = accent, modifier = Modifier.size(17.dp))
                    }
                } else if (healthy != null) {
                    Surface(
                        modifier = Modifier.size(7.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (healthy) Color(0xFF7DB68A) else Color(0xFFE66A64)
                    ) {}
                }
            }
        }
    }
}

private data class HomeCard(
    val id: String,
    val healthy: Boolean? = null,
    val refreshing: Boolean = false,
    val updatedAt: Long? = null,
    val title: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val onRetry: (() -> Unit)? = null
)

private fun formatRefreshTime(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMillis))
