package com.chugunova.myweather

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chugunova.myweather.ui.mainScreenFragment.MainScreenFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainScreenFragment.newInstance())
                .commitNow()
        }
    }
}