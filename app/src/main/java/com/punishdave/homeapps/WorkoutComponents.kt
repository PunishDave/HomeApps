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

@Composable
internal fun StretchCard(stretch: WorkoutMove) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = WorkoutPanel,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stretch.name, color = Color.White, fontWeight = FontWeight.Bold)
                stretch.duration_seconds?.takeIf { it > 0 }?.let {
                    Text("${it}s", color = WorkoutAccent, fontWeight = FontWeight.SemiBold)
                }
            }
            stretch.media_url?.takeIf { it.isNotBlank() }?.let { mediaUrl ->
                AsyncImage(
                    model = mediaUrl,
                    imageLoader = imageLoader,
                    contentDescription = "Demonstration of ${stretch.name}",
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
internal fun WorkoutMoveCard(
    move: WorkoutMove,
    latest: WorkoutLatestEntry?,
    onAdd: (String) -> Unit
) {
    var weightText by rememberSaveable(move.name) { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = WorkoutPanel,
        tonalElevation = 0.dp
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
internal fun WorkoutRow(
    entry: WorkoutEntry,
    last: WorkoutEntry?,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = WorkoutPanel,
        tonalElevation = 0.dp
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

internal fun parseDate(raw: String): LocalDate? = parseFlexibleDate(raw)

internal fun formatDate(raw: String): String {
    return formatDateForDisplay(raw) ?: raw
}

internal fun entryWeight(entry: WorkoutEntry): String? {
    return entry.weight?.takeIf { it.isNotBlank() } ?: entry.notes.takeIf { it.isNotBlank() }
}

internal fun displayReps(move: WorkoutMove, latest: WorkoutLatestEntry?): Int? {
    return move.reps?.takeIf { it > 0 }
        ?: latest?.reps?.takeIf { it > 0 }
        ?: move.last_reps?.takeIf { it > 0 }
}

@Composable
internal fun WorkoutTabCard(
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

