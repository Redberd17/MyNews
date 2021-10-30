package com.chugunova.myweather.ui.model

data class CurrentWeather(
    val temp_c: String, val condition: Condition, val wind_kph: String,
    val feelslike_c: String, val vis_km: String
) {
}