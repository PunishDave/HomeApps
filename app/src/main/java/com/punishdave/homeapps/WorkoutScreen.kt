@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.punishdave.homeapps

import android.os.Build
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.decode.GifDecoder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

internal val WorkoutBg = Color(0xFF1C1C1C)
internal val WorkoutPanel = Color(0xFF242424)
internal val WorkoutAccent = Color(0xFFE66A64)

@Composable
fun WorkoutScreen(
    onBack: () -> Unit
) {
    val vm: WorkoutViewModel = viewModel()
    val lastErr by vm.lastError.collectAsState()
    val lastSync by vm.lastSyncStatus.collectAsState()
    val days by vm.days.collectAsState()
    val selectedDayKey by vm.selectedDayKey.collectAsState()
    val selectedDayDetail by vm.selectedDayDetail.collectAsState()
    val latestEntries by vm.latestEntries.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = WorkoutBg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp)
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
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            WorkoutLogSection(
                vm = vm,
                lastErr = lastErr,
                days = days,
                selectedDayKey = selectedDayKey,
                lastSyncStatus = lastSync,
                dayDetail = selectedDayDetail,
                latestEntries = latestEntries
            )
        }
    }
}
@Composable
private fun WorkoutLogSection(
    vm: WorkoutViewModel,
    lastErr: String?,
    days: List<WorkoutDay>,
    selectedDayKey: String?,
    lastSyncStatus: String?,
    dayDetail: WorkoutDay?,
    latestEntries: List<WorkoutLatestEntry>
) {
    val selectedDay = days.firstOrNull { it.day_key == selectedDayKey }
    val activeDay = dayDetail ?: selectedDay
    val stretches = activeDay?.workouts.orEmpty().filter { it.type.equals("stretch", ignoreCase = true) }
    val exercises = activeDay?.workouts.orEmpty().filterNot { it.type.equals("stretch", ignoreCase = true) }

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
                        color = if (isSelected) WorkoutAccent.copy(alpha = 0.3f) else WorkoutPanel,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeDay?.label ?: "Select a day to see workouts",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (activeDay != null) {
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
                    text = "No workouts loaded for this day yet. Pull down on Home to refresh.",
                    color = Color(0xFFB0B0B0),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(exercises, key = { "exercise-${it.name}" }) { move ->
                val latestForMove = latestEntries.firstOrNull { it.workout.equals(move.name, ignoreCase = true) }
                WorkoutMoveCard(
                    move = move,
                    latest = latestForMove,
                    onAdd = { weight -> vm.addEntryForMove(move, weight, activeDay.day_key) }
                )
            }
            if (stretches.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Stretches",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(stretches, key = { "stretch-${it.name}" }) { stretch ->
                    StretchCard(stretch)
                }
            }
        }
    }
}
