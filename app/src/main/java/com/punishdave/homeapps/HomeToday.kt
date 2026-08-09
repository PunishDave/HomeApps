package com.punishdave.homeapps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TodayOverview(
    taskCount: Int,
    meal: String?,
    workoutDone: Boolean,
    gameNight: String?,
    sophonDevices: List<SophonDevice>,
    onTodo: () -> Unit = {},
    onMeal: () -> Unit = {},
    onWorkout: () -> Unit = {},
    onGameWithDave: () -> Unit = {},
    onSophon: () -> Unit = {}
) {
    Surface(color = Color(0xFF222222), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("TODAY", color = Color(0xFFE66A64), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            TodayRow("Tasks", if (taskCount == 0) "Nothing due" else "$taskCount due", onTodo)
            TodayRow("Meal", meal ?: "Not planned", onMeal)
            TodayRow("Workout", if (workoutDone) "Logged today" else "Not logged", onWorkout)
            gameNight?.let { TodayRow("Game night", it, onGameWithDave) }
            if (sophonDevices.isNotEmpty()) SophonTodayRow(sophonDevices, onSophon)
        }
    }
}

@Composable
private fun SophonTodayRow(devices: List<SophonDevice>, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Sophon", color = Color(0xFF999999), fontSize = 12.sp, modifier = Modifier.width(82.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            devices.forEach { device ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(friendlySensorName(device.device_id), color = Color.White, fontSize = 13.sp)
                    Text(device.temperature_c?.let { "%.1f°C".format(it) } ?: "--", color = Color(0xFFE66A64), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

fun friendlySensorName(id: String): String = when (id) {
    "garden-sensor-01" -> "Garden"
    "office-temp-reader" -> "Office"
    else -> id
}

@Composable
private fun TodayRow(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFF999999), fontSize = 12.sp, modifier = Modifier.width(82.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
