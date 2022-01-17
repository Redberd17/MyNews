package com.chugunova.mynews.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.chugunova.mynews.R


class NewsDetailFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var menuProgressBar: MenuItem

    companion object {
        fun newInstance() = NewsDetailFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        val url = arguments?.getString(NewsAllFragment.NEWS_URL_STRING)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW;
        webSettings.allowFileAccess = true
        webView.clearCache(true)
        url?.let {
            webView.loadUrl(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu2, menu)
        menuProgressBar = menu.findItem(R.id.waiter)
        menuProgressBar.actionView = ProgressBar(context)
    }

    inner class WebViewClient : android.webkit.WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            menuProgressBar.isVisible = false
        }
    }
}