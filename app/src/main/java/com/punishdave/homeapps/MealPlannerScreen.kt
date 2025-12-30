package com.punishdave.homeapps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import androidx.compose.foundation.layout.statusBarsPadding

private val Bg = Color(0xFF2A2A2A)
private val Accent = Color(0xFFB00020)
private val PanelBg = Color(0xFF0F0F0F)

private fun startOfWeekSaturday(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))

/* -----------------------
   Screen 1: Menu
------------------------ */

@Composable
fun MealPlannerMenuScreen(
    onBack: () -> Unit,
    onOpenCurrentWeek: () -> Unit,
    onOpenPlanWeek: () -> Unit,
    onSync: () -> Unit // kept for compatibility; we call VM directly anyway
) {
    val vm: MealPlannerViewModel = viewModel()
    val syncing by vm.isSyncing.collectAsState()
    val lastErr by vm.lastError.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .padding(top = 16.dp)
        ) {
        Text(
                text = "Meal Planner",
                color = Accent,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        title = "Current Week",
                        subtitle = "View this week's meals and shopping list.",
                        onClick = onOpenCurrentWeek,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp)
                    )

                    ActionCard(
                        title = "Plan a week",
                        subtitle = "Pick meals for a week and save it.",
                        onClick = onOpenPlanWeek,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp)
                    )
                }
            }

            // Status / error line (small + unobtrusive)
            if (syncing) {
                Text(
                    text = "Syncing…",
                    color = Color(0xFFBDBDBD),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            } else if (lastErr != null) {
                Text(
                    text = "Sync error: ${lastErr}",
                    color = Color(0xFFFF8080),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BottomBarBackSync(
                onBack = onBack,
                onSync = { vm.sync() } // <— real wiring
            )
        }
    }
}

@Composable
private fun BottomBarBackSync(onBack: () -> Unit, onSync: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back")
        }

        TextButton(onClick = onSync) {
            Text("Sync")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.Sync, contentDescription = null)
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 92.dp),
        shape = RoundedCornerShape(14.dp),
        color = PanelBg,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.55f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                color = Color(0xFFB0B0B0),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* -----------------------
   Screen 2: Current Week
------------------------ */

@Composable
fun MealPlannerCurrentWeekScreen(
    onBack: () -> Unit
) {
    val vm: MealPlannerViewModel = viewModel()
    val currentWeek = vm.currentWeek.collectAsState().value

    val start = startOfWeekSaturday(LocalDate.now())
    val days = remember(start) { (0..6).map { start.plusDays(it.toLong()) } }

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .padding(top = 16.dp)
        ) {
            TopTitleWithBack(title = "Current Week", onBack = onBack)

            Spacer(modifier = Modifier.height(10.dp))

            if (currentWeek == null) {
                Text(
                    text = "No current week loaded yet. Go back and press Sync.",
                    color = Color(0xFFBDBDBD),
                    fontSize = 14.sp
                )
                return@Column
            }

            DayGrid(
                days = days,
                cardContent = { date, index ->
                    val meal = currentWeek.meals.getOrNull(index)
                    CurrentWeekDayCard(
                        date = date,
                        mealTitle = meal?.title
                    )
                }
            )
        }
    }
}

@Composable
private fun CurrentWeekDayCard(
    date: LocalDate,
    mealTitle: String?
) {
    val headerFmt = DateTimeFormatter.ofPattern("EEE d MMM")

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFEFEFEF),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.45f))
    ) {
        Column {
            Surface(color = Color(0xFF2F2F2F)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.format(headerFmt),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mealTitle ?: "—",
                        color = Color(0xFF111111),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/* -----------------------
   Screen 3: Plan a week
------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerPlanWeekScreen(
    onBack: () -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit
) {
    val vm: MealPlannerViewModel = viewModel()
    val recipes = vm.recipes.collectAsState().value
    val plannedWeek = vm.plannedWeek.collectAsState().value

    val start = startOfWeekSaturday(LocalDate.now()).plusDays(7)
    val days = remember(start) { (0..6).map { start.plusDays(it.toLong()) } }

    // Dropdown options come from recipes (plus a default)
    val options: List<Recipe> = remember(recipes) {
        listOf(Recipe(id = -1, title = "Select a meal…", ingredients = emptyList())) + recipes
    }

    // Local selection state: dayIndex -> recipeId
    val selectedByDay = remember(plannedWeek, options) {
        mutableStateMapOf<Int, Int>().apply {
            // If a random week exists, seed selections from it
            plannedWeek?.meals?.forEachIndexed { idx, recipe ->
                put(idx, recipe.id)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .padding(top = 16.dp)
        ) {
            TopTitleWithBack(title = "Plan a week", onBack = onBack)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalButton(
                    onClick = { vm.generateWeek() }, // hits /random-week and stores planned week
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Accent.copy(alpha = 0.55f),
                        contentColor = Color.White
                    )
                ) { Text("Generate Week") }

                FilledTonalButton(
                    onClick = {
                        // Build WeekResponse from current selections and save locally; sync will push to API
                        val meals = mutableListOf<Recipe>()
                        for (i in 0..6) {
                            val rid = selectedByDay[i] ?: -1
                            val recipe = options.firstOrNull { it.id == rid }?.takeIf { it.id != -1 }
                            if (recipe != null) {
                                meals.add(recipe)
                            } else {
                                vm.lastError.value = "Pick a meal for all 7 days before saving."
                                return@FilledTonalButton
                            }
                        }

                        val week = WeekResponse(
                            week_start = start.toString(),
                            meals = meals
                        )
                        vm.savePlannedWeekLocal(week)
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Accent.copy(alpha = 0.55f),
                        contentColor = Color.White
                    )
                ) { Text("Save Week") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recipes.isEmpty()) {
                Text(
                    text = "No recipes loaded yet. Press Sync on the menu screen first.",
                    color = Color(0xFFBDBDBD),
                    fontSize = 14.sp
                )
                return@Column
            }

            DayGrid(
                days = days,
                cardContent = { date, index ->
                    PlanWeekDayCard(
                        date = date,
                        options = options,
                        selectedRecipeId = selectedByDay[index] ?: -1,
                        onSelected = { rid -> selectedByDay[index] = rid }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanWeekDayCard(
    date: LocalDate,
    options: List<Recipe>,
    selectedRecipeId: Int,
    onSelected: (Int) -> Unit
) {
    val headerFmt = DateTimeFormatter.ofPattern("EEE d MMM")
    val headerBg = Color(0xFF3A0000)
    val bodyBg = Color(0xFF333333)

    var expanded by remember { mutableStateOf(false) }
    val selectedTitle = options.firstOrNull { it.id == selectedRecipeId }?.title ?: options.first().title

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bodyBg,
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.45f))
    ) {
        Column {
            Surface(color = headerBg) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.format(headerFmt),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show current selection prominently inside the day cell
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        value = selectedTitle,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1A1A1A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Accent.copy(alpha = 0.8f),
                            unfocusedIndicatorColor = Accent.copy(alpha = 0.6f),
                            focusedTrailingIconColor = Color.White,
                            unfocusedTrailingIconColor = Color.White
                        ),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    onSelected(option.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = { /* Later: replace just this day */ },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Accent.copy(alpha = 0.55f),
                        contentColor = Color.White
                    )
                ) {
                    Text("Replace", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* -----------------------
   Shared helpers
------------------------ */

@Composable
private fun TopTitleWithBack(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color(0xFFBDBDBD)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Accent,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Responsive grid so the “days stack correctly”.
 * - Phones portrait: 2 columns
 * - Wider screens: 3–4 columns
 */
@Composable
private fun DayGrid(
    days: List<LocalDate>,
    cardContent: @Composable (LocalDate, Int) -> Unit
) {
    val config = LocalConfiguration.current
    val w = config.screenWidthDp

    val columns = when {
        w >= 840 -> 4
        w >= 600 -> 3
        else -> 2
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 14.dp)
    ) {
        items(days.indices.toList()) { idx ->
            cardContent(days[idx], idx)
        }
    }
}
