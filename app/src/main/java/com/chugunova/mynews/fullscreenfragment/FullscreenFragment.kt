package com.chugunova.mynews.fullscreenfragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.chugunova.mynews.R


class FullscreenFragment : Fragment() {

    private lateinit var webView: WebView

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
        webView.webViewClient = WebViewClient()
        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val url = arguments?.getString(getString(R.string.news_url))
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        url?.let {
            webView.loadUrl(it)
        }
    }
}