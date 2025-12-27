package com.punishdave.homeapps

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import retrofit2.HttpException

class MealPlannerRepository(
    private val store: MealPlannerStore,
    private val api: MealPlannerApi = Network.api
) {
    fun recipesFlow() = store.recipesFlow
    fun currentWeekFlow() = store.currentWeekFlow
    fun plannedWeekFlow() = store.plannedWeekFlow

    // Match your UI week starting Saturday (change to MONDAY if needed)
    fun computeWeekStartIso(today: LocalDate = LocalDate.now()): String {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        return start.toString() // YYYY-MM-DD
    }

    suspend fun sync() {
        // Always try recipes first
        val recipes = api.getRecipes()
        store.saveRecipes(recipes)

        // Then try current week, but 404 is "no data" not "sync failed"
        val weekStart = computeWeekStartIso()
        try {
            val week = api.getWeek(weekStart)
            store.saveCurrentWeek(week)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                // No week exists for this start date yet
                // Store nothing (or an empty week if you prefer)
                // If you want explicit "empty", tell me and I’ll show that approach.
                // For now we just "leave current week as null/unchanged".
            } else {
                throw e
            }
        }
    }

    suspend fun generateRandomWeek() {
        val week = api.getRandomWeek()
        store.savePlannedWeek(week)
    }

    suspend fun savePlannedWeekToServer(week: WeekResponse) {
        // Convert [Recipe, Recipe, ...] to {"0": Recipe, "1": Recipe, ...}
        val mealsMap: Map<String, Recipe> =
            week.meals.mapIndexed { index, recipe -> index.toString() to recipe }.toMap()

        val body = WeekPostRequest(
            week_start = week.week_start,
            meals = mealsMap
        )

        val saved = api.postWeek(body)

        // optional: store as current week
        store.saveCurrentWeek(saved)
    }

}
