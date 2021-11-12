package com.chugunova.mynews.api

import com.chugunova.mynews.model.NewsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface Api {

    @GET("everything")
    fun getEverythingNews(
        @Query("q") q: String,
        @Query("apiKey") apiKey: String,
        @Query("pageSize") pageSize: Int,
        @Query("page") page: Int
    ): Call<NewsResponse>

    @GET("top-headlines")
    fun getTopHeadlinesNews(
        @Query("country") country: String,
        @Query("apiKey") apiKey: String,
        @Query("page") page: Int
    ): Call<NewsResponse>

    @GET("everything")
    fun sortNewsBy(
        @Query("q") q: String,
        @Query("apiKey") apiKey: String,
        @Query("pageSize") pageSize: Int,
        @Query("page") page: Int,
        @Query("sortBy") sortBy: String
    ): Call<NewsResponse>
}