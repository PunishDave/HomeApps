@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.punishdave.homeapps

import android.app.DatePickerDialog
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

@Composable
internal fun TodoRow(
    task: TodoItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val rowAlpha = if (task.done) 0.45f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .alpha(rowAlpha)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                role = Role.Checkbox,
                onClick = onToggle
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle() })

        Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    task.dueDate?.takeIf { it.isNotBlank() }?.let { due ->
                        Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = shortDate(due),
                        color = dueTextColor(due),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = Color(0xFF777777), modifier = Modifier.size(18.dp))
        }
    }
}

private fun shortDate(raw: String): String {
    return friendlyDateLabel(raw) ?: raw.trim()
}

internal fun showDueDatePicker(context: android.content.Context, current: String, onSelected: (String) -> Unit) {
    val initial = parseFlexibleDate(current) ?: LocalDate.now()
    val dialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val picked = LocalDate.of(year, month + 1, dayOfMonth)
            onSelected(picked.toDisplayDate())
        },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth
    )
    dialog.show()
}

private fun dueTextColor(raw: String): Color {
    val parsed = parseFlexibleDate(raw)
    val isToday = parsed == LocalDate.now()
    return if (isToday) TodoAccent else Color(0xFFBDBDBD)
}

internal fun orderTasksByDue(tasks: List<TodoItem>): List<TodoItem> {
    val today = LocalDate.now()
    val todayEpoch = today.toEpochDay()
    val comparator = Comparator<TodoItem> { a, b ->
        val pa = parseFlexibleDate(a.dueDate)
        val pb = parseFlexibleDate(b.dueDate)

        val da = pa?.let { abs(it.toEpochDay() - todayEpoch) } ?: Long.MAX_VALUE
        val db = pb?.let { abs(it.toEpochDay() - todayEpoch) } ?: Long.MAX_VALUE
        if (da != db) return@Comparator da.compareTo(db)

        val ea = pa?.toEpochDay() ?: Long.MAX_VALUE
        val eb = pb?.toEpochDay() ?: Long.MAX_VALUE
        if (ea != eb) return@Comparator ea.compareTo(eb)

        a.title.compareTo(b.title, ignoreCase = true)
    }

    val (open, done) = tasks.partition { !it.done }
    return open.sortedWith(comparator) + done.sortedWith(comparator)
}

internal fun filterTasksForView(tasks: List<TodoItem>, showingWeek: Boolean): List<TodoItem> {
    val today = LocalDate.now()
    if (!showingWeek) {
        return tasks.filter { parseFlexibleDate(it.dueDate) == today }
    }

    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd = weekStart.plusDays(6)
    return tasks.filter { task ->
        val due = parseFlexibleDate(task.dueDate) ?: return@filter false
        !due.isBefore(weekStart) && !due.isAfter(weekEnd)
    }
}

