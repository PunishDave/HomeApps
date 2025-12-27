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
    suspend fun saveLocalPlannedWeek(week: WeekResponse) = store.savePlannedWeek(week)
    suspend fun clearPlannedWeek() = store.clearPlannedWeek()

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
        // Backend returns a bare list of recipes; wrap it into a WeekResponse the app expects.
        val recipes = api.getRandomWeek()
        if (recipes.isEmpty()) throw IllegalStateException("No recipes available to generate a week.")

        // Align with UI: plan for the upcoming week starting Saturday.
        val nextWeekStart = computeWeekStartIso(LocalDate.now().plusDays(7))

        // Ensure we always have 7 entries; repeat recipes if the API returns fewer.
        val mealsForWeek = (0 until 7).map { idx -> recipes[idx % recipes.size] }

        val week = WeekResponse(
            week_start = nextWeekStart,
            meals = mealsForWeek
        )
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

        // Only mirror to current-week cache if it matches the "current" start
        if (week.week_start == computeWeekStartIso()) {
            store.saveCurrentWeek(saved)
        }
    }

}
