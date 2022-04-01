package com.chugunova.mynews.model.api

import com.chugunova.mynews.model.AuthenticationUser
import com.chugunova.mynews.model.Article
import com.chugunova.mynews.model.NewsResponse
import com.chugunova.mynews.model.NewsToServer
import com.chugunova.mynews.model.UserResponse
import retrofit2.Response
import retrofit2.http.*

interface NewsApiService {

    @GET("news/top/headlines/{country}/{page}")
    suspend fun getTopHeadlinesNews(
            @Path("country") country: String,
            @Path("page") page: Int,
            @Header("Authorization") token: String
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

    @POST("/auth/login")
    suspend fun login(@Body authUser: AuthenticationUser): Response<UserResponse>

    @POST("/users/user")
    suspend fun createUser(@Body authUser: AuthenticationUser): Response<UserResponse>

    @GET("/news/all")
    suspend fun getAllUserNews(@Header("Authorization") token: String): ArrayList<Article>

    @POST("/news/new")
    suspend fun saveUserNews(
            @Header("Authorization") token: String,
            @Body article: NewsToServer
    ): Response<NewsToServer>
}