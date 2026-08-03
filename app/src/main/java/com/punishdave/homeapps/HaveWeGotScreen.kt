@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private val HwgBg = Color(0xFF1C1C1C)
private val HwgPanel = Color(0xFF242424)
private val HwgAccent = Color(0xFFE66A64)

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun HaveWeGotScreen(
    onBack: () -> Unit
) {
    val vm: HaveWeGotViewModel = viewModel()
    val summary by vm.summary.collectAsState()
    val items by vm.items.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val type by vm.typeFilter.collectAsState()
    val status by vm.statusFilter.collectAsState()
    val search by vm.search.collectAsState()
    var filtersExpanded by remember { mutableStateOf(true) }
    val pullRefreshState = rememberPullRefreshState(isLoading, onRefresh = { vm.refresh() })

    Surface(modifier = Modifier.fillMaxSize(), color = HwgBg) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color(0xFFBDBDBD))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Have We Got",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = HwgAccent
                )
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error ?: "",
                    color = Color(0xFFFFB3B3),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            summary?.let { SummarySection(it) }

            Spacer(modifier = Modifier.height(12.dp))

            FiltersCard(
                expanded = filtersExpanded,
                onToggle = { filtersExpanded = !filtersExpanded },
                selectedType = type,
                onTypeSelected = vm::setType,
                selectedStatus = status,
                onStatusSelected = vm::setStatus,
                availableStatuses = summary?.by_status ?: emptyMap(),
                search = search,
                onSearchChange = vm::setSearch,
                onApplySearch = { vm.refresh() },
                onSwipeHide = { filtersExpanded = false },
                onSwipeShow = { filtersExpanded = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ItemList(items = items)
            }
            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
                backgroundColor = HwgPanel,
                contentColor = HwgAccent
            )
        }
    }
}

@Composable
private fun SummarySection(summary: HaveWeGotSummary) {
    val films = summary.by_type["film"] ?: 0
    val tv = summary.by_type["tvshow"] ?: 0

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { StatCard(label = "Total", value = summary.total, modifier = Modifier.width(112.dp)) }
        item { StatCard(label = "Films", value = films, modifier = Modifier.width(112.dp)) }
        item { StatCard(label = "TV Shows", value = tv, modifier = Modifier.width(112.dp)) }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = HwgPanel,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, color = Color(0xFFBDBDBD), fontWeight = FontWeight.Medium)
            Text(
                text = value.toString(),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FiltersCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    onSwipeHide: () -> Unit,
    onSwipeShow: () -> Unit,
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    availableStatuses: Map<String, Int>,
    search: String,
    onSearchChange: (String) -> Unit,
    onApplySearch: () -> Unit
) {
    var totalDrag = 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HwgPanel,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
        onClick = onToggle
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .pointerInput(expanded) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        },
                        onDragEnd = {
                            if (totalDrag < -60f) onSwipeHide()
                            if (totalDrag > 60f) onSwipeShow()
                            totalDrag = 0f
                        },
                        onDragCancel = { totalDrag = 0f }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Filters (swipe up to hide)" else "Filters (swipe down to show)",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (expanded) "Hide" else "Show",
                    color = HwgAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val options = listOf(
                            "all" to "All",
                            "film" to "Films",
                            "tvshow" to "TV"
                        )

                        options.forEach { (id, label) ->
                            FilterChip(
                                selected = selectedType == id,
                                onClick = { onTypeSelected(id) },
                                label = { Text(label) },
                                leadingIcon = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HwgAccent.copy(alpha = 0.4f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (availableStatuses.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedStatus.isEmpty(),
                                    onClick = { onStatusSelected("") },
                                    label = { Text("All") },
                                    leadingIcon = null
                                )
                            }
                            items(availableStatuses.entries.toList()) { entry ->
                                FilterChip(
                                    selected = selectedStatus.equals(entry.key, ignoreCase = true),
                                    onClick = { onStatusSelected(entry.key) },
                                    label = { Text("${entry.key} (${entry.value})") },
                                    leadingIcon = null
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = search,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            placeholder = { Text("Search by name") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { onApplySearch() }
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FilledTonalButton(
                                onClick = onApplySearch,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = HwgAccent.copy(alpha = 0.65f),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Apply")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemList(items: List<HaveWeGotItem>) {
    if (items.isEmpty()) {
        Text(
            text = "No items found.",
            color = Color(0xFFB0B0B0),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(items) { item ->
            ItemCard(item)
            androidx.compose.material3.Divider(
                modifier = Modifier.padding(start = 12.dp),
                color = Color(0xFF363636)
            )
        }
    }
}

@Composable
private fun ItemCard(item: HaveWeGotItem) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = item.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.item_type.uppercase(), color = HwgAccent, style = MaterialTheme.typography.labelSmall)
                Text("•", color = Color(0xFF666666), style = MaterialTheme.typography.labelSmall)
                Text(item.status, color = Color(0xFFAAAAAA), style = MaterialTheme.typography.labelSmall)
            }

            val lastAccess = item.last_access ?: "Unknown"
            Text(
                text = "Last accessed: $lastAccess",
                color = Color(0xFFBDBDBD),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start
            )
        }
    }
}
