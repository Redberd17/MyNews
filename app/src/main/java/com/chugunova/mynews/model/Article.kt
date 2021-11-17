package com.chugunova.mynews.model

import java.io.Serializable

data class Article(
    val source: Source,
    val author: String,
    val title: String,
    val description: String,
    val url: String,
    val urlToImage: String,
    var publishedAt: String,
    val content: String
) : Serializable