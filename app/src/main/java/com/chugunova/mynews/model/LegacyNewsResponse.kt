package com.chugunova.mynews.model

data class LegacyNewsResponse(
        val status: String,
        val totalResults: Int,
        val articles: ArrayList<Article>
)