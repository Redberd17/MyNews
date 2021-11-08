package com.chugunova.mynews.fullscreenfragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.chugunova.mynews.R
import com.chugunova.mynews.adapter.GlideApp
import com.chugunova.mynews.model.Articles
import com.chugunova.mynews.model.ArticlesWrapper
import java.util.*
import kotlinx.android.synthetic.main.fullscreen_news_item.view.fullNewsContent
import kotlinx.android.synthetic.main.fullscreen_news_item.view.fullNewsTitle
import kotlinx.android.synthetic.main.fullscreen_news_item.view.fullscreen_image_item


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
        val view = inflater.inflate(R.layout.fullscreen_news_fragment, container, false)
        viewPager = view.findViewById(R.id.viewPager)
        fullscreenPagerAdapter = FullscreenPagerAdapter()
        newsItems =
            (arguments?.getSerializable(getString(R.string.news_items)) as ArticlesWrapper).articles
        position = arguments?.getInt(getString(R.string.position)) as Int
        viewPager.adapter = fullscreenPagerAdapter
        setChosenNews(position)
        /*val webView = view.findViewById(R.id.webView) as WebView
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://www.sport-express.ru/football/rfpl/news/massimo-karrera-zayavil-chto-vsegda-gotov-vernutsya-v-spartak-1854462/")*/
        return view
    }

    private fun setChosenNews(position: Int) {
        viewPager.setCurrentItem(position, false)
    }

    inner class FullscreenPagerAdapter : PagerAdapter() {
        override fun getCount(): Int {
            return newsItems.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object` as View
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val layoutInflater =
                activity?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = layoutInflater.inflate(R.layout.fullscreen_news_item, container, false)
            val news = newsItems[position]
            GlideApp.with(view.context)
                .load(news.urlToImage)
                .error(R.drawable.ic_launcher_background)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(view.fullscreen_image_item)
            view.fullNewsTitle.text = news.title
            view.fullNewsContent.text = news.description
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
    }
}