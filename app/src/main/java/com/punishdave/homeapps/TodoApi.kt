package com.punishdave.homeapps

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TodoApi {
    @GET("items")
    suspend fun listItems(
        @Header("X-PD-Todo-Key") key: String?,
        @Query("pd_todo_key") keyQuery: String? = key,
        @Query("status") status: String? = null,
        @Query("due_on") dueOn: String? = null,
        @Query("due_after") dueAfter: String? = null,
        @Query("orderby") orderBy: String? = "updated_at",
        @Query("order") order: String? = "DESC",
        @Query("per_page") perPage: Int? = 100
    ): List<TodoRemoteItem>
}

@JsonClass(generateAdapter = true)
data class TodoRemoteItem(
    val id: Int,
    val title: String,
    val description: String? = null,
    val status: String? = null,
    val category: String? = null,
    val habit: String? = null,
    val due_date: String? = null,
    val updated_at: String? = null
)
