package com.punishdave.homeapps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

private val GwdBackground = Color(0xFF1C1C1C)
private val GwdPanel = Color(0xFF242424)
private val GwdAccent = Color(0xFFE66A64)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GameWithDaveScreen(onBack: () -> Unit) {
    val vm: GameWithDaveViewModel = viewModel()
    val dashboard by vm.dashboard.collectAsState()
    val loading by vm.isLoading.collectAsState()
    val message by vm.message.collectAsState()
    val accessKey by vm.accessKey.collectAsState()
    val pullRefresh = rememberPullRefreshState(loading, vm::refresh)

    Surface(Modifier.fillMaxSize(), color = GwdBackground) {
        Box(Modifier.fillMaxSize().pullRefresh(pullRefresh)) {
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFBDBDBD))
                        }
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text("GameWithDave", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Pull down to refresh", color = Color(0xFF8D8D8D), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = GwdAccent) }
                message?.let { item { Text(it, color = if (it.contains("fail", true) || it.contains("unable", true)) Color(0xFFFFB3B3) else Color(0xFF9AD6A3)) } }
                if (accessKey.isBlank()) {
                    item { EmptyPanel("Add the GameWithDave access key in Settings before refreshing.") }
                } else {
                    item { AvailabilityEditor(dashboard.users, loading, vm::saveAvailability) }
                    item {
                        Text("Upcoming activity", color = GwdAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (!loading && dashboard.days.isEmpty()) item { EmptyPanel("No upcoming availability or game nights yet.") }
                    items(dashboard.days, key = { it.date }) { day ->
                        GameDayPanel(day, loading, vm::updateNight)
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
            PullRefreshIndicator(loading, pullRefresh, Modifier.align(Alignment.TopCenter).statusBarsPadding(), GwdPanel, GwdAccent)
        }
    }
}

@Composable
private fun AvailabilityEditor(
    users: List<GameWithDaveUser>,
    loading: Boolean,
    onSave: (String, String, String, String) -> Unit
) {
    var user by remember(users) { mutableStateOf(users.firstOrNull()?.role.orEmpty()) }
    var status by remember { mutableStateOf("yes") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().toString()) }

    Surface(color = GwdPanel, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF3A3A3A))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Set availability", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ChoiceField("Member", user, users.map { it.role }) { user = it }
            ChoiceField("Status", status, listOf("yes", "tentative", "no")) { status = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(startDate, { startDate = it }, Modifier.weight(1f), label = { Text("Start YYYY-MM-DD") }, singleLine = true)
                OutlinedTextField(endDate, { endDate = it }, Modifier.weight(1f), label = { Text("End YYYY-MM-DD") }, singleLine = true)
            }
            Button(
                onClick = { onSave(user, startDate, endDate, status) },
                enabled = !loading && user.isNotBlank() && validDate(startDate) && validDate(endDate),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GwdAccent)
            ) { Text("Save availability") }
        }
    }
}

@Composable
private fun ChoiceField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value.replaceFirstChar { it.uppercase() },
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            label = { Text(label) },
            readOnly = true,
            enabled = options.isNotEmpty()
        )
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar { it.uppercase() }) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun GameDayPanel(day: GameWithDaveDay, loading: Boolean, onUpdate: (GameWithDaveNight, String) -> Unit) {
    Surface(color = GwdPanel, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF353535))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(day.display_date, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (day.availability.isNotEmpty()) {
                Text(
                    day.availability.joinToString("  ") { "${it.initials} ${it.status}" },
                    color = Color(0xFFBDBDBD),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            day.game_nights.forEach { night ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(night.team_label, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(night.status.replaceFirstChar { it.uppercase() }, color = GwdAccent, style = MaterialTheme.typography.labelMedium)
                    }
                    if (night.status != "locked") {
                        OutlinedButton(onClick = { onUpdate(night, "lock") }, enabled = !loading) { Text("Lock") }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onUpdate(night, "remove") }, enabled = !loading && night.status != "removed") { Text("Remove") }
                }
            }
        }
    }
}

@Composable
private fun EmptyPanel(text: String) {
    Surface(color = GwdPanel, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF353535))) {
        Text(text, Modifier.fillMaxWidth().padding(22.dp), color = Color(0xFFBDBDBD))
    }
}

private fun validDate(value: String): Boolean = runCatching { LocalDate.parse(value) }.isSuccess
