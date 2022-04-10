package com.chugunova.mynews.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NewsRequest(
        val title: String,
        val description: String,
        val url: String,
        val urlToImage: String
) : Parcelable

