@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.punishdave.homeapps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

private val WorkoutBg = Color(0xFF1C1C1C)
private val WorkoutPanel = Color(0xFF0F0F0F)
private val WorkoutAccent = Color(0xFFB00020)

private enum class WorkoutTab { Log, Settings }

@Composable
fun WorkoutScreen(
    onBack: () -> Unit
) {
    val vm: WorkoutViewModel = viewModel()
    val entries by vm.entries.collectAsState()
    val lastErr by vm.lastError.collectAsState()
    val accessKey by vm.accessKey.collectAsState()
    val lastSync by vm.lastSyncStatus.collectAsState()
    val days by vm.days.collectAsState()
    val selectedDayKey by vm.selectedDayKey.collectAsState()
    val selectedDayDetail by vm.selectedDayDetail.collectAsState()
    val latestEntries by vm.latestEntries.collectAsState()
    var currentTab by rememberSaveable { mutableStateOf(WorkoutTab.Log) }

    Surface(modifier = Modifier.fillMaxSize(), color = WorkoutBg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color(0xFFBDBDBD))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Workout Log",
                    color = WorkoutAccent,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WorkoutTabCard(
                    modifier = Modifier.weight(1f),
                    title = "Log",
                    subtitle = "Add & view sessions",
                    selected = currentTab == WorkoutTab.Log,
                    onClick = { currentTab = WorkoutTab.Log }
                )
                WorkoutTabCard(
                    modifier = Modifier.weight(1f),
                    title = "Settings",
                    subtitle = "Access key & sync",
                    selected = currentTab == WorkoutTab.Settings,
                    onClick = { currentTab = WorkoutTab.Settings }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (currentTab) {
                WorkoutTab.Log -> WorkoutLogSection(
                    vm = vm,
                    entries = entries,
                    lastErr = lastErr,
                    days = days,
                    selectedDayKey = selectedDayKey,
                    lastSyncStatus = lastSync,
                    dayDetail = selectedDayDetail,
                    latestEntries = latestEntries
                )
                WorkoutTab.Settings -> WorkoutSettingsSection(
                    accessKey = accessKey,
                    lastErr = lastErr,
                    lastSync = lastSync,
                    onAccessKeyChange = { vm.saveAccessKey(it) },
                    onSync = { vm.syncFromApi() }
                )
            }
        }
    }
}

@Composable
private fun WorkoutLogSection(
    vm: WorkoutViewModel,
    entries: List<WorkoutEntry>,
    lastErr: String?,
    days: List<WorkoutDay>,
    selectedDayKey: String?,
    lastSyncStatus: String?,
    dayDetail: WorkoutDay?,
    latestEntries: List<WorkoutLatestEntry>
) {
    val selectedDay = days.firstOrNull { it.day_key == selectedDayKey }
    val activeDay = dayDetail ?: selectedDay

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (lastErr != null) {
            item {
                Text(
                    text = lastErr,
                    color = Color(0xFFFFB3B3),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        lastSyncStatus?.takeIf { it.isNotBlank() }?.let { status ->
            item {
                Text(
                    text = status,
                    color = Color(0xFFBDBDBD),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            Text(
                text = "Available days",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days.size) { idx ->
                    val day = days[idx]
                    val isSelected = day.day_key == selectedDayKey
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) WorkoutAccent else WorkoutPanel,
                        border = BorderStroke(1.dp, WorkoutAccent.copy(alpha = 0.5f)),
                        onClick = { vm.selectDay(day.day_key) }
                    ) {
                        Text(
                            text = day.label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = activeDay?.label ?: "Select a day to see workouts",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (activeDay != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledIconButton(
                        onClick = { vm.pushEntriesForDay(activeDay.day_key) },
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = WorkoutAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = "Push day to API")
                    }
                }
            }
        }

        if (activeDay == null || activeDay.workouts.isEmpty()) {
            item {
                Text(
                    text = "No workouts loaded for this day yet. Hit Sync in Settings to fetch days.",
                    color = Color(0xFFB0B0B0),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(activeDay.workouts, key = { it.name }) { move ->
                val latestForMove = latestEntries.firstOrNull { it.workout.equals(move.name, ignoreCase = true) }
                val history = remember(entries, move.name) {
                    entries.filter { it.workout.equals(move.name, ignoreCase = true) }
                        .sortedWith(compareByDescending<WorkoutEntry> { parseDate(it.date) ?: LocalDate.MIN })
                }
                WorkoutMoveCard(
                    move = move,
                    latest = latestForMove,
                    onAdd = { weight -> vm.addEntryForMove(move, weight, activeDay.day_key) }
                )
            }
        }
    }
}

@Composable
private fun WorkoutMoveCard(
    move: WorkoutMove,
    latest: WorkoutLatestEntry?,
    onAdd: (String) -> Unit
) {
    var weightText by rememberSaveable(move.name) { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = WorkoutPanel,
        border = BorderStroke(1.dp, WorkoutAccent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = move.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                displayReps(move, latest)?.let { reps ->
                    Text(
                        text = "Reps: $reps",
                        color = WorkoutAccent,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            move.type?.takeIf { it.isNotBlank() }?.let { type ->
                Text(
                    text = type,
                    color = Color(0xFFBDBDBD),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val latestWeight = latest?.weight ?: move.last_weight
            val latestDate = latest?.performedOn ?: move.last_performed_on
            val latestReps = latest?.reps ?: move.last_reps
            val lastParts = buildList {
                if (!latestWeight.isNullOrBlank()) add(latestWeight)
                latestReps?.let { add("x$it") }
            }
            if (lastParts.isNotEmpty() || latestDate != null) {
                val dateLabel = latestDate?.let { formatDate(it) }
                val prefix = if (lastParts.isNotEmpty()) "Last: ${lastParts.joinToString(" ")}" else "Last:"
                val text = listOfNotNull(prefix, dateLabel).joinToString(" – ")
                Text(
                    text = text,
                    color = Color(0xFFDFDFDF),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            if (lastParts.isEmpty() && latestDate == null) {
                Text(
                    text = "No previous entry yet. Add a weight to get started.",
                    color = Color(0xFF9E9E9E),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = weightText,
                onValueChange = { weightText = it },
                singleLine = true,
                label = {
                    val isTime = move.type.equals("time", ignoreCase = true)
                    Text(if (isTime) "Time (e.g., 01:00)" else "Weight")
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledIconButton(
                    onClick = {
                        val trimmed = weightText.trim()
                        if (trimmed.isNotEmpty()) {
                            onAdd(trimmed)
                            weightText = ""
                        }
                    },
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = WorkoutAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add entry")
                }
            }
        }
    }
}

@Composable
private fun WorkoutSettingsSection(
    accessKey: String,
    lastErr: String?,
    lastSync: String?,
    onAccessKeyChange: (String) -> Unit,
    onSync: () -> Unit
) {
    var accessKeyInput by rememberSaveable { mutableStateOf(accessKey) }

    LaunchedEffect(accessKey) { accessKeyInput = accessKey }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        color = WorkoutPanel,
        border = BorderStroke(1.dp, WorkoutAccent.copy(alpha = 0.4f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sync & API",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = accessKeyInput,
                onValueChange = {
                    accessKeyInput = it
                    onAccessKeyChange(it)
                },
                singleLine = true,
                label = { Text("Access key") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledIconButton(
                    onClick = onSync,
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = WorkoutAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Sync, contentDescription = "Sync")
                }
            }

            lastErr?.let {
                Text(
                    text = it,
                    color = Color(0xFFFF9B9B),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!lastSync.isNullOrBlank()) {
                Text(
                    text = lastSync,
                    color = Color(0xFFBDBDBD),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Not synced yet. Enter the access key and press Sync.",
                    color = Color(0xFFBDBDBD),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun WorkoutRow(
    entry: WorkoutEntry,
    last: WorkoutEntry?,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = WorkoutPanel,
        border = BorderStroke(1.dp, WorkoutAccent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = entry.workout,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = formatDate(entry.date),
                        color = Color(0xFFBDBDBD),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete entry", tint = Color(0xFFFF9B9B))
                }
            }

            entryWeight(entry)?.let { w ->
                Text(
                    text = "Weight: $w",
                    color = Color(0xFFDFDFDF),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (last != null) {
                val lastLabel = if (last.date == entry.date) "Last logged today" else "Last logged ${formatDate(last.date)}"
                val lastNotes = entryWeight(last)?.let { "Weight: $it" }
                Text(
                    text = listOfNotNull(lastLabel, lastNotes).joinToString(" – "),
                    color = Color(0xFFBDBDBD),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun parseDate(raw: String): LocalDate? = parseFlexibleDate(raw)

private fun formatDate(raw: String): String {
    return formatDateForDisplay(raw) ?: raw
}

private fun entryWeight(entry: WorkoutEntry): String? {
    return entry.weight?.takeIf { it.isNotBlank() } ?: entry.notes.takeIf { it.isNotBlank() }
}

private fun displayReps(move: WorkoutMove, latest: WorkoutLatestEntry?): Int? {
    return move.reps?.takeIf { it > 0 }
        ?: latest?.reps?.takeIf { it > 0 }
        ?: move.last_reps?.takeIf { it > 0 }
}

private fun lastLogByWorkout(entries: List<WorkoutEntry>): Map<String, WorkoutEntry> {
    val sorted = entries.sortedWith(compareByDescending<WorkoutEntry> { parseDate(it.date) ?: LocalDate.MIN })
    val map = mutableMapOf<String, WorkoutEntry>()
    for (e in sorted) {
        val key = e.workout.trim().lowercase()
        if (key.isNotEmpty() && key !in map) {
            map[key] = e
        }
    }
    return map
}

@Composable
private fun WorkoutTabCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) WorkoutAccent else WorkoutAccent.copy(alpha = 0.4f)
    val bg = if (selected) WorkoutPanel else Color(0xFF121212)
    Surface(
        modifier = modifier.heightIn(min = 80.dp),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, borderColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color(0xFFBDBDBD),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }
    }
}
