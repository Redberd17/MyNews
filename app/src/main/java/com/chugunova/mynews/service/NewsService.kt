package com.chugunova.mynews.service

import com.chugunova.mynews.model.NewsToServer
import com.chugunova.mynews.model.api.ConfigRetrofit
import retrofit2.Response

class NewsService {

    suspend fun saveUserNews(token: String, news: NewsToServer): Response<NewsToServer> {
        return ConfigRetrofit.apiService.saveUserNews(token, news)
    }

}