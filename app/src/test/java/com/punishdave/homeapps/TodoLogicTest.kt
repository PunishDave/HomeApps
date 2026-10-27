package com.punishdave.homeapps

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoLogicTest {
    @Test
	fun todayViewCarriesForwardOpenOverdueAndUndatedTasks() {
        val today = LocalDate.now()
        val tasks = listOf(
			TodoItem(id = "0", title = "Overdue", dueDate = today.minusDays(2).toString()),
			TodoItem(id = "00", title = "Completed overdue", done = true, dueDate = today.minusDays(1).toString()),
            TodoItem(id = "1", title = "Today ISO", dueDate = today.toString()),
            TodoItem(id = "2", title = "Today UK", dueDate = today.toDisplayDate()),
			TodoItem(id = "3", title = "Tomorrow", dueDate = today.plusDays(1).toString()),
			TodoItem(id = "4", title = "Undated")
        )

		assertEquals(listOf("Overdue", "Today ISO", "Today UK", "Undated"), filterTasksForView(tasks, false).map { it.title })
    }

    @Test
	fun weekViewUsesMondayToSundayAndCarriesForwardOpenBacklog() {
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val tasks = listOf(
            TodoItem(id = "1", title = "Monday", dueDate = monday.toString()),
            TodoItem(id = "2", title = "Sunday", dueDate = monday.plusDays(6).toString()),
            TodoItem(id = "3", title = "Next week", dueDate = monday.plusDays(7).toString()),
			TodoItem(id = "4", title = "No date"),
			TodoItem(id = "5", title = "Old open", dueDate = monday.minusDays(1).toString()),
			TodoItem(id = "6", title = "Old done", done = true, dueDate = monday.minusDays(1).toString())
        )

		assertEquals(listOf("Monday", "Sunday", "No date", "Old open"), filterTasksForView(tasks, true).map { it.title })
    }

    @Test
    fun unfinishedTasksSortBeforeCompletedTasks() {
        val today = LocalDate.now().toString()
        val sorted = orderTasksByDue(
            listOf(
                TodoItem(id = "1", title = "Done", done = true, dueDate = today),
                TodoItem(id = "2", title = "Open", dueDate = today)
            )
        )

        assertEquals("Open", sorted.first().title)
        assertTrue(sorted.last().done)
    }

	@Test
	fun overdueTasksSortOldestFirstAheadOfTodayAndUndated() {
		val today = LocalDate.now()
		val sorted = orderTasksByDue(
			listOf(
				TodoItem(id = "1", title = "Undated"),
				TodoItem(id = "2", title = "Today", dueDate = today.toString()),
				TodoItem(id = "3", title = "Yesterday", dueDate = today.minusDays(1).toString()),
				TodoItem(id = "4", title = "Oldest", dueDate = today.minusDays(5).toString())
			)
		)

		assertEquals(listOf("Oldest", "Yesterday", "Today", "Undated"), sorted.map { it.title })
	}
}
