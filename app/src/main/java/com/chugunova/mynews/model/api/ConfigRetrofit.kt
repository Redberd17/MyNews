package com.chugunova.mynews.model.api

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ConfigRetrofit {

    private var retrofit: NewsApiService? = null

    private const val BASE_URL: String = "https://newsapi.org/v2/"

    private fun configureRetrofit(): NewsApiService {
        if (retrofit == null) {
            val okHttpClient = OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)
                .connectTimeout(3, TimeUnit.SECONDS)
                .build()
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NewsApiService::class.java)
        }
        return retrofit!!
    }

    val apiService: NewsApiService = configureRetrofit()
}