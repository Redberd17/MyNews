package com.chugunova.mynews.service

import com.chugunova.mynews.model.AuthenticationUser
import com.chugunova.mynews.model.UserResponse
import com.chugunova.mynews.model.api.ConfigRetrofit
import retrofit2.Response

class UserService {

    suspend fun login(authUser: AuthenticationUser): Response<UserResponse> {
        return ConfigRetrofit.apiService.login(authUser)
    }

    suspend fun createUser(authUser: AuthenticationUser): Response<UserResponse> {
        return ConfigRetrofit.apiService.createUser(authUser)
    }
}