package com.punishdave.homeapps

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import retrofit2.HttpException

class MealPlannerRepository(
    private val store: MealPlannerStore,
    private val api: MealPlannerApi = Network.api
) {
    private val weekStartCandidates = listOf(
        DayOfWeek.SATURDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.SUNDAY
    )

    fun recipesFlow() = store.recipesFlow
    fun currentWeekFlow() = store.currentWeekFlow
    fun plannedWeekFlow() = store.plannedWeekFlow
    fun shoppingListFlow() = store.shoppingListFlow
    fun accessKeyFlow() = store.accessKeyFlow

    suspend fun saveLocalPlannedWeek(week: WeekResponse) = store.savePlannedWeek(week)
    suspend fun saveLocalShoppingList(weekStart: String, items: List<String>) =
        store.saveShoppingList(
            ShoppingListResponse(
                week_start = weekStart,
                shopping_list = items.map { it.trim() }.filter { it.isNotEmpty() }
            )
        )
    suspend fun saveAccessKey(key: String) = store.saveAccessKey(key)
    suspend fun clearPlannedWeek() = store.clearPlannedWeek()
    suspend fun clearShoppingList() = store.clearShoppingList()

    // Match your UI week starting Saturday (change to MONDAY if needed)
    fun computeWeekStartIso(today: LocalDate = LocalDate.now(), startDay: DayOfWeek = DayOfWeek.SATURDAY): String {
        val start = today.with(TemporalAdjusters.previousOrSame(startDay))
        return start.toString() // YYYY-MM-DD
    }

    suspend fun sync() {
        val ts = System.currentTimeMillis()
        // Always try recipes first
        val recipes = api.getRecipes(ts)
        store.saveRecipes(recipes)

        val today = LocalDate.now()
        val currentFetched = fetchWeekForCandidates(today, ts) { store.saveCurrentWeek(it) }
        if (!currentFetched) {
            store.clearCurrentWeek()
        }

        val plannedFetched = fetchWeekForCandidates(today.plusDays(7), ts) { store.savePlannedWeek(it) }
        if (!plannedFetched) {
            store.clearPlannedWeek()
        }
    }

    suspend fun generateRandomWeek(startDay: DayOfWeek = DayOfWeek.SATURDAY) {
        val ts = System.currentTimeMillis()
        // Backend returns a bare list of recipes; wrap it into a WeekResponse the app expects.
        val recipes = api.getRandomWeek(ts)
        if (recipes.isEmpty()) throw IllegalStateException("No recipes available to generate a week.")

        // Align with UI: plan for the upcoming week starting Saturday.
        val nextWeekStart = computeWeekStartIso(LocalDate.now().plusDays(7), startDay)

        // Ensure we always have 7 entries; repeat recipes if the API returns fewer.
        val mealsForWeek = (0 until 7).map { idx -> recipes[idx % recipes.size] }

        val week = WeekResponse(
            week_start = nextWeekStart,
            meals = mealsForWeek
        )
        store.savePlannedWeek(week)
    }

    suspend fun savePlannedWeekToServer(week: WeekResponse, key: String) {
        val authKey = key.trim().ifEmpty { throw IllegalStateException("Access key required to save week.") }
        val ts = System.currentTimeMillis()
        // Convert [Recipe, Recipe, ...] to {"0": Recipe, "1": Recipe, ...}
        val mealsMap: Map<String, Recipe> =
            week.meals.mapIndexed { index, recipe -> index.toString() to recipe }.toMap()

        val body = WeekPostRequest(
            week_start = week.week_start,
            meals = mealsMap
        )

        val saved = api.postWeek(authKey, body, ts)

        // Only mirror to current-week cache if it matches the "current" start
        val startDay = LocalDate.parse(week.week_start).dayOfWeek
        if (week.week_start == computeWeekStartIso(LocalDate.now(), startDay)) {
            store.saveCurrentWeek(saved)
        }
    }

    suspend fun fetchShoppingList(weekStart: String): ShoppingListResponse {
        val ts = System.currentTimeMillis()
        return try {
            api.getShoppingList(weekStart, ts)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                ShoppingListResponse(week_start = weekStart, shopping_list = emptyList())
            } else {
                throw e
            }
        }
    }

    suspend fun pushShoppingList(weekStart: String, items: List<String>, key: String): ShoppingListResponse {
        val authKey = key.trim().ifEmpty { throw IllegalStateException("Access key required to save shopping list.") }
        val cleanedItems = items.map { it.trim() }.filter { it.isNotEmpty() }
        val ts = System.currentTimeMillis()
        val body = ShoppingListPostRequest(
            week_start = weekStart,
            items = cleanedItems
        )
        val saved = api.postShoppingList(authKey, body, ts)
        store.saveShoppingList(saved)
        return saved
    }

    private suspend fun fetchWeekForCandidates(
        targetDate: LocalDate,
        ts: Long,
        onFound: suspend (WeekResponse) -> Unit
    ): Boolean {
        for (startDay in weekStartCandidates) {
            val weekStart = computeWeekStartIso(targetDate, startDay)
            try {
                val week = api.getWeek(weekStart, ts)
                onFound(week)
                return true
            } catch (e: HttpException) {
                if (e.code() == 404) continue else throw e
            }
        }
        return false
    }
}
