package com.chugunova.mynews.view

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.chugunova.mynews.R


class MainActivity : AppCompatActivity() {

    private lateinit var ad: AlertDialog.Builder

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
        ad = AlertDialog.Builder(this)
        ad.setTitle("Warning")
        ad.setMessage("Do you want to close the app?")
        ad.setPositiveButton("Yes") { _, _ -> finish() }
        ad.setNegativeButton("No") { _, _ -> }
    }

    private fun isDarkTheme(): Boolean {
        return this.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onBackPressed() {
        val f: Fragment? = supportFragmentManager.findFragmentById(R.id.container)
        if (f is NewsAllFragment || f is LoginFragment) {
            ad.show()
            return

        }
        super.onBackPressed()
    }
}