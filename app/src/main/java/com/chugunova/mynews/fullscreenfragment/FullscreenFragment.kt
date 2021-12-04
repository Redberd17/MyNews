package com.chugunova.mynews.fullscreenfragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.chugunova.mynews.R
import com.chugunova.mynews.mainscreenfragment.MainScreenFragment


class FullscreenFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    companion object {
        fun newInstance() = FullscreenFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fullscreen_news_fragment, container, false)
        webView = view.findViewById(R.id.webView)
        progressBar = view.findViewById(R.id.progressBar);
        webView.webViewClient = WebViewClient()
        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val url = arguments?.getString(MainScreenFragment.NEWS_URL_STRING)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        url?.let {
            webView.loadUrl(it)
        }
    }

    inner class WebViewClient : android.webkit.WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
        }
    }
}