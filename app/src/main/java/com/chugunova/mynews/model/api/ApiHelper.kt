package com.chugunova.mynews.model.api

import com.chugunova.mynews.model.LegacyNewsResponse
import retrofit2.Response

private const val API_KEY: String = "122c312bd35846c3bc567d40c992cb42"

class ApiHelper(private val apiService: NewsApiService) {

    suspend fun getEverythingNews(
            q: String,
            pageSize: Int,
            page: Int,
            sortBy: String?,
            token: String
    ): Response<LegacyNewsResponse> {
        return if (sortBy != null) {
            apiService.sortNewsBy(q, pageSize, page, sortBy, token)
        } else {
            apiService.getEverythingNews(q, pageSize, page, token)
        }
    }

    suspend fun getTopHeadlinesNews(country: String, page: Int, token: String): Response<LegacyNewsResponse> {
        return apiService.getTopHeadlinesNews(country, page, token)
    }
}