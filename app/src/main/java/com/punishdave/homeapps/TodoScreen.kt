@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.punishdave.homeapps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.weight
import android.app.DatePickerDialog
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

internal val TodoBg = Color(0xFF1C1C1C)
internal val TodoPanel = Color(0xFF0F0F0F)
internal val TodoAccent = Color(0xFFE66A64)

@Composable
fun TodoScreen(
    onBack: () -> Unit
) {
    val vm: TodoViewModel = viewModel()
    val tasks by vm.tasks.collectAsState()
    var showingWeek by rememberSaveable { mutableStateOf(false) }
    val visibleTasks = remember(tasks, showingWeek) {
        orderTasksByDue(filterTasksForView(tasks, showingWeek))
    }
    val weekTaskCount = remember(tasks) { filterTasksForView(tasks, true).size }
    val newTaskText by vm.newTaskText.collectAsState()
    val dueDate by vm.dueDateText.collectAsState()
    val isSyncing by vm.isSyncing.collectAsState()
    val lastErr by vm.lastError.collectAsState()
    val lastSync by vm.lastSyncStatus.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val showDatePicker = remember(context, dueDate) {
        {
            showDueDatePicker(context, dueDate) { picked ->
                vm.dueDateText.value = picked
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TodoBg) {
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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFBDBDBD))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (showingWeek) "This week" else "Today",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            val statusText = lastErr ?: lastSync
            if (!statusText.isNullOrBlank()) {
                Text(
                    text = statusText,
                    color = if (lastErr != null) Color(0xFFFF9B9B) else Color(0xFF8D8D8D),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            TasksSection(
                tasks = visibleTasks,
                newTaskText = newTaskText,
                dueDateText = dueDate,
                listState = listState,
                onChangeNewTask = { vm.newTaskText.value = it },
                onChangeDueDate = { vm.dueDateText.value = it },
                onPickDueDate = showDatePicker,
                onAdd = { vm.addTask() },
                onToggle = { vm.toggleTask(it) },
                onDelete = { vm.deleteTask(it) },
                isSyncing = isSyncing,
                onSync = { vm.syncFromApi() },
                showingWeek = showingWeek,
                weekTaskCount = weekTaskCount,
                onToggleView = { showingWeek = !showingWeek }
            )

        }
    }
}
@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) TodoAccent else TodoAccent.copy(alpha = 0.4f)
    val bg = if (selected) TodoPanel else Color(0xFF121212)

    Surface(
        modifier = modifier.heightIn(min = 86.dp),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, borderColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                icon()
            }
            Column(modifier = Modifier.weight(1f)) {
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun TasksSection(
    tasks: List<TodoItem>,
    newTaskText: String,
    dueDateText: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onChangeNewTask: (String) -> Unit,
    onChangeDueDate: (String) -> Unit,
    onPickDueDate: () -> Unit,
    onAdd: () -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    isSyncing: Boolean,
    onSync: () -> Unit,
    showingWeek: Boolean,
    weekTaskCount: Int,
    onToggleView: () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(isSyncing, onSync)
    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = newTaskText,
                        onValueChange = onChangeNewTask,
                        singleLine = true,
                        placeholder = { Text("Add task", color = Color(0xFF777777)) },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, tint = TodoAccent) }
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .width(132.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPickDueDate() },
                        value = dueDateText,
                        onValueChange = onChangeDueDate,
                        singleLine = true,
                        readOnly = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        trailingIcon = {
                            IconButton(onClick = onPickDueDate) {
                                Icon(Icons.Filled.DateRange, contentDescription = "Pick due date")
                            }
                        }
                    )
                    FilledIconButton(
                        onClick = onAdd,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = TodoAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add task")
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            androidx.compose.material3.Divider(color = Color(0xFF3A3A3A))

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        if (showingWeek) "No tasks due this week" else "No tasks due today",
                        color = Color(0xFF777777),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(items = tasks) { task ->
                        TodoRow(
                            task = task,
                            onToggle = { onToggle(task.id) },
                            onDelete = { onDelete(task.id) }
                        )
                        androidx.compose.material3.Divider(
                            modifier = Modifier.padding(start = 48.dp),
                            color = Color(0xFF333333)
                        )
                    }
                }
            }
            androidx.compose.material3.TextButton(
                onClick = onToggleView,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    if (showingWeek) "Show tasks due today"
                    else "View tasks due this week ($weekTaskCount)",
                    color = TodoAccent
                )
            }
        }
        PullRefreshIndicator(
            refreshing = isSyncing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = TodoPanel,
            contentColor = TodoAccent
        )
    }
}
