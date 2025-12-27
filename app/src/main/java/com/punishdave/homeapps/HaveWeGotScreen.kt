package com.punishdave.homeapps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardActions
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val HwgBg = Color(0xFF1C1C1C)
private val HwgPanel = Color(0xFF0F0F0F)
private val HwgAccent = Color(0xFFB00020)

@Composable
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

    Surface(modifier = Modifier.fillMaxSize(), color = HwgBg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color(0xFFBDBDBD)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Have We Got",
                        color = HwgAccent,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                IconButton(onClick = { vm.refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
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

            FilterRow(
                selectedType = type,
                onTypeSelected = vm::setType,
                selectedStatus = status,
                onStatusSelected = vm::setStatus,
                availableStatuses = summary?.by_status ?: emptyMap(),
                search = search,
                onSearchChange = vm::setSearch,
                onApplySearch = { vm.refresh() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ItemList(items = items)
        }
    }
}

@Composable
private fun SummarySection(summary: HaveWeGotSummary) {
    val films = summary.by_type["film"] ?: 0
    val tv = summary.by_type["tvshow"] ?: 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(label = "Total", value = summary.total)
        StatCard(label = "Films", value = films)
        StatCard(label = "TV Shows", value = tv)
    }

    if (summary.by_status.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Status",
            color = Color(0xFFEEEEEE),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                AssistChip(
                    onClick = { /* no-op display only */ },
                    label = { Text("All: ${summary.total}") },
                    leadingIcon = null
                )
            }
            items(summary.by_status.entries.toList()) { entry ->
                AssistChip(
                    onClick = { /* display only */ },
                    label = { Text("${entry.key}: ${entry.value}") }
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        color = HwgPanel,
        tonalElevation = 3.dp,
        border = BorderStroke(1.dp, HwgAccent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, color = Color(0xFFBDBDBD), fontWeight = FontWeight.Medium)
            Text(
                text = value.toString(),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FilterRow(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    availableStatuses: Map<String, Int>,
    search: String,
    onSearchChange: (String) -> Unit,
    onApplySearch: () -> Unit
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
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HwgAccent.copy(alpha = 0.4f),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (availableStatuses.isNotEmpty()) {
            Text(
                text = "Filter by status",
                color = Color(0xFFDDDDDD),
                fontWeight = FontWeight.SemiBold
            )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = search,
                onValueChange = onSearchChange,
                singleLine = true,
                placeholder = { Text("Search by name") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                    onSearch = { onApplySearch() }
                )
            )

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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            ItemCard(item)
        }
    }
}

@Composable
private fun ItemCard(item: HaveWeGotItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = HwgPanel,
        border = BorderStroke(1.dp, HwgAccent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(item.item_type.uppercase()) })
                AssistChip(onClick = {}, label = { Text(item.status) })
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
