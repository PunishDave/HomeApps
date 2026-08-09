package com.punishdave.homeapps

import android.os.Bundle
import android.Manifest
import android.os.Build
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
                    startDestination = Route.Home.id
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

    val tasks by todoVm.tasks.collectAsState()
    val currentWeek by mealVm.currentWeek.collectAsState()
    val workoutEntries by workoutVm.entries.collectAsState()
    val gameWithDaveDashboard by gameWithDaveVm.dashboard.collectAsState()
    val usageCounts by usageVm.counts.collectAsState()
    val sophonSummary by sophonVm.summary.collectAsState()
    val sophonUrl by settingsVm.sophonUrl.collectAsState()
    val notificationsEnabled by settingsVm.notificationsEnabled.collectAsState()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val bg = Color(0xFF1C1C1C)
    val accent = Color(0xFFE66A64)
    var syncingAll by rememberSaveable { mutableStateOf(false) }
    var syncAllMsg by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshedServices by remember { mutableStateOf<Set<String>>(emptySet()) }
    var failedServices by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val syncAll: () -> Unit = {
        if (!syncingAll) {
            syncingAll = true
            syncAllMsg = "Refreshing home apps..."
            scope.launch {
                try {
                    val jobs = mutableListOf<Job>()
                    jobs += mealVm.sync()
                    jobs += haveVm.refresh()
                    jobs += todoVm.syncFromApi()
                    jobs += workoutVm.syncFromApi()
                    jobs += gameWithDaveVm.refresh()
                    jobs += sophonVm.refresh(sophonUrl)
                    jobs.joinAll()
                    val failures = buildSet {
                        if (mealVm.lastError.value != null) add("meal_planner")
                        if (haveVm.error.value != null) add("have_we_got")
                        if (todoVm.lastError.value != null) add("todo")
                        if (workoutVm.lastError.value != null) add("workout")
                        if (gameWithDaveVm.message.value != null) add("gamewithdave")
                        if (sophonVm.error.value != null) add("sophon")
                    }
                    failedServices = failures
                    refreshedServices = setOf("meal_planner", "have_we_got", "todo", "workout", "gamewithdave", "sophon")
                    syncAllMsg = if (failures.isEmpty()) "Up to date" else "${failures.size} service${if (failures.size == 1) "" else "s"} need attention"
                    if (notificationsEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        gameWithDaveVm.dashboard.value.days.firstOrNull { day -> day.game_nights.any { it.status != "removed" } }?.let { day ->
                            notifyGameNight(context, "Next game night: ${day.display_date}")
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
            title = "Meal Planner",
            icon = { Icon(Icons.Filled.Restaurant, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("meal_planner", onOpenMealPlanner)
        ),
        HomeCard(
            id = "have_we_got",
            healthy = serviceStatus("have_we_got"),
            title = "Have We Got",
            icon = { Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("have_we_got", onOpenHaveWeGot)
        ),
        HomeCard(
            id = "todo",
            healthy = serviceStatus("todo"),
            title = "To-Do",
            icon = { Icon(Icons.Filled.Checklist, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("todo", onOpenTodo)
        ),
        HomeCard(
            id = "workout",
            healthy = serviceStatus("workout"),
            title = "Workout Log",
            icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = tracked("workout", onOpenWorkoutLog)
        ),
        HomeCard(
            id = "gamewithdave",
            healthy = serviceStatus("gamewithdave"),
            title = "GameWithDave",
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
            title = "Sophon",
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
                AppCard(
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
private fun TodayOverview(
    taskCount: Int,
    meal: String?,
    workoutDone: Boolean,
    gameNight: String?,
    sophonDevices: List<SophonDevice>,
    onTodo: () -> Unit,
    onMeal: () -> Unit,
    onWorkout: () -> Unit,
    onGameWithDave: () -> Unit,
    onSophon: () -> Unit
) {
    Surface(color = Color(0xFF222222), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("TODAY", color = Color(0xFFE66A64), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            TodayRow("Tasks", if (taskCount == 0) "Nothing due" else "$taskCount due", onTodo)
            TodayRow("Meal", meal ?: "Not planned", onMeal)
            TodayRow("Workout", if (workoutDone) "Logged today" else "Not logged", onWorkout)
            gameNight?.let { TodayRow("Game night", it, onGameWithDave) }
            if (sophonDevices.isNotEmpty()) SophonTodayRow(sophonDevices, onSophon)
        }
    }
}

@Composable
private fun SophonTodayRow(devices: List<SophonDevice>, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Sophon", color = Color(0xFF999999), fontSize = 12.sp, modifier = Modifier.width(82.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            devices.forEach { device ->
                val name = when (device.device_id) {
                    "garden-sensor-01" -> "Garden"
                    "office-temp-reader" -> "Office"
                    else -> device.device_id
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, color = Color.White, fontSize = 13.sp)
                    Text(device.temperature_c?.let { "%.1f°C".format(it) } ?: "--", color = Color(0xFFE66A64), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TodayRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF999999), fontSize = 12.sp, modifier = Modifier.width(82.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun WebAppScreen(
    title: String,
    url: String,
    username: String = "",
    password: String = "",
    onBack: () -> Unit
) {
    var webView by androidx.compose.runtime.remember { mutableStateOf<WebView?>(null) }
    var loading by androidx.compose.runtime.remember { mutableStateOf(true) }
    var loadedRootUrl by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
    val currentUsername by androidx.compose.runtime.rememberUpdatedState(username)
    val currentPassword by androidx.compose.runtime.rememberUpdatedState(password)

    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onBack()
    }

    Scaffold(
        topBar = {
            Surface(color = Color(0xFF0F0F0F), shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = url,
                            color = Color(0xFF8D8D8D),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val refreshLayout = SwipeRefreshLayout(context)
                    val browser = WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                (view?.parent as? SwipeRefreshLayout)?.isRefreshing = false
                            }

                            override fun onReceivedHttpAuthRequest(
                                view: WebView?,
                                handler: HttpAuthHandler?,
                                host: String?,
                                realm: String?
                            ) {
                                if (currentUsername.isNotBlank() || currentPassword.isNotBlank()) {
                                    handler?.proceed(currentUsername, currentPassword)
                                } else {
                                    handler?.cancel()
                                }
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(url)
                        loadedRootUrl = url
                        webView = this
                    }
                    refreshLayout.setColorSchemeColors(0xFFE66A64.toInt())
                    refreshLayout.setProgressBackgroundColorSchemeColor(0xFF222222.toInt())
                    refreshLayout.setOnChildScrollUpCallback { _, _ -> browser.canScrollVertically(-1) }
                    refreshLayout.setOnRefreshListener { browser.reload() }
                    refreshLayout.addView(
                        browser,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    refreshLayout
                },
                update = { refreshLayout ->
                    val browser = refreshLayout.getChildAt(0) as? WebView
                    webView = browser
                    if (browser != null && loadedRootUrl != url) {
                        loadedRootUrl = url
                        browser.loadUrl(url)
                    }
                }
            )
            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = Color(0xFFE66A64),
                    trackColor = Color.Transparent
                )
            }
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
            AppCard(
                title = card.title,
                accent = accent,
                healthy = card.healthy,
                icon = card.icon,
                onClick = card.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
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
    modifier: Modifier = Modifier,
    healthy: Boolean? = null
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
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                healthy?.let {
                    Surface(
                        modifier = Modifier.size(7.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (it) Color(0xFF7DB68A) else Color(0xFFE66A64)
                    ) {}
                }
            }
        }
    }
}

private data class HomeCard(
    val id: String,
    val healthy: Boolean? = null,
    val title: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)
