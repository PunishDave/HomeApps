package com.punishdave.homeapps

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.Request

private val Context.webSettingsDataStore by preferencesDataStore(name = "web_app_settings")

class AppSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val context = app.applicationContext
    private val mealStore = MealPlannerStore(context)
    private val todoStore = ToDoStore(context)
    private val workoutStore = WorkoutStore(context)
    private val gameWithDaveStore = GameWithDaveStore(context)

    private val transmissionUserKey = stringPreferencesKey("transmission_username")
    private val transmissionPasswordKey = stringPreferencesKey("transmission_password")
    private val sophonUrlKey = stringPreferencesKey("sophon_url")
    private val notificationsKey = booleanPreferencesKey("notifications_enabled")
    private val biometricKey = booleanPreferencesKey("biometric_settings_enabled")
    private val gameNotificationsKey = booleanPreferencesKey("game_notifications_enabled")
    private val temperatureNotificationsKey = booleanPreferencesKey("temperature_notifications_enabled")
    private val lowTemperatureKey = stringPreferencesKey("temperature_low_threshold")
    private val highTemperatureKey = stringPreferencesKey("temperature_high_threshold")

    val mealKey = mealStore.accessKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val todoKey = todoStore.accessKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val todoCategory = todoStore.categoryFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val todoHabit = todoStore.habitFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val workoutKey = workoutStore.accessKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val gameWithDaveKey = gameWithDaveStore.accessKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val gameWithDaveUsername = gameWithDaveStore.usernameFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val gameWithDavePassword = gameWithDaveStore.passwordFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val transmissionUsername = context.webSettingsDataStore.data
        .map { CredentialCipher.decrypt(it[transmissionUserKey] ?: "") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val transmissionPassword = context.webSettingsDataStore.data
        .map { CredentialCipher.decrypt(it[transmissionPasswordKey] ?: "") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val sophonUrl = context.webSettingsDataStore.data
        .map { it[sophonUrlKey] ?: "http://192.168.0.234:8096" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val notificationsEnabled = context.webSettingsDataStore.data
        .map { it[notificationsKey] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val biometricEnabled = context.webSettingsDataStore.data
        .map { it[biometricKey] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val gameNotificationsEnabled = context.webSettingsDataStore.data
        .map { it[gameNotificationsKey] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val temperatureNotificationsEnabled = context.webSettingsDataStore.data
        .map { it[temperatureNotificationsKey] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val lowTemperature = context.webSettingsDataStore.data
        .map { it[lowTemperatureKey] ?: "8" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "8")
    val highTemperature = context.webSettingsDataStore.data
        .map { it[highTemperatureKey] ?: "25" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "25")

    var saveStatus by mutableStateOf<String?>(null)
        private set
    var connectionStatus by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var testingConnections by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            mealStore.saveAccessKey(mealStore.accessKeyFlow.first())
            todoStore.saveAccessKey(todoStore.accessKeyFlow.first())
            workoutStore.saveAccessKey(workoutStore.accessKeyFlow.first())
            gameWithDaveStore.saveAccessKey(gameWithDaveStore.accessKeyFlow.first())
            gameWithDaveStore.saveCredentials(gameWithDaveStore.usernameFlow.first(), gameWithDaveStore.passwordFlow.first())
            context.webSettingsDataStore.edit {
                it[transmissionUserKey] = CredentialCipher.encrypt(CredentialCipher.decrypt(it[transmissionUserKey] ?: ""))
                it[transmissionPasswordKey] = CredentialCipher.encrypt(CredentialCipher.decrypt(it[transmissionPasswordKey] ?: ""))
            }
        }
    }

    fun save(
        mealKey: String,
        todoKey: String,
        todoCategory: String,
        todoHabit: String,
        workoutKey: String,
        gameWithDaveKey: String,
        gameWithDaveUsername: String,
        gameWithDavePassword: String,
        sophonUrl: String,
        transmissionUsername: String,
        transmissionPassword: String,
        notificationsEnabled: Boolean,
        biometricEnabled: Boolean,
        gameNotificationsEnabled: Boolean,
        temperatureNotificationsEnabled: Boolean,
        lowTemperature: String,
        highTemperature: String
    ) = viewModelScope.launch {
        mealStore.saveAccessKey(mealKey.trim())
        todoStore.saveAccessKey(todoKey.trim())
        todoStore.saveCategory(todoCategory.trim())
        todoStore.saveHabit(todoHabit.trim())
        workoutStore.saveAccessKey(workoutKey.trim())
        gameWithDaveStore.saveAccessKey(gameWithDaveKey.trim())
        gameWithDaveStore.saveCredentials(gameWithDaveUsername.trim(), gameWithDavePassword)
        context.webSettingsDataStore.edit {
            it[sophonUrlKey] = sophonUrl.trim().ifBlank { "http://192.168.0.234:8096" }
            it[transmissionUserKey] = CredentialCipher.encrypt(transmissionUsername.trim())
            it[transmissionPasswordKey] = CredentialCipher.encrypt(transmissionPassword)
            it[notificationsKey] = notificationsEnabled
            it[biometricKey] = biometricEnabled
            it[gameNotificationsKey] = gameNotificationsEnabled
            it[temperatureNotificationsKey] = temperatureNotificationsEnabled
            it[lowTemperatureKey] = lowTemperature
            it[highTemperatureKey] = highTemperature
        }
        saveStatus = "Settings saved"
    }

    fun testConnections(
        todoKey: String,
        workoutKey: String,
        gameWithDaveKey: String,
        sophonUrl: String,
        transmissionUsername: String,
        transmissionPassword: String
    ) = viewModelScope.launch {
        testingConnections = true
        connectionStatus = linkedMapOf(
            "Meal Planner" to check { Network.api.getRecipes() },
            "To-Do" to check { Network.todoApi.listItems(todoKey.trim()) },
            "Workout" to check { Network.workoutApi.listDays(workoutKey.trim()) },
            "GameWithDave" to check {
                val key = gameWithDaveKey.trim()
                Network.gameWithDaveApi.dashboard(key, "Bearer $key", key)
            },
            "Sophon" to checkHttp(sophonUrl.trim()),
            "Transmission" to checkHttp("http://192.168.0.234:9091/", transmissionUsername, transmissionPassword)
        )
        testingConnections = false
    }

    private suspend fun check(call: suspend () -> Any?): String = try {
        call()
        "Connected"
    } catch (error: retrofit2.HttpException) {
        if (error.code() == 401 || error.code() == 403) "Authentication failed" else "HTTP ${error.code()}"
    } catch (_: Exception) {
        "Unreachable"
    }

    private suspend fun checkHttp(url: String, username: String = "", password: String = ""): String = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext "URL missing"
        runCatching {
            val request = Request.Builder().url(url).apply {
                if (username.isNotBlank()) header("Authorization", Credentials.basic(username, password))
            }.build()
            Network.client.newCall(request).execute().use { response ->
                when (response.code) {
                    401, 403 -> "Authentication failed"
                    in 200..399 -> "Connected"
                    else -> "HTTP ${response.code}"
                }
            }
        }.getOrDefault("Unreachable")
    }
}

@Composable
fun AppSettingsScreen(onBack: () -> Unit) {
    val vm: AppSettingsViewModel = viewModel()
    val storedMealKey by vm.mealKey.collectAsState()
    val storedTodoKey by vm.todoKey.collectAsState()
    val storedTodoCategory by vm.todoCategory.collectAsState()
    val storedTodoHabit by vm.todoHabit.collectAsState()
    val storedWorkoutKey by vm.workoutKey.collectAsState()
    val storedGameWithDaveKey by vm.gameWithDaveKey.collectAsState()
    val storedGameWithDaveUsername by vm.gameWithDaveUsername.collectAsState()
    val storedGameWithDavePassword by vm.gameWithDavePassword.collectAsState()
    val storedTransmissionUsername by vm.transmissionUsername.collectAsState()
    val storedTransmissionPassword by vm.transmissionPassword.collectAsState()
    val storedSophonUrl by vm.sophonUrl.collectAsState()
    val storedNotificationsEnabled by vm.notificationsEnabled.collectAsState()
    val storedBiometricEnabled by vm.biometricEnabled.collectAsState()
    val storedGameNotificationsEnabled by vm.gameNotificationsEnabled.collectAsState()
    val storedTemperatureNotificationsEnabled by vm.temperatureNotificationsEnabled.collectAsState()
    val storedLowTemperature by vm.lowTemperature.collectAsState()
    val storedHighTemperature by vm.highTemperature.collectAsState()

    var mealKey by rememberSaveable(storedMealKey) { mutableStateOf(storedMealKey) }
    var todoKey by rememberSaveable(storedTodoKey) { mutableStateOf(storedTodoKey) }
    var todoCategory by rememberSaveable(storedTodoCategory) { mutableStateOf(storedTodoCategory) }
    var todoHabit by rememberSaveable(storedTodoHabit) { mutableStateOf(storedTodoHabit) }
    var workoutKey by rememberSaveable(storedWorkoutKey) { mutableStateOf(storedWorkoutKey) }
    var gameWithDaveKey by rememberSaveable(storedGameWithDaveKey) { mutableStateOf(storedGameWithDaveKey) }
    var gameWithDaveUsername by rememberSaveable(storedGameWithDaveUsername) { mutableStateOf(storedGameWithDaveUsername) }
    var gameWithDavePassword by rememberSaveable(storedGameWithDavePassword) { mutableStateOf(storedGameWithDavePassword) }
    var sophonUrl by rememberSaveable(storedSophonUrl) { mutableStateOf(storedSophonUrl) }
    var transmissionUsername by rememberSaveable(storedTransmissionUsername) { mutableStateOf(storedTransmissionUsername) }
    var transmissionPassword by rememberSaveable(storedTransmissionPassword) { mutableStateOf(storedTransmissionPassword) }
    var notificationsEnabled by rememberSaveable(storedNotificationsEnabled) { mutableStateOf(storedNotificationsEnabled) }
    var biometricEnabled by rememberSaveable(storedBiometricEnabled) { mutableStateOf(storedBiometricEnabled) }
    var gameNotificationsEnabled by rememberSaveable(storedGameNotificationsEnabled) { mutableStateOf(storedGameNotificationsEnabled) }
    var temperatureNotificationsEnabled by rememberSaveable(storedTemperatureNotificationsEnabled) { mutableStateOf(storedTemperatureNotificationsEnabled) }
    var lowTemperature by rememberSaveable(storedLowTemperature) { mutableStateOf(storedLowTemperature) }
    var highTemperature by rememberSaveable(storedHighTemperature) { mutableStateOf(storedHighTemperature) }

    Surface(Modifier.fillMaxSize(), color = Color(0xFF1C1C1C)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text("Settings", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SettingsHeading("API access") }
                item { SettingsField("Meal Planner access key", mealKey) { mealKey = it } }
                item { SettingsField("To-Do access key", todoKey) { todoKey = it } }
                item { SettingsField("To-Do default category", todoCategory) { todoCategory = it } }
                item { SettingsField("To-Do default habit", todoHabit) { todoHabit = it } }
                item { SettingsField("Workout access key", workoutKey) { workoutKey = it } }
                item { SettingsField("GameWithDave access key", gameWithDaveKey) { gameWithDaveKey = it } }
                item { Spacer(Modifier.height(6.dp)); SettingsHeading("GameWithDave profile") }
                item { SettingsField("User", gameWithDaveUsername) { gameWithDaveUsername = it } }
                item {
                    SecureSettingsField("Password", gameWithDavePassword) { gameWithDavePassword = it }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Notifications", color = Color.White)
                            Text("Game nights and service alerts", color = Color(0xFF8D8D8D), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                    }
                }
                if (notificationsEnabled) {
                    item { SettingsToggle("Game night alerts", "Notify once for each upcoming night", gameNotificationsEnabled) { gameNotificationsEnabled = it } }
                    item { SettingsToggle("Temperature alerts", "Notify when Sophon leaves your preferred range", temperatureNotificationsEnabled) { temperatureNotificationsEnabled = it } }
                    if (temperatureNotificationsEnabled) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SettingsField("Low °C", lowTemperature, Modifier.weight(1f)) { lowTemperature = it }
                                SettingsField("High °C", highTemperature, Modifier.weight(1f)) { highTemperature = it }
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Protect Settings", color = Color.White)
                            Text("Use fingerprint or device unlock", color = Color(0xFF8D8D8D), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = biometricEnabled, onCheckedChange = { biometricEnabled = it })
                    }
                }
                item { Spacer(Modifier.height(6.dp)); SettingsHeading("Home services") }
                item { SettingsField("Sophon URL", sophonUrl) { sophonUrl = it } }
                item { Spacer(Modifier.height(6.dp)); SettingsHeading("Transmission") }
                item { SettingsField("Username", transmissionUsername) { transmissionUsername = it } }
                item {
                    SecureSettingsField("Password", transmissionPassword) { transmissionPassword = it }
                }
                item {
                    OutlinedButton(
                        onClick = { vm.testConnections(todoKey, workoutKey, gameWithDaveKey, sophonUrl, transmissionUsername, transmissionPassword) },
                        enabled = !vm.testingConnections,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (vm.testingConnections) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Test connections")
                    }
                }
                vm.connectionStatus.forEach { (service, status) ->
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(service, color = Color(0xFFBDBDBD))
                            Text(status, color = if (status == "Connected") Color(0xFF9AD6A3) else Color(0xFFFFB3B3))
                        }
                    }
                }
                item {
                    Button(
                        onClick = {
                            vm.save(mealKey, todoKey, todoCategory, todoHabit, workoutKey, gameWithDaveKey, gameWithDaveUsername, gameWithDavePassword, sophonUrl, transmissionUsername, transmissionPassword, notificationsEnabled, biometricEnabled, gameNotificationsEnabled, temperatureNotificationsEnabled, lowTemperature, highTemperature)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE66A64))
                    ) { Text("Save settings") }
                }
                vm.saveStatus?.let { status -> item { Text(status, color = Color(0xFF9AD6A3)) } }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsHeading(text: String) {
    Text(text, color = Color(0xFFE66A64), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SettingsField(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, modifier, label = { Text(label) }, singleLine = true)
}

@Composable
fun SettingsToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White)
            Text(subtitle, color = Color(0xFF8D8D8D), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SecureSettingsField(label: String, value: String, onValueChange: (String) -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (visible) "Hide password" else "Show password")
            }
        }
    )
}
