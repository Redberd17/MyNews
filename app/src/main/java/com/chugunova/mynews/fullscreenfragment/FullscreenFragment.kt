package com.chugunova.mynews.fullscreenfragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.chugunova.mynews.R
import com.chugunova.mynews.model.Articles
import com.chugunova.mynews.model.ArticlesWrapper
import java.util.*


class FullscreenFragment : Fragment() {

    private var newsItems = ArrayList<Articles>()
    private var position = 0

    private lateinit var viewPager: ViewPager
    private lateinit var fullscreenPagerAdapter: FullscreenPagerAdapter

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
        //viewPager = view.findViewById(R.id.viewPager)
        newsItems =
            (arguments?.getSerializable(getString(R.string.news_items)) as ArticlesWrapper).articles
        //fullscreenPagerAdapter = FullscreenPagerAdapter(newsItems, requireContext())
        position = arguments?.getInt(getString(R.string.position)) as Int
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webView.loadUrl(newsItems[position].url)
        //viewPager.adapter = fullscreenPagerAdapter
        //setChosenNews(position)
    }

    private fun setChosenNews(position: Int) {
        viewPager.setCurrentItem(position, false)
    }
}