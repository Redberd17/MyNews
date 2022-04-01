package com.chugunova.mynews.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NewsToServer(
        val _title: String,
        val _description: String,
        val _url: String,
        val _urlToImage: String
) : Parcelable {

    val title: String = _title
    val description: String = _description
    val url: String = _url
    val urlToImage: String = _urlToImage
    var author: Long = 0
    var publishedAt: String = ""

    constructor(
            _title: String,
            _description: String,
            _url: String,
            _urlToImage: String,
            _publishedAt: String,
            _userId: Long
    ) : this(_title, _description, _url, _urlToImage) {
        author = _userId
        publishedAt = _publishedAt
    }
}

