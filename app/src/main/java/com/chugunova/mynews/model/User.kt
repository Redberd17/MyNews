package com.chugunova.mynews.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
        val userId: Int,
        var username: String,
        val password: String,
        val role: String
) : Parcelable