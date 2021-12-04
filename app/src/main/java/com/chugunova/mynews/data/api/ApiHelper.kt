package com.chugunova.mynews.data.api

import com.chugunova.mynews.data.model.NewsResponse

private const val API_KEY: String = "a4c6d91742e74962957312c56c72e861"

class ApiHelper(private val apiService: ApiService) {

    suspend fun getEverythingNews(
        q: String,
        pageSize: Int,
        page: Int,
        sortBy: String?
    ): NewsResponse {
        return if (sortBy != null) {
            apiService.sortNewsBy(q, API_KEY, pageSize, page, sortBy)
        } else {
            apiService.getEverythingNews(q, API_KEY, pageSize, page)
        }
    }

    suspend fun getTopHeadlinesNews(country: String, page: Int): NewsResponse {
        return apiService.getTopHeadlinesNews(country, API_KEY, page)
    }
}