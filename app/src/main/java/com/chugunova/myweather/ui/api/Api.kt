package com.chugunova.myweather.ui.api

import com.chugunova.myweather.ui.model.ForecastResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface Api {

    @GET("/v1/forecast.json")
    fun getForecast(
        @Query("key") key: String,
        @Query("q") q: String,
        @Query("days") days: Int
    ): Call<ForecastResponse>
}