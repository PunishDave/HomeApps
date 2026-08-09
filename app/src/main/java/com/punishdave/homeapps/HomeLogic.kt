package com.punishdave.homeapps

fun rankHomeSectionIds(ids: List<String>, counts: Map<String, Int>): List<String> =
    ids.withIndex()
        .sortedWith(compareByDescending<IndexedValue<String>> { counts[it.value] ?: 0 }.thenBy { it.index })
        .map { it.value }
