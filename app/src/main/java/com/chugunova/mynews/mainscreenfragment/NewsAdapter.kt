package com.chugunova.mynews.mainscreenfragment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.chugunova.mynews.R
import com.chugunova.mynews.model.Article
import com.chugunova.mynews.utils.GlideApp
import com.chugunova.mynews.utils.StringPool
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.android.synthetic.main.news_item.view.newsItem
import kotlinx.android.synthetic.main.news_item.view.newsItemTitle
import kotlinx.android.synthetic.main.news_item.view.publishedAt

class NewsAdapter(val adapterOnClick: (Int) -> Unit) : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    private var newsItems = ArrayList<Article>()
    private lateinit var context: Context

    fun setNewsItems(newsItems: ArrayList<Article>) {
        if (newsItems.isEmpty()) {
            this.newsItems.clear()
        } else {
            this.newsItems.addAll(newsItems)
        }
        this.notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            val news = newsItems[bindingAdapterPosition]
            GlideApp.with(itemView.context)
                .load(news.urlToImage)
                .error(R.drawable.no_image)
                .placeholder(R.drawable.spinner_ring)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontTransform()
                .into(itemView.newsItem)

            itemView.apply {
                newsItemTitle.text = news.title
                val date = LocalDateTime.parse(
                    news.publishedAt,
                    DateTimeFormatter.ofPattern(StringPool.ISO_DATE_TIME.value)
                )
                publishedAt.text = context.getString(
                    R.string.publishedAt, date.format(
                        DateTimeFormatter.ofPattern(StringPool.MY_DATE_FORMAT.value)
                    )
                )
                setOnClickListener {
                    adapterOnClick(bindingAdapterPosition)
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
}