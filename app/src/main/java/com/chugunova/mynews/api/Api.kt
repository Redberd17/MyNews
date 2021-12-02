package com.chugunova.mynews.api

import com.chugunova.mynews.model.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface Api {

    @GET("top-headlines")
    suspend fun getTopHeadlinesNews(
        @Query("country") country: String,
        @Query("apiKey") apiKey: String,
        @Query("page") page: Int
    ): NewsResponse

    @GET("everything")
    suspend fun getEverythingNews(
        @Query("q") q: String,
        @Query("apiKey") apiKey: String,
        @Query("pageSize") pageSize: Int,
        @Query("page") page: Int
    ): NewsResponse

    @GET("everything")
    suspend fun sortNewsBy(
        @Query("q") q: String,
        @Query("apiKey") apiKey: String,
        @Query("pageSize") pageSize: Int,
        @Query("page") page: Int,
        @Query("sortBy") sortBy: String
    ): NewsResponse
}