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
import java.time.format.DateTimeFormatter

private val WorkoutBg = Color(0xFF1C1C1C)
private val WorkoutPanel = Color(0xFF0F0F0F)
private val WorkoutAccent = Color(0xFFB00020)
private val DateDisplayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

private enum class WorkoutTab { Log, Settings }

@Composable
fun WorkoutScreen(
    onBack: () -> Unit
) {
    val vm: WorkoutViewModel = viewModel()
    val entries by vm.entries.collectAsState()
    val workoutText by vm.workoutText.collectAsState()
    val notesText by vm.notesText.collectAsState()
    val dateText by vm.dateText.collectAsState()
    val lastErr by vm.lastError.collectAsState()
    val accessKey by vm.accessKey.collectAsState()
    val lastSync by vm.lastSyncStatus.collectAsState()
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
                    workoutText = workoutText,
                    notesText = notesText,
                    dateText = dateText,
                    lastErr = lastErr
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
    workoutText: String,
    notesText: String,
    dateText: String,
    lastErr: String?
) {
    val availableDates = remember(entries) {
        entries.mapNotNull { it.date.takeIf { it.isNotBlank() } }
            .distinct()
            .sortedWith(compareByDescending<String> { parseDate(it) ?: LocalDate.MIN })
    }
    val visibleDates = if (availableDates.isEmpty()) {
        listOf(dateText.ifBlank { LocalDate.now().toString() })
    } else {
        availableDates
    }
    var selectedDate by rememberSaveable(visibleDates) {
        mutableStateOf(visibleDates.first())
    }

    // When entries change, keep selection to a valid date
    LaunchedEffect(visibleDates) {
        if (visibleDates.isNotEmpty() && selectedDate !in visibleDates) {
            selectedDate = visibleDates.first()
        }
    }

    val dayEntries = remember(selectedDate, entries) {
        entries.filter { it.date == selectedDate }
    }
    val lastByWorkout = remember(entries) { lastLogByWorkout(entries) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = WorkoutPanel,
                border = BorderStroke(1.dp, WorkoutAccent.copy(alpha = 0.4f)),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = dateText,
                        onValueChange = { vm.dateText.value = it },
                        singleLine = true,
                        label = { Text("Date (YYYY-MM-DD)") }
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = workoutText,
                        onValueChange = { vm.workoutText.value = it },
                        singleLine = true,
                        label = { Text("Workout / Day") },
                        leadingIcon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) }
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = notesText,
                        onValueChange = { vm.notesText.value = it },
                        singleLine = false,
                        maxLines = 3,
                        label = { Text("Notes (weight, reps, etc.)") }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledIconButton(
                            onClick = { vm.addEntry() },
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

        if (lastErr != null) {
            item {
                Text(
                    text = lastErr,
                    color = Color(0xFFFFB3B3),
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
                items(visibleDates.size) { idx ->
                    val dateStr = visibleDates[idx]
                    val isSelected = dateStr == selectedDate
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) WorkoutAccent else WorkoutPanel,
                        border = BorderStroke(1.dp, WorkoutAccent.copy(alpha = 0.5f)),
                        onClick = { selectedDate = dateStr }
                    ) {
                        Text(
                            text = formatDate(dateStr),
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
                text = "Workouts on ${formatDate(selectedDate)}",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (dayEntries.isEmpty()) {
            item {
                Text(
                    text = "No entries for this day yet. Add one above to start tracking.",
                    color = Color(0xFFB0B0B0),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(dayEntries) { entry ->
                WorkoutRow(
                    entry = entry,
                    last = lastByWorkout[entry.workout.trim().lowercase()],
                    onDelete = { vm.deleteEntry(entry.id) }
                )
            }
        }

        if (entries.isNotEmpty()) {
            item {
                Text(
                    text = "All entries (latest first)",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(entries.sortedWith(compareByDescending<WorkoutEntry> { parseDate(it.date) ?: LocalDate.MIN })) { entry ->
                WorkoutRow(
                    entry = entry,
                    last = lastByWorkout[entry.workout.trim().lowercase()],
                    onDelete = { vm.deleteEntry(entry.id) }
                )
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
                        text = entry.date,
                        color = Color(0xFFBDBDBD),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete entry", tint = Color(0xFFFF9B9B))
                }
            }

            if (entry.notes.isNotBlank()) {
                Text(
                    text = entry.notes,
                    color = Color(0xFFDFDFDF),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (last != null) {
                val lastLabel = if (last.date == entry.date) "Last logged today" else "Last logged ${formatDate(last.date)}"
                val lastNotes = last.notes.takeIf { it.isNotBlank() }
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

private fun parseDate(raw: String): LocalDate? = runCatching { LocalDate.parse(raw) }.getOrNull()

private fun formatDate(raw: String): String {
    val parsed = parseDate(raw)
    return parsed?.format(DateDisplayFmt) ?: raw
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
