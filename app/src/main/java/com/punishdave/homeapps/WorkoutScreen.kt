package com.punishdave.homeapps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
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
    val availableDates = remember(entries) {
        entries.mapNotNull { it.date.takeIf { it.isNotBlank() } }
            .distinct()
            .sortedWith(compareByDescending<String> { parseDate(it) ?: LocalDate.MIN })
    }
    var selectedDate by rememberSaveable(availableDates) {
        mutableStateOf(availableDates.firstOrNull() ?: dateText)
    }

    // When entries change, keep selection to a valid date
    LaunchedEffect(availableDates) {
        if (availableDates.isNotEmpty() && selectedDate !in availableDates) {
            selectedDate = availableDates.first()
        }
    }

    val dayEntries = remember(selectedDate, entries) {
        entries.filter { it.date == selectedDate }
    }
    val lastByWorkout = remember(entries) { lastLogByWorkout(entries) }

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

            if (lastErr != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lastErr ?: "",
                    color = Color(0xFFFFB3B3),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (entries.isEmpty()) {
                Text(
                    text = "No workouts logged yet. Add your last session.",
                    color = Color(0xFFB0B0B0),
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            // Day selector
            Text(
                text = "Available days",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableDates.size) { idx ->
                            val dateStr = availableDates[idx]
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

                // Selected day details
                item {
                    Spacer(modifier = Modifier.height(6.dp))
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
                            text = "No entries for this day yet.",
                            color = Color(0xFFB0B0B0),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
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

                item {
                    Spacer(modifier = Modifier.height(16.dp))
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
