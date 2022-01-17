package com.chugunova.mynews.mainscreenfragment

import androidx.recyclerview.widget.DiffUtil
import com.chugunova.mynews.model.Article

class DiffCallback(
    private var oldArticles: ArrayList<Article>,
    private var newArticles: ArrayList<Article>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldArticles.size
    }

    override fun getNewListSize(): Int {
        return newArticles.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldArticles[oldItemPosition] == newArticles[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldArticles[oldItemPosition] == newArticles[newItemPosition]
    }
}