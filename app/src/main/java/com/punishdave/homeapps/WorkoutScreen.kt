package com.punishdave.homeapps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val WorkoutBg = Color(0xFF1C1C1C)
private val WorkoutPanel = Color(0xFF0F0F0F)
private val WorkoutAccent = Color(0xFFB00020)

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFFBDBDBD))
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries) { entry ->
                        WorkoutRow(
                            entry = entry,
                            onDelete = { vm.deleteEntry(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutRow(
    entry: WorkoutEntry,
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
        }
    }
}
