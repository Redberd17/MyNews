package com.chugunova.mynews.fullscreenfragment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.chugunova.mynews.R
import com.chugunova.mynews.model.Articles
import com.chugunova.mynews.utils.GlideApp
import java.util.*
import kotlinx.android.synthetic.main.fullscreen_news_item.view.fullNewsContent
import kotlinx.android.synthetic.main.fullscreen_news_item.view.fullNewsTitle
import kotlinx.android.synthetic.main.fullscreen_news_item.view.fullscreen_image_item

class FullscreenPagerAdapter(private val newsItems: ArrayList<Articles>, val context: Context) :
    PagerAdapter() {

    override fun getCount(): Int {
        return newsItems.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as View
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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