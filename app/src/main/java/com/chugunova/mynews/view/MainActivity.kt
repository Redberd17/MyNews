package com.chugunova.mynews.view

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chugunova.mynews.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.setDisplayShowTitleEnabled(false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (isDarkTheme()) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.AppTheme)
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, LoginFragment.newInstance())
                    .commit()
        }
    }

    private fun isDarkTheme(): Boolean {
        return this.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}