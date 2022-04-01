package com.chugunova.mynews.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthenticationUser(
        val username: String,
        val password: String
) : Parcelable