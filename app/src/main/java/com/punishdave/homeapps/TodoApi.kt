package com.punishdave.homeapps

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TodoApi {
    @GET("categories")
    suspend fun listCategories(
        @Header("X-PD-Todo-Key") key: String?,
        @Query("pd_todo_key") keyQuery: String? = key
    ): List<String>

    @GET("habits")
    suspend fun listHabits(
        @Header("X-PD-Todo-Key") key: String?,
        @Query("pd_todo_key") keyQuery: String? = key
    ): List<String>

    @GET("items")
    suspend fun listItems(
        @Header("X-PD-Todo-Key") key: String?,
        @Query("pd_todo_key") keyQuery: String? = key,
        @Query("status") status: String? = null,
        @Query("due_on") dueOn: String? = null,
        @Query("due_after") dueAfter: String? = null,
        @Query("orderby") orderBy: String? = "updated_at",
        @Query("order") order: String? = "DESC",
        @Query("per_page") perPage: Int? = 100,
        @Query("page") page: Int? = 1
    ): List<TodoRemoteItem>

    @POST("items/{id}")
    suspend fun updateItem(
        @Path("id") id: Int,
        @Header("X-PD-Todo-Key") key: String?,
        @Query("pd_todo_key") keyQuery: String? = key,
        @Body body: Map<String, String>
    ): TodoRemoteItem

    @POST("items")
    suspend fun createItem(
        @Header("X-PD-Todo-Key") key: String?,
        @Query("pd_todo_key") keyQuery: String? = key,
        @Body body: Map<String, String>
    ): TodoRemoteItem

    @DELETE("items/{id}")
    suspend fun deleteItem(
        @Path("id") id: Int,
        @Header("X-PD-Todo-Key") key: String?,
        @Query("pd_todo_key") keyQuery: String? = key
    )
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
