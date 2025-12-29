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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val TodoBg = Color(0xFF1C1C1C)
private val TodoPanel = Color(0xFF0F0F0F)
private val TodoAccent = Color(0xFFB00020)

@Composable
fun TodoScreen(
    onBack: () -> Unit
)
{
    val vm: TodoViewModel = viewModel()
    val tasks by vm.tasks.collectAsState()
    val newText by vm.newTaskText.collectAsState()
    val accessKey by vm.accessKey.collectAsState()
    val category by vm.category.collectAsState()
    val habit by vm.habit.collectAsState()
    val lastErr by vm.lastError.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = TodoBg) {
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
                    text = "To-Do",
                    color = TodoAccent,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = TodoPanel,
                border = BorderStroke(1.dp, TodoAccent.copy(alpha = 0.4f)),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = newText,
                        onValueChange = { vm.newTaskText.value = it },
                        singleLine = true,
                        placeholder = { Text("Add a task") }
                    )

                    FilledIconButton(
                        onClick = { vm.addTask() },
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = TodoAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add task")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = TodoPanel,
                border = BorderStroke(1.dp, TodoAccent.copy(alpha = 0.4f)),
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
                        value = accessKey,
                        onValueChange = { vm.saveAccessKey(it) },
                        singleLine = true,
                        placeholder = { Text("Enter access key to sync with server") },
                        label = { Text("Access Key") }
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = category,
                        onValueChange = { vm.saveCategory(it) },
                        singleLine = true,
                        placeholder = { Text("Category (must match WP To-Do > Categories)") },
                        label = { Text("Category") },
                        leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) }
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = habit,
                        onValueChange = { vm.saveHabit(it) },
                        singleLine = true,
                        placeholder = { Text("Habit (optional, must match WP To-Do > Habits)") },
                        label = { Text("Habit") },
                        leadingIcon = { Icon(Icons.Filled.EmojiPeople, contentDescription = null) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledIconButton(
                            onClick = { vm.syncFromApi() },
                            colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                containerColor = TodoAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = "Sync")
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

            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks yet. Add something to get started.",
                    color = Color(0xFFB0B0B0),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = tasks,
                        key = { it.id }
                    ) { task ->
                        TodoRow(
                            task = task,
                            onToggle = { vm.toggleTask(task.id) },
                            onDelete = { vm.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoRow(
    task: TodoItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = TodoPanel,
        border = BorderStroke(1.dp, TodoAccent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Make the whole row tappable; checkbox remains the primary affordance.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Checkbox, onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(checked = task.done, onCheckedChange = { onToggle() })

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.title,
                        color = Color.White,
                        fontWeight = if (task.done) FontWeight.Medium else FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (task.done) {
                        Text(
                            text = "Completed",
                            color = Color(0xFF7DD37D),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = Color(0xFFFF9B9B))
            }
        }
    }
}
