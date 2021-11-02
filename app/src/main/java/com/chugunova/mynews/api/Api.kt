package com.chugunova.mynews.api

import com.chugunova.mynews.model.NewsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface Api {

    @GET("everything")
    fun getEverythingNews(
        @Query("q") q: String,
        @Query("apiKey") apiKey: String
    ): Call<NewsResponse>

    @GET("top-headlines")
    fun getTopHeadlinesNews(
        @Query("country") country: String,
        @Query("apiKey") apiKey: String
    ): Call<NewsResponse>
}