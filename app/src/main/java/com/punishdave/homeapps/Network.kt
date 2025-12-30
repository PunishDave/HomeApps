package com.punishdave.homeapps

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object Network {
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val api: MealPlannerApi = Retrofit.Builder()
        .baseUrl("https://apm.d4c.myftpupload.com/index.php/wp-json/meal-planner/v1/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()
        .create(MealPlannerApi::class.java)

    val haveWeGotApi: HaveWeGotApi = Retrofit.Builder()
        .baseUrl("https://apm.d4c.myftpupload.com/index.php/wp-json/have-we-got/v1/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()
        .create(HaveWeGotApi::class.java)

    val todoApi: TodoApi = Retrofit.Builder()
        .baseUrl("https://apm.d4c.myftpupload.com/index.php/wp-json/pd-todo/v1/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()
        .create(TodoApi::class.java)

    val workoutApi: WorkoutApi = Retrofit.Builder()
        .baseUrl("https://apm.d4c.myftpupload.com/index.php/wp-json/pdswl/v1/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()
        .create(WorkoutApi::class.java)

    // DataStore JSON adapters
    val weekAdapter = moshi.adapter(WeekResponse::class.java)

    private val recipesType = Types.newParameterizedType(List::class.java, Recipe::class.java)
    val recipesAdapter = moshi.adapter<List<Recipe>>(recipesType)
}
