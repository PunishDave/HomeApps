package com.punishdave.homeapps

import android.os.Bundle
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                            onOpenSettings = { navController.navigate(Route.Settings.id) }
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
    val gameWithDaveVm: GameWithDaveViewModel = viewModel()

    val bg = Color(0xFF1C1C1C)
    val accent = Color(0xFFE66A64)
    var syncingAll by rememberSaveable { mutableStateOf(false) }
    var syncAllMsg by rememberSaveable { mutableStateOf<String?>(null) }
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
                    jobs += gameWithDaveVm.refresh()
                    jobs.joinAll()
                    syncAllMsg = "Up to date"
                } catch (_: Exception) {
                    syncAllMsg = "Refresh finished with an error"
                } finally {
                    syncingAll = false
                }
            }
        }
    }
    val pullRefreshState = rememberPullRefreshState(syncingAll, syncAll)

    val cards = listOf(
        HomeCard(
            title = "Meal Planner",
            icon = { Icon(Icons.Filled.Restaurant, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = onOpenMealPlanner
        ),
        HomeCard(
            title = "Have We Got",
            icon = { Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = onOpenHaveWeGot
        ),
        HomeCard(
            title = "To-Do",
            icon = { Icon(Icons.Filled.Checklist, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = onOpenTodo
        ),
        HomeCard(
            title = "Workout Log",
            icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = onOpenWorkoutLog
        ),
        HomeCard(
            title = "GameWithDave",
            icon = { Icon(Icons.Filled.EventAvailable, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = onOpenGameWithDave
        ),
        HomeCard(
            title = "Transmission",
            icon = { Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = onOpenTransmission
        ),
        HomeCard(
            title = "Wallfacer",
            icon = { Icon(Icons.Filled.Wallpaper, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = onOpenWallfacer
        ),
        HomeCard(
            title = "Sophon",
            icon = { Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = onOpenSophon
        ),
        HomeCard(
            title = "Droplet",
            icon = { Icon(Icons.Filled.CloudQueue, contentDescription = null, modifier = Modifier.size(24.dp)) },
            onClick = onOpenDroplet
        )
    )

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
                Spacer(modifier = Modifier.height(18.dp))
                HomeAppsGrid(cards = cards, accent = accent, modifier = Modifier.weight(1f))
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
    modifier: Modifier = Modifier
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
                    maxLines = 1
                )
            }
        }
    }
}

private data class HomeCard(
    val title: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)
