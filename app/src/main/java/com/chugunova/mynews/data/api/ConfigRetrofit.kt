package com.chugunova.mynews.data.api

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ConfigRetrofit {

    private var retrofit: ApiService? = null

    private const val BASE_URL: String = "https://newsapi.org/v2/"

    private fun configureRetrofit(): ApiService {
        if (retrofit == null) {
            val okHttpClient = OkHttpClient.Builder()
                .readTimeout(2, TimeUnit.SECONDS)
                .connectTimeout(2, TimeUnit.SECONDS)
                .build()
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
        return retrofit!!
    }

    val apiService: ApiService = configureRetrofit()
}