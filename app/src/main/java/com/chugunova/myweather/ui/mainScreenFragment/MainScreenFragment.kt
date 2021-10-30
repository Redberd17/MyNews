package com.chugunova.myweather.ui.mainScreenFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chugunova.myweather.R
import com.chugunova.myweather.ui.api.ConfigRetrofit
import com.chugunova.myweather.ui.model.ForecastResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainScreenFragment : Fragment() {

    companion object {
        fun newInstance() = MainScreenFragment()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onStart() {
        super.onStart()
        val forecast = ConfigRetrofit.getForecast("London", 1)
        forecast.enqueue(object : Callback<ForecastResponse> {
            override fun onResponse(
                call: Call<ForecastResponse>,
                response: Response<ForecastResponse>
            ) {
                if (response.isSuccessful) {
                    val forecastResponse = response.body()
                    forecastResponse?.location
                }
            }

            override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}