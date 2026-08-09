package com.punishdave.homeapps

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLogicTest {
    @Test
    fun mostUsedSectionsComeFirstAndTiesKeepTheirOriginalOrder() {
        val ids = listOf("meal", "todo", "workout", "sophon")
        val counts = mapOf("todo" to 8, "sophon" to 3, "meal" to 1, "workout" to 1)

        assertEquals(listOf("todo", "sophon", "meal", "workout"), rankHomeSectionIds(ids, counts))
    }

    @Test
    fun unusedSectionsRetainTheirDesignedOrder() {
        val ids = listOf("meal", "todo", "workout")

        assertEquals(ids, rankHomeSectionIds(ids, emptyMap()))
    }
}
