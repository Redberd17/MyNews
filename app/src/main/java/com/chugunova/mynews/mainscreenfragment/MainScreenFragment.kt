package com.chugunova.mynews.mainscreenfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugunova.mynews.R
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
    private var currentCountryPage: Int = 1
    private var currentSearchPage: Int = 1
    private var rowNumber: Int = 2

    private var savedQuery: String = ""

    companion object {
        fun newInstance() = MainScreenFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);
        dataAdapterNews = DataAdapterNews()
        loadCountryNews()
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
                if (!recyclerView.canScrollVertically(1) && currentCountryPage <= availablePages) {
                    showMoreButton.visibility = View.VISIBLE
                } else {
                    showMoreButton.visibility = View.GONE
                }
            }
        })
        showMoreButton.setOnClickListener {
            if (savedQuery.isNotEmpty()) searchNews(savedQuery) else loadCountryNews()
        }
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu, menu)
        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem?.actionView as SearchView
        searchView.setOnCloseListener {
            currentSearchPage = 1
            false
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchPage = 1
                query?.let { savedQuery = query }
                dataAdapterNews.deleteNewsItems()
                searchNews(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun loadCountryNews() {
        val news = ConfigRetrofit.getTopHeadlinesNews("us", currentCountryPage++)
        news.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(
                call: Call<NewsResponse>,
                response: Response<NewsResponse>
            ) {
                if (response.isSuccessful) {
                    val newsResponse = response.body()
                    newsResponse?.let {
                        recalculatePages(it)
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

    private fun searchNews(query: String?) {
        query?.let { ConfigRetrofit.getEverythingNews(it, itemsOnPage, currentSearchPage++) }
            ?.enqueue(object : Callback<NewsResponse> {
                override fun onResponse(
                    call: Call<NewsResponse>,
                    response: Response<NewsResponse>
                ) {
                    if (response.isSuccessful) {
                        val newsResponse = response.body()
                        newsResponse?.let {
                            recalculatePages(it)
                            dataAdapterNews.addNewsItems(it.articles)
                            showMoreButton.visibility = View.GONE
                        }
                    } else {
                        response.errorBody()
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

    private fun recalculatePages(response: NewsResponse) {
        val fullPages: Int = response.totalResults / itemsOnPage
        val lost: Int = response.totalResults % itemsOnPage
        availablePages = fullPages + if (lost > 0) 1 else 0
    }
}