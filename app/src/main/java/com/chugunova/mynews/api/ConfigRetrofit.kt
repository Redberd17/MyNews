package com.chugunova.mynews.api

import com.chugunova.mynews.model.NewsResponse
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ConfigRetrofit {

    private var retrofit: Api? = null

    private const val BASE_URL: String = "https://newsapi.org/v2/"
    private const val API_KEY: String = "a4c6d91742e74962957312c56c72e861"

    private fun configureRetrofit(): Api {
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
                .create(Api::class.java)
        }
        return retrofit!!
    }

    suspend fun getEverythingNews(
        q: String,
        pageSize: Int,
        page: Int,
        sortBy: String?
    ): NewsResponse {
        return if (sortBy != null) {
            configureRetrofit().sortNewsBy(q, API_KEY, pageSize, page, sortBy)
        } else {
            configureRetrofit().getEverythingNews(q, API_KEY, pageSize, page)
        }
    }

    suspend fun getTopHeadlinesNews(country: String, page: Int): NewsResponse {
        return configureRetrofit().getTopHeadlinesNews(country, API_KEY, page)
    }
}