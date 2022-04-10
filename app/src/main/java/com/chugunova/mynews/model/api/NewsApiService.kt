package com.chugunova.mynews.model.api

import com.chugunova.mynews.model.*
import retrofit2.Response
import retrofit2.http.*

interface NewsApiService {

    @GET("news/top/headlines")
    suspend fun getTopHeadlinesNews(
            @Query("country") country: String,
            @Query("page") page: Int,
            @Header("Authorization") token: String
    ): Response<LegacyNewsResponse>

    @GET("news/everything")
    suspend fun getEverythingNews(
            @Query("q") q: String,
            @Query("pageSize") pageSize: Int,
            @Query("page") page: Int,
            @Header("Authorization") token: String
    ): Response<LegacyNewsResponse>

    @GET("news/everything")
    suspend fun sortNewsBy(
            @Query("q") q: String,
            @Query("pageSize") pageSize: Int,
            @Query("page") page: Int,
            @Query("sortBy") sortBy: String,
            @Header("Authorization") token: String
    ): Response<LegacyNewsResponse>

    @POST("/auth/login")
    suspend fun login(@Body authUser: AuthenticationUser): Response<UserResponse>

    @POST("/users/user")
    suspend fun createUser(@Body authUser: AuthenticationUser): Response<UserResponse>

    @GET("/news/all")
    suspend fun getAllUserNews(@Header("Authorization") token: String): Response<ArrayList<Article>?>

    @POST("/news/new")
    suspend fun saveUserNews(
            @Header("Authorization") token: String,
            @Body article: NewsRequest
    ): Response<NewsResponse>

    @PUT("/news/{newsId}")
    suspend fun updateUserNews(
            @Header("Authorization") token: String,
            @Path("newsId") id: Long,
            @Body newsRequest: NewsRequest
    ): Response<NewsResponse>

    @DELETE("/news/{newsId}")
    suspend fun deleteUserNews(
            @Header("Authorization") token: String,
            @Path("newsId") newsId: Long
    ): Response<Unit>
}