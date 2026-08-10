package com.punishdave.homeapps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

/* -----------------------
   Screen 3: Shopping list
------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerShoppingListScreen(
    onBack: () -> Unit
) {
    val vm: MealPlannerViewModel = viewModel()
    val listState = vm.shoppingList.collectAsState().value
    val newItem by vm.newShoppingItem.collectAsState()
    val syncing by vm.isSyncing.collectAsState()
    val lastErr by vm.lastError.collectAsState()
    val syncStatus by vm.shoppingSyncStatus.collectAsState()

    val weekStart = listState?.week_start ?: vm.shoppingWeekStart()
    val weekLabel = remember(weekStart) {
        runCatching { LocalDate.parse(weekStart).toDisplayDate() }.getOrElse { weekStart }
    }
    val items = listState?.shopping_list ?: emptyList()

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TopTitleWithBack(title = "Shopping List", onBack = onBack)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = PanelBg,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.ShoppingCart,
                                contentDescription = null,
                                tint = Accent
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Week of $weekLabel",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$weekLabel • ${items.size} items",
                                    color = Color(0xFFB0B0B0),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { vm.syncShoppingList() },
                            enabled = !syncing,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Accent.copy(alpha = 0.6f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (syncing) "Syncing…" else "Sync")
                        }
                    }

                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { vm.newShoppingItem.value = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Add item") },
                    placeholder = { Text("e.g. Milk, chicken thighs") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { vm.addShoppingItem() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Accent.copy(alpha = 0.6f),
                        focusedLabelColor = Accent,
                        unfocusedLabelColor = Color(0xFFB0B0B0),
                        cursorColor = Accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = Color(0xFF8A8A8A),
                        unfocusedPlaceholderColor = Color(0xFF8A8A8A)
                    )
                )

                FilledTonalButton(
                    onClick = { vm.addShoppingItem() },
                    enabled = newItem.isNotBlank(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Accent.copy(alpha = 0.7f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add")
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = PanelBg,
                tonalElevation = 0.dp
            ) {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items yet. Add one and tap Sync to push it.",
                            color = Color(0xFFBDBDBD),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        itemsIndexed(items) { index, item ->
                            ShoppingListRow(
                                index = index,
                                text = item,
                                onRemove = { vm.removeShoppingItem(index) }
                            )
                        }
                    }
                }
            }

            if (syncing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Accent
                )
            }

            if (lastErr != null) {
                Text(
                    text = lastErr ?: "",
                    color = Color(0xFFFF8080),
                    fontSize = 13.sp
                )
            } else if (syncStatus != null) {
                Text(
                    text = syncStatus ?: "",
                    color = Color(0xFFBDBDBD),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ShoppingListRow(
    index: Int,
    text: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${index + 1}.",
            color = Accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color(0xFFFF9B9B))
        }
    }
}

