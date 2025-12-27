package com.punishdave.homeapps

class HaveWeGotRepository(
    private val api: HaveWeGotApi = Network.haveWeGotApi
) {
    suspend fun fetchSummary(): HaveWeGotSummary = api.getSummary()

    suspend fun fetchItems(
        type: String = "all",
        status: String = "",
        search: String = "",
        order: String = "last_access_desc",
        limit: Int = 400
    ): List<HaveWeGotItem> {
        val typeParam = type.lowercase().takeIf { it != "all" }
        val statusParam = status.trim().ifBlank { null }
        val searchParam = search.trim().ifBlank { null }

        return api.listItems(
            type = typeParam,
            status = statusParam,
            search = searchParam,
            order = order,
            limit = limit
        )
    }
}
