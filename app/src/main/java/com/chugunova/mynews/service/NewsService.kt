package com.chugunova.mynews.service

import com.chugunova.mynews.model.NewsRequest
import com.chugunova.mynews.model.NewsResponse
import com.chugunova.mynews.model.api.ConfigRetrofit
import retrofit2.Response

class NewsService {

    suspend fun saveUserNews(token: String, newsRequest: NewsRequest): Response<NewsResponse> {
        return ConfigRetrofit.apiService.saveUserNews(token, newsRequest)
    }

    suspend fun updateUserNews(token: String, newsId: Long, newsRequest: NewsRequest): Response<NewsResponse> {
        return ConfigRetrofit.apiService.updateUserNews(token, newsId, newsRequest)
    }

    suspend fun deleteUserNews(token: String, newsId: Long): Response<Unit> {
        return ConfigRetrofit.apiService.deleteUserNews(token, newsId)
    }

}