package com.chugunova.mynews

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chugunova.mynews.mainscreenfragment.MainScreenFragment

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