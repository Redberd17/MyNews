package com.chugunova.mynews.model

import com.chugunova.mynews.model.api.ApiHelper

class NewsRepository(private val apiHelper: ApiHelper) {

    suspend fun getEverythingNews(
        q: String,
        pageSize: Int,
        page: Int,
        sortBy: String?
    ): NewsResponse {
        return apiHelper.getEverythingNews(q, pageSize, page, sortBy)
    }

    suspend fun getTopHeadlinesNews(country: String, page: Int): NewsResponse {
        return apiHelper.getTopHeadlinesNews(country, page)
    }
}