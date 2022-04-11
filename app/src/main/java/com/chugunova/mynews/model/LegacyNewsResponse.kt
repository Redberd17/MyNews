package com.chugunova.mynews.model

data class LegacyNewsResponse(
        val totalResults: Int,
        val articles: ArrayList<Article>
)