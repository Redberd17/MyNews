package com.chugunova.mynews.mainscreenfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugunova.mynews.R
import com.chugunova.mynews.adapter.DataAdapterNews
import com.chugunova.mynews.api.ConfigRetrofit
import com.chugunova.mynews.model.NewsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainScreenFragment : Fragment() {

    private lateinit var dataAdapterNews: DataAdapterNews
    private lateinit var recyclerView: RecyclerView
    private lateinit var showMoreButton: Button

    private var availablePages: Int = 0
    private var itemsOnPage: Int = 20
    private var currentPage: Int = 1
    private var rowNumber: Int = 2

    companion object {
        fun newInstance() = MainScreenFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataAdapterNews = DataAdapterNews()
        loadNews()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.main_fragment, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, rowNumber)
            adapter = dataAdapterNews
        }
        showMoreButton = view.findViewById(R.id.showMoreButton)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && currentPage <= availablePages) {
                    showMoreButton.visibility = View.VISIBLE
                } else {
                    showMoreButton.visibility = View.GONE
                }
            }
        })
        showMoreButton.setOnClickListener {
            loadNews()
        }
        return view
    }

    private fun loadNews() {
        val news = ConfigRetrofit.getTopHeadlinesNews("us", currentPage++)
        news.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(
                call: Call<NewsResponse>,
                response: Response<NewsResponse>
            ) {
                if (response.isSuccessful) {
                    val newsResponse = response.body()
                    newsResponse?.let {
                        val fullPages: Int = it.totalResults / itemsOnPage
                        val lost: Int = it.totalResults % itemsOnPage
                        availablePages = fullPages + if (lost > 0) 1 else 0
                        dataAdapterNews.addNewsItems(it.articles)
                        showMoreButton.visibility = View.GONE
                    }
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                Toast(context).apply {
                    setText("Internet error")
                    duration = Toast.LENGTH_LONG
                    show()
                }
            }
        })
    }
}