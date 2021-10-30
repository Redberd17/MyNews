package com.chugunova.myweather.ui.api

import com.chugunova.myweather.ui.model.ForecastResponse
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ConfigRetrofit {

    var retrofit: Api? = null

    private const val baseUrl: String = "https://api.weatherapi.com"
    private const val apiKey: String = "f2a3217fc2bf4e5d809113534213010"

    private fun configureRetrofit(): Api? {
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
        return retrofit
    }


    fun getForecast(q: String, days: Int): Call<ForecastResponse> {
        return configureRetrofit()!!.getForecast(apiKey, q, days)
    }
}