package com.chugunova.mynews.fullscreenfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    companion object {
        fun newInstance() = FullscreenFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        /*val webView = view.findViewById(R.id.webView) as WebView
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://www.sport-express.ru/football/rfpl/news/massimo-karrera-zayavil-chto-vsegda-gotov-vernutsya-v-spartak-1854462/")*/
        return inflater.inflate(R.layout.fullscreen_news_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.viewPager)
        newsItems =
            (arguments?.getSerializable(getString(R.string.news_items)) as ArticlesWrapper).articles
        fullscreenPagerAdapter = FullscreenPagerAdapter(newsItems, requireContext())
        position = arguments?.getInt(getString(R.string.position)) as Int
        viewPager.adapter = fullscreenPagerAdapter
        setChosenNews(position)
    }

    private fun setChosenNews(position: Int) {
        viewPager.setCurrentItem(position, false)
    }
}