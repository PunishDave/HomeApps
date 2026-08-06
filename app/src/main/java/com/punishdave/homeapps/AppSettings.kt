package com.punishdave.homeapps

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    val mealKey = mealStore.accessKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val todoKey = todoStore.accessKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val todoCategory = todoStore.categoryFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val todoHabit = todoStore.habitFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val workoutKey = workoutStore.accessKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val gameWithDaveKey = gameWithDaveStore.accessKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val transmissionUsername = context.webSettingsDataStore.data
        .map { it[transmissionUserKey] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val transmissionPassword = context.webSettingsDataStore.data
        .map { it[transmissionPasswordKey] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val sophonUrl = context.webSettingsDataStore.data
        .map { it[sophonUrlKey] ?: "http://192.168.0.234:8096" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    var saveStatus by mutableStateOf<String?>(null)
        private set

    fun save(
        mealKey: String,
        todoKey: String,
        todoCategory: String,
        todoHabit: String,
        workoutKey: String,
        gameWithDaveKey: String,
        sophonUrl: String,
        transmissionUsername: String,
        transmissionPassword: String
    ) = viewModelScope.launch {
        mealStore.saveAccessKey(mealKey.trim())
        todoStore.saveAccessKey(todoKey.trim())
        todoStore.saveCategory(todoCategory.trim())
        todoStore.saveHabit(todoHabit.trim())
        workoutStore.saveAccessKey(workoutKey.trim())
        gameWithDaveStore.saveAccessKey(gameWithDaveKey.trim())
        context.webSettingsDataStore.edit {
            it[sophonUrlKey] = sophonUrl.trim().ifBlank { "http://192.168.0.234:8096" }
            it[transmissionUserKey] = transmissionUsername.trim()
            it[transmissionPasswordKey] = transmissionPassword
        }
        saveStatus = "Settings saved"
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
    val storedTransmissionUsername by vm.transmissionUsername.collectAsState()
    val storedTransmissionPassword by vm.transmissionPassword.collectAsState()
    val storedSophonUrl by vm.sophonUrl.collectAsState()

    var mealKey by rememberSaveable(storedMealKey) { mutableStateOf(storedMealKey) }
    var todoKey by rememberSaveable(storedTodoKey) { mutableStateOf(storedTodoKey) }
    var todoCategory by rememberSaveable(storedTodoCategory) { mutableStateOf(storedTodoCategory) }
    var todoHabit by rememberSaveable(storedTodoHabit) { mutableStateOf(storedTodoHabit) }
    var workoutKey by rememberSaveable(storedWorkoutKey) { mutableStateOf(storedWorkoutKey) }
    var gameWithDaveKey by rememberSaveable(storedGameWithDaveKey) { mutableStateOf(storedGameWithDaveKey) }
    var sophonUrl by rememberSaveable(storedSophonUrl) { mutableStateOf(storedSophonUrl) }
    var transmissionUsername by rememberSaveable(storedTransmissionUsername) { mutableStateOf(storedTransmissionUsername) }
    var transmissionPassword by rememberSaveable(storedTransmissionPassword) { mutableStateOf(storedTransmissionPassword) }

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
                item { Spacer(Modifier.height(6.dp)); SettingsHeading("Home services") }
                item { SettingsField("Sophon URL", sophonUrl) { sophonUrl = it } }
                item { Spacer(Modifier.height(6.dp)); SettingsHeading("Transmission") }
                item { SettingsField("Username", transmissionUsername) { transmissionUsername = it } }
                item {
                    OutlinedTextField(
                        value = transmissionPassword,
                        onValueChange = { transmissionPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
                item {
                    Button(
                        onClick = {
                            vm.save(mealKey, todoKey, todoCategory, todoHabit, workoutKey, gameWithDaveKey, sophonUrl, transmissionUsername, transmissionPassword)
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
private fun SettingsField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true)
}
