package com.punishdave.homeapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HaveWeGotViewModel(
    private val repo: HaveWeGotRepository = HaveWeGotRepository()
) : ViewModel() {

    private val _summary = MutableStateFlow<HaveWeGotSummary?>(null)
    val summary: StateFlow<HaveWeGotSummary?> = _summary

    private val _items = MutableStateFlow<List<HaveWeGotItem>>(emptyList())
    val items: StateFlow<List<HaveWeGotItem>> = _items

    val typeFilter = MutableStateFlow("all")
    val statusFilter = MutableStateFlow("")
    val search = MutableStateFlow("")

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        isLoading.value = true
        error.value = null
        try {
            val type = typeFilter.value
            val status = statusFilter.value
            val searchText = search.value

            val itemsResponse = repo.fetchItems(
                type = type,
                status = status,
                search = searchText
            )

            val summaryResponse = repo.fetchSummary()
            val computed = computeCounts(itemsResponse)

            val mergedSummary = HaveWeGotSummary(
                total = if (summaryResponse.total > 0) summaryResponse.total else computed.total,
                by_type = if (summaryResponse.by_type.isNotEmpty()) summaryResponse.by_type else computed.by_type,
                by_status = if (summaryResponse.by_status.isNotEmpty()) summaryResponse.by_status else computed.by_status
            )

            _summary.value = mergedSummary
            _items.value = itemsResponse
        } catch (e: Exception) {
            error.value = e.message ?: "Failed to load data"
        } finally {
            isLoading.value = false
        }
    }

    fun setType(type: String) {
        typeFilter.value = type
        refresh()
    }

    fun setStatus(status: String) {
        statusFilter.value = status
        refresh()
    }

    fun setSearch(text: String) {
        search.value = text
    }

    private fun computeCounts(items: List<HaveWeGotItem>): HaveWeGotSummary {
        val byType = mutableMapOf<String, Int>()
        val byStatus = mutableMapOf<String, Int>()

        items.forEach { item ->
            val typeKey = item.item_type.lowercase()
            val statusKey = item.status

            byType[typeKey] = (byType[typeKey] ?: 0) + 1
            byStatus[statusKey] = (byStatus[statusKey] ?: 0) + 1
        }

        return HaveWeGotSummary(
            total = items.size,
            by_type = byType,
            by_status = byStatus
        )
    }
}
