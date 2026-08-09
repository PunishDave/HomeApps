package com.punishdave.homeapps

import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

private val GwdBackground = Color(0xFF1C1C1C)
private val GwdPanel = Color(0xFF242424)
private val GwdPanelSoft = Color(0xFF202020)
private val GwdBorder = Color(0xFF383838)
private val GwdMuted = Color(0xFF999999)
private val GwdAccent = Color(0xFFE66A64)
private val GwdPositive = Color(0xFF7DB68A)
private val GwdDateFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.UK)
    .withResolverStyle(ResolverStyle.STRICT)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GameWithDaveScreen(onBack: () -> Unit) {
    val vm: GameWithDaveViewModel = viewModel()
    val dashboard by vm.dashboard.collectAsState()
    val loading by vm.isLoading.collectAsState()
    val message by vm.message.collectAsState()
    val accessKey by vm.accessKey.collectAsState()
    val username by vm.username.collectAsState()
    var selectedCalendarMonth by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val pullRefresh = rememberPullRefreshState(loading, vm::refresh)

    Surface(Modifier.fillMaxSize(), color = GwdBackground) {
        Box(Modifier.fillMaxSize().pullRefresh(pullRefresh)) {
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { GameWithDaveHeader(onBack) }
                if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = GwdAccent) }
                message?.let { item { StatusMessage(it) } }

                if (accessKey.isBlank()) {
                    item { EmptyPanel("Add the GameWithDave access key in Settings before refreshing.") }
                } else {
                    item {
                        CompactCalendar(
                            days = dashboard.days,
                            selectedMonth = selectedCalendarMonth,
                            onMonthSelected = { selectedCalendarMonth = it }
                        )
                    }
                    item { SectionLabel("UPDATE", "Your availability") }
                    item { AvailabilityEditor(username, loading, vm::saveAvailability) }
                    item { SectionLabel("ADMIN", "Upcoming game nights") }
                    if (!loading && dashboard.days.none { it.game_nights.isNotEmpty() }) {
                        item { EmptyPanel("No upcoming game nights yet.") }
                    }
                    items(
                        dashboard.days.filter { it.game_nights.isNotEmpty() },
                        key = { it.date }
                    ) { day -> AdminDayRow(day, loading, vm::updateNight) }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
            PullRefreshIndicator(
                loading,
                pullRefresh,
                Modifier.align(Alignment.TopCenter).statusBarsPadding(),
                GwdPanel,
                GwdAccent
            )
        }
    }
}

@Composable
private fun GameWithDaveHeader(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFBDBDBD))
        }
        Spacer(Modifier.width(4.dp))
        Column {
            Text("GameWithDave", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Availability and game nights", color = GwdMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CompactCalendar(
    days: List<GameWithDaveDay>,
    selectedMonth: String,
    onMonthSelected: (String) -> Unit
) {
    val datedDays = remember(days) { days.mapNotNull { day -> runCatching { LocalDate.parse(day.date) }.getOrNull()?.let { it to day } }.toMap() }
    val month = YearMonth.parse(selectedMonth)
    val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
    val cells = List(firstDayOffset) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    val paddedCells = cells + List((7 - cells.size % 7) % 7) { null }

    Surface(color = GwdPanel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, GwdBorder)) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { onMonthSelected(month.minusMonths(1).toString()) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ChevronLeft, "Previous month", tint = GwdMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        month.format(DateTimeFormatter.ofPattern("MMMM", Locale.UK)),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(month.year.toString(), color = GwdMuted, style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { onMonthSelected(month.plusMonths(1).toString()) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ChevronRight, "Next month", tint = GwdMuted)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Text(it, Modifier.weight(1f).padding(vertical = 5.dp), color = GwdMuted, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                }
            }
            paddedCells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    week.forEach { date -> CalendarDay(date, date?.let(datedDays::get), Modifier.weight(1f)) }
                }
            }
            CalendarLegend(Modifier.padding(horizontal = 14.dp, vertical = 11.dp))
        }
    }
}

@Composable
private fun CalendarDay(date: LocalDate?, day: GameWithDaveDay?, modifier: Modifier = Modifier) {
    val today = date == LocalDate.now()
    val hasGame = day?.game_nights?.any { it.status != "removed" } == true
    Column(
        modifier.height(48.dp).padding(2.dp).clip(RoundedCornerShape(9.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (date != null) {
            Surface(
                color = if (today) GwdAccent else Color.Transparent,
                shape = CircleShape,
                border = if (hasGame && !today) BorderStroke(1.dp, GwdAccent) else null
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    Modifier.size(27.dp).padding(top = 5.dp),
                    color = if (today) Color.White else if (date.isBefore(LocalDate.now())) Color(0xFF666666) else Color(0xFFE2E2E2),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (today || hasGame) FontWeight.Bold else FontWeight.Normal
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                day?.availability?.take(4)?.forEach { availability ->
                    Box(Modifier.size(4.dp).clip(CircleShape).then(Modifier), contentAlignment = Alignment.Center) {
                        Surface(
                            Modifier.fillMaxSize(),
                            color = when (availability.status) {
                                "yes" -> GwdPositive
                                "tentative" -> Color(0xFFD6A85C)
                                else -> Color(0xFF656565)
                            },
                            shape = CircleShape
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegend(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(GwdAccent, "Game night")
        LegendItem(GwdPositive, "Available")
        LegendItem(Color(0xFFD6A85C), "Tentative")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Surface(Modifier.size(6.dp), shape = CircleShape, color = color) {}
        Text(label, color = GwdMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SectionLabel(eyebrow: String, title: String) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(eyebrow, color = GwdAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AvailabilityEditor(username: String, loading: Boolean, onSave: (String, String, String) -> Unit) {
    var status by remember { mutableStateOf("yes") }
    var startDate by remember { mutableStateOf(LocalDate.now().format(GwdDateFormatter)) }
    var endDate by remember { mutableStateOf(LocalDate.now().format(GwdDateFormatter)) }
    val context = LocalContext.current

    Surface(color = GwdPanelSoft, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, GwdBorder)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Updating as", color = GwdMuted, style = MaterialTheme.typography.labelSmall)
                    Text(username.ifBlank { "Set user in Settings" }, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Box(Modifier.width(150.dp)) { StatusDropdown(status) { status = it } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DatePickerField("From", startDate, Modifier.weight(1f)) {
                    showGameWithDaveDatePicker(context, startDate) { startDate = it }
                }
                DatePickerField("To", endDate, Modifier.weight(1f)) {
                    showGameWithDaveDatePicker(context, endDate) { endDate = it }
                }
            }
            Button(
                onClick = { onSave(startDate, endDate, status) },
                enabled = !loading && username.isNotBlank() && validDate(startDate) && validDate(endDate),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GwdAccent)
            ) { Text("Update availability") }
        }
    }
}

@Composable
private fun DatePickerField(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(58.dp).clickable(onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color(0xFF777777))
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = GwdMuted, style = MaterialTheme.typography.labelSmall)
                Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.CalendarMonth, contentDescription = "Choose $label date", tint = GwdAccent, modifier = Modifier.size(20.dp))
        }
    }
}

private fun showGameWithDaveDatePicker(
    context: android.content.Context,
    current: String,
    onSelected: (String) -> Unit
) {
    val initial = runCatching { LocalDate.parse(current, GwdDateFormatter) }.getOrDefault(LocalDate.now())
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onSelected(LocalDate.of(year, month + 1, day).format(GwdDateFormatter))
        },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth
    ).show()
}

@Composable
private fun StatusDropdown(value: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "yes" to "Available",
        "tentative" to "Tentative",
        "no" to "Not available"
    )
    Box(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(58.dp).clickable { expanded = true },
            color = Color.Transparent,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, Color(0xFF777777))
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Status", color = GwdMuted, style = MaterialTheme.typography.labelSmall)
                    Text(options.first { it.first == value }.second, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Choose status", tint = GwdAccent)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(190.dp)
        ) {
            options.forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AdminDayRow(day: GameWithDaveDay, loading: Boolean, onUpdate: (GameWithDaveNight, String) -> Unit) {
    Surface(color = GwdPanelSoft, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, GwdBorder)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(day.display_date, color = GwdMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            day.game_nights.forEach { night ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape)) { Surface(Modifier.fillMaxSize(), color = GwdAccent) {} }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(night.team_label, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(night.status.replaceFirstChar { it.uppercase() }, color = GwdAccent, style = MaterialTheme.typography.labelSmall)
                    }
                    if (night.status != "locked") {
                        OutlinedButton(onClick = { onUpdate(night, "lock") }, enabled = !loading, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Lock") }
                        Spacer(Modifier.width(6.dp))
                    }
                    OutlinedButton(onClick = { onUpdate(night, "remove") }, enabled = !loading && night.status != "removed", contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(text: String) {
    val error = text.contains("fail", true) || text.contains("unable", true) || text.contains("error", true)
    Surface(color = if (error) Color(0xFF3A2424) else Color(0xFF203126), shape = RoundedCornerShape(10.dp)) {
        Text(text, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), color = if (error) Color(0xFFFFB3B3) else Color(0xFF9AD6A3), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyPanel(text: String) {
    Surface(color = GwdPanel, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, GwdBorder)) {
        Text(text, Modifier.fillMaxWidth().padding(22.dp), color = Color(0xFFBDBDBD))
    }
}

private fun validDate(value: String): Boolean = runCatching { LocalDate.parse(value, GwdDateFormatter) }.isSuccess
