package com.punishdave.homeapps

class HaveWeGotRepository(
    private val api: HaveWeGotApi = Network.haveWeGotApi
) {
    suspend fun fetchSummary(): HaveWeGotSummary {
        val raw = api.getSummary()
        val total = raw["total"].asInt()

        val byType = normalizeCounts(raw["by_type"], primaryKey = "item_type", secondaryKey = "status")
        val byStatus = normalizeCounts(raw["by_status"], primaryKey = "status", secondaryKey = "item_type")

        return HaveWeGotSummary(
            total = total,
            by_type = byType,
            by_status = byStatus
        )
    }

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

    private fun normalizeCounts(
        value: Any?,
        primaryKey: String,
        secondaryKey: String
    ): Map<String, Int> {
        return when (value) {
            is Map<*, *> -> {
                value.mapNotNull { (k, v) ->
                    val key = k as? String
                    val total = v.asInt()
                    key?.let { it to total }
                }.toMap()
            }
            is List<*> -> {
                value.mapNotNull { entry ->
                    if (entry is Map<*, *>) {
                        val key = (entry[primaryKey] as? String)
                            ?: (entry[secondaryKey] as? String)
                        val total = entry["total"].asInt()
                        key?.let { it to total }
                    } else {
                        null
                    }
                }.toMap()
            }
            else -> emptyMap()
        }
    }

    private fun Any?.asInt(): Int {
        return when (this) {
            is Number -> this.toInt()
            is String -> this.toIntOrNull() ?: 0
            else -> 0
        }
    }
}

data class HaveWeGotSummary(
    val total: Int = 0,
    val by_type: Map<String, Int> = emptyMap(),
    val by_status: Map<String, Int> = emptyMap()
)
