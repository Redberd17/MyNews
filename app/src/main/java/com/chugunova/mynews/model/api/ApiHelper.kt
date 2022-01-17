package com.chugunova.mynews.model.api

import com.chugunova.mynews.model.NewsResponse

private const val API_KEY: String = "122c312bd35846c3bc567d40c992cb42"

class ApiHelper(private val apiService: NewsApiService) {

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