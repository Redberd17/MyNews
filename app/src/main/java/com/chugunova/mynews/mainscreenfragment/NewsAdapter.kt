package com.chugunova.mynews.mainscreenfragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.chugunova.mynews.R
import com.chugunova.mynews.fullscreenfragment.FullscreenFragment
import com.chugunova.mynews.model.Articles
import com.chugunova.mynews.model.ArticlesWrapper
import com.chugunova.mynews.utils.GlideApp
import java.util.*
import kotlinx.android.synthetic.main.news_item.view.newsItem
import kotlinx.android.synthetic.main.news_item.view.newsItemTitle

class NewsAdapter : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    private var newsItems = ArrayList<Articles>()
    private lateinit var context: Context

    fun addNewsItems(newsItems: ArrayList<Articles>) {
        this.newsItems.addAll(newsItems)
        this.notifyDataSetChanged()
    }

    fun deleteNewsItems() {
        this.newsItems.clear()
        this.notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            val news = newsItems[bindingAdapterPosition]
            GlideApp.with(itemView.context)
                .load(news.urlToImage)
                .error(R.drawable.ic_launcher_background)
                .placeholder(R.drawable.spinner_ring)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontTransform()
                .into(itemView.newsItem)

            itemView.apply {
                newsItemTitle.text = news.title
                setOnClickListener {
                    showFullScreenNewsFragment(bindingAdapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.news_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return newsItems.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    private fun showFullScreenNewsFragment(position: Int) {
        val activity = context as AppCompatActivity
        val bundle = Bundle().apply {
            putSerializable(context.getString(R.string.news_items), ArticlesWrapper(newsItems))
            putInt(context.getString(R.string.position), position)
        }
        val mainScreenFragment =
            activity.supportFragmentManager.findFragmentByTag(context.getString(R.string.main_screen_fragment)) as MainScreenFragment
        activity.supportFragmentManager.beginTransaction().apply {
            replace(
                mainScreenFragment.id,
                FullscreenFragment.newInstance().apply { arguments = bundle })
            addToBackStack(null)
            commit()
        }
    }
}