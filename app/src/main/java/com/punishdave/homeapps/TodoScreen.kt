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
import kotlin.math.abs

private val TodoBg = Color(0xFF1C1C1C)
private val TodoPanel = Color(0xFF0F0F0F)
private val TodoAccent = Color(0xFFB00020)

private enum class TodoTab { Tasks, Settings }

@Composable
fun TodoScreen(
    onBack: () -> Unit
) {
    val vm: TodoViewModel = viewModel()
    val tasks by vm.tasks.collectAsState()
    val orderedTasks = remember(tasks) { orderTasksByDue(tasks) }
    val newTaskText by vm.newTaskText.collectAsState()
    val accessKey by vm.accessKey.collectAsState()
    val category by vm.category.collectAsState()
    val habit by vm.habit.collectAsState()
    val categoryOptions by vm.categoryOptions.collectAsState()
    val habitOptions by vm.habitOptions.collectAsState()
    val dueDate by vm.dueDateText.collectAsState()
    val isSyncing by vm.isSyncing.collectAsState()
    val lastErr by vm.lastError.collectAsState()
    val lastSync by vm.lastSyncStatus.collectAsState()
    var currentTab by rememberSaveable { mutableStateOf(TodoTab.Tasks) }
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
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFBDBDBD))
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionCard(
                    title = "Tasks",
                    subtitle = "Add & check off items",
                    icon = { Icon(Icons.Filled.Checklist, contentDescription = null) },
                    selected = currentTab == TodoTab.Tasks,
                    onClick = { currentTab = TodoTab.Tasks },
                    modifier = Modifier.weight(1f)
                )
                SectionCard(
                    title = "Settings",
                    subtitle = "Access key, sync, categories",
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    selected = currentTab == TodoTab.Settings,
                    onClick = { currentTab = TodoTab.Settings },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (currentTab) {
                TodoTab.Tasks -> TasksSection(
                    tasks = orderedTasks,
                    newTaskText = newTaskText,
                    dueDateText = dueDate,
                    listState = listState,
                    onChangeNewTask = { vm.newTaskText.value = it },
                    onChangeDueDate = { vm.dueDateText.value = it },
                    onPickDueDate = showDatePicker,
                    onAdd = { vm.addTask() },
                    onToggle = { vm.toggleTask(it) },
                    onDelete = { vm.deleteTask(it) }
                )

                TodoTab.Settings -> SettingsSection(
                    accessKey = accessKey,
                    category = category,
                    habit = habit,
                    categoryOptions = categoryOptions,
                    habitOptions = habitOptions,
                    isSyncing = isSyncing,
                    lastErr = lastErr,
                    lastSync = lastSync,
                    onAccessKeyChange = { vm.saveAccessKey(it) },
                    onCategoryChange = { vm.saveCategory(it) },
                    onHabitChange = { vm.saveHabit(it) },
                    onSync = { vm.syncFromApi() }
                )
            }

            if (currentTab == TodoTab.Tasks && lastErr != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = lastErr ?: "",
                    color = Color(0xFFFF9B9B),
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
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
                Text(
                    text = "Add a task",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = newTaskText,
                        onValueChange = onChangeNewTask,
                        singleLine = true,
                        label = { Text("Task name") }
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPickDueDate() },
                        value = dueDateText,
                        onValueChange = onChangeDueDate,
                        singleLine = true,
                        readOnly = true,
                        label = { Text("Due date") },
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tasks yet. Add your first one above.",
                    color = Color(0xFFB0B0B0),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(
                    items = tasks,
                    key = { it.id }
                ) { task ->
                    TodoRow(
                        task = task,
                        onToggle = { onToggle(task.id) },
                        onDelete = { onDelete(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    accessKey: String,
    category: String,
    habit: String,
    categoryOptions: List<String>,
    habitOptions: List<String>,
    isSyncing: Boolean,
    lastErr: String?,
    lastSync: String?,
    onAccessKeyChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onHabitChange: (String) -> Unit,
    onSync: () -> Unit
) {
    var accessKeyInput by rememberSaveable { mutableStateOf(accessKey) }
    var categoryInput by rememberSaveable { mutableStateOf(category) }
    var habitInput by rememberSaveable { mutableStateOf(habit) }

    LaunchedEffect(accessKey) { accessKeyInput = accessKey }
    LaunchedEffect(category) { categoryInput = category }
    LaunchedEffect(habit) { habitInput = habit }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        color = TodoPanel,
        border = BorderStroke(1.dp, TodoAccent.copy(alpha = 0.4f)),
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
            DropdownOrTextField(
                label = "Category (optional)",
                value = categoryInput,
                options = categoryOptions,
                leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) },
                onValueChange = {
                    categoryInput = it
                    onCategoryChange(it)
                }
            )
            DropdownOrTextField(
                label = "Habit (optional)",
                value = habitInput,
                options = habitOptions,
                leadingIcon = { Icon(Icons.Filled.EmojiPeople, contentDescription = null) },
                onValueChange = {
                    habitInput = it
                    onHabitChange(it)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledIconButton(
                    onClick = onSync,
                    enabled = !isSyncing,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TodoAccent,
                        contentColor = Color.White
                    )
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Sync, contentDescription = "Sync")
                    }
                }
            }

            if (lastErr != null) {
                Text(
                    text = lastErr,
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
private fun DropdownOrTextField(
    label: String,
    value: String,
    options: List<String>,
    leadingIcon: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    if (options.isEmpty()) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            label = { Text(label) },
            leadingIcon = leadingIcon
        )
        return
    }

    var expanded by rememberSaveable { mutableStateOf(false) }
    val normalized = remember(options, value) {
        buildList {
            add("")
            if (value.isNotBlank()) add(value)
            options.forEach { opt ->
                if (opt.isNotBlank() && !contains(opt)) add(opt)
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = { expanded = !expanded },
                    indication = LocalIndication.current,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = options.isNotEmpty(),
            singleLine = true,
            label = { Text(label) },
            leadingIcon = leadingIcon,
            trailingIcon = {
                IconButton(
                    onClick = { expanded = !expanded },
                    enabled = options.isNotEmpty(),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color(0xFFBDBDBD))
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            normalized.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option.isBlank()) "None" else option) },
                    onClick = {
                        expanded = false
                        onValueChange(option)
                    }
                )
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
    val rowAlpha = if (task.done) 0.45f else 1f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = TodoPanel,
        border = BorderStroke(1.dp, TodoAccent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .alpha(rowAlpha)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    role = Role.Checkbox,
                    onClick = onToggle
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggle() }
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
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
                if (task.done) {
                    Text(
                        text = "Completed",
                        color = TodoAccent,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = Color(0xFFFF9B9B))
            }
        }
    }
}

private fun shortDate(raw: String): String {
    return friendlyDateLabel(raw) ?: raw.trim()
}

private fun showDueDatePicker(context: android.content.Context, current: String, onSelected: (String) -> Unit) {
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

private fun orderTasksByDue(tasks: List<TodoItem>): List<TodoItem> {
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
