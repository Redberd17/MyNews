package com.chugunova.mynews.utils

enum class SortVariants(val sortBy: String) {
    RELEVANCY("relevancy"),
    POPULARITY("popularity"),
    PUBLISHED_AT("publishedAt")
}