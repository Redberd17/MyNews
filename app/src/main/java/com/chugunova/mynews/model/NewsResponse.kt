package com.chugunova.mynews.model

import java.io.Serializable

data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: ArrayList<Article>
) : Serializable