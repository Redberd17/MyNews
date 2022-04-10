package com.chugunova.mynews.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NewsResponse(
        val id: Long,
        val title: String,
        val description: String,
        val url: String,
        val urlToImage: String,
        val author: String,
        val publishedAt: String
) : Parcelable

