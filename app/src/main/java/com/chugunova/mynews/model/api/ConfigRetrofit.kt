package com.chugunova.mynews.model.api

import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ConfigRetrofit {

    private var retrofit: NewsApiService? = null

    private const val BASE_URL: String = "http://192.168.0.155:8080"
//    private const val BASE_URL: String = "http://172.20.10.2:8080"

    private fun configureRetrofit(): NewsApiService {
        if (retrofit == null) {
            val okHttpClient = OkHttpClient.Builder()
                    .readTimeout(3, TimeUnit.SECONDS)
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .connectionSpecs(mutableListOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
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