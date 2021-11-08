package com.chugunova.mynews.api

import com.chugunova.mynews.model.NewsResponse
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ConfigRetrofit {

    var retrofit: Api? = null

    private const val baseUrl: String = "https://newsapi.org/v2/"
    private const val apiKey: String = "a4c6d91742e74962957312c56c72e861"

    private fun configureRetrofit(): Api {
        if (retrofit == null) {
            val okHttpClient = OkHttpClient.Builder()
                .readTimeout(2, TimeUnit.SECONDS)
                .connectTimeout(2, TimeUnit.SECONDS)
                .build()
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(Api::class.java)
        }
        return retrofit!!
    }


    fun getEverythingNews(q: String): Call<NewsResponse> {
        return configureRetrofit().getEverythingNews(q, apiKey)
    }

    fun getTopHeadlinesNews(country: String, page: Int): Call<NewsResponse> {
        return configureRetrofit().getTopHeadlinesNews(country, apiKey, page)
    }
}