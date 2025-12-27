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

            val summaryResponse = repo.fetchSummary()
            _summary.value = summaryResponse

            val itemsResponse = repo.fetchItems(
                type = type,
                status = status,
                search = searchText
            )
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
}
