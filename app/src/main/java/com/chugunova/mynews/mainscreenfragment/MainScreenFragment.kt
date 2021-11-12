package com.chugunova.mynews.mainscreenfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugunova.mynews.R
import com.chugunova.mynews.api.ConfigRetrofit
import com.chugunova.mynews.model.NewsResponse
import com.chugunova.mynews.utils.SortVariants
import com.chugunova.mynews.utils.StringPool
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainScreenFragment : Fragment() {

    private lateinit var newsAdapter: NewsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var showMoreButton: Button
    private lateinit var searchView: SearchView

    private var availablePages: Int = 0
    private var itemsOnPage: Int = 20
    private var currentCountryPage: Int = 1
    private var currentSearchPage: Int = 1
    private var rowNumber: Int = 2

    private val defaultCurrentSearchPage: Int = 1
    private val scrollVerticallyDirection: Int = 1

    private var savedQuery: String = StringPool.EMPTY.value
    private var savedSortByParameter = SortVariants.PUBLISHED_AT

    companion object {
        fun newInstance() = MainScreenFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);
        newsAdapter = NewsAdapter()
        loadCountryNews()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, rowNumber)
            adapter = newsAdapter
        }
        showMoreButton = view.findViewById(R.id.showMoreButton)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(scrollVerticallyDirection) && currentCountryPage <= availablePages) {
                    showMoreButton.visibility = View.VISIBLE
                } else {
                    showMoreButton.visibility = View.GONE
                }
            }
        })
        showMoreButton.setOnClickListener {
            if (savedQuery.isNotEmpty()) searchNews(
                savedQuery,
                savedSortByParameter.sortBy
            ) else loadCountryNews()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu, menu)
        val searchItem = menu.findItem(R.id.search)
        val sortItem = menu.findItem(R.id.sort)
        searchView = searchItem?.actionView as SearchView
        searchView.setOnSearchClickListener {
            searchView.setQuery(savedQuery, false)
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchPage = defaultCurrentSearchPage
                query?.let { savedQuery = query }
                newsAdapter.deleteNewsItems()
                searchNews(query, savedSortByParameter.sortBy)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        searchView.setOnCloseListener {
            currentSearchPage = defaultCurrentSearchPage
            false
        }
        sortItem.setOnMenuItemClickListener {
            showSortDialog()
            false
        }
    }

    private fun showSortDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottoms_sheet)
        val relevancy = bottomSheetDialog.findViewById<LinearLayout>(R.id.relevancy)
        val popularity = bottomSheetDialog.findViewById<LinearLayout>(R.id.popularity)
        val publishedAt = bottomSheetDialog.findViewById<LinearLayout>(R.id.publishedAt)
        relevancy?.setOnClickListener {
            performSortAction(SortVariants.RELEVANCY, bottomSheetDialog)
        }
        popularity?.setOnClickListener {
            performSortAction(SortVariants.POPULARITY, bottomSheetDialog)
        }
        publishedAt?.setOnClickListener {
            performSortAction(SortVariants.PUBLISHED_AT, bottomSheetDialog)
        }
        bottomSheetDialog.show()
    }

    private fun performSortAction(sortBy: SortVariants, bottomSheetDialog: BottomSheetDialog) {
        currentSearchPage = defaultCurrentSearchPage
        newsAdapter.deleteNewsItems()
        savedSortByParameter = sortBy
        searchNews(savedQuery, savedSortByParameter.sortBy)
        bottomSheetDialog.dismiss()
        clearFocus()
    }

    private fun clearFocus() {
        val view = requireActivity().currentFocus
        view?.clearFocus()
    }

    private fun loadCountryNews() {
        val news = ConfigRetrofit.getTopHeadlinesNews(StringPool.US.value, currentCountryPage++)
        news.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(
                call: Call<NewsResponse>,
                response: Response<NewsResponse>
            ) {
                if (response.isSuccessful) {
                    val newsResponse = response.body()
                    newsResponse?.let {
                        recalculatePages(it)
                        newsAdapter.addNewsItems(it.articles)
                        showMoreButton.visibility = View.GONE
                    }
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                Toast(context).apply {
                    setText(getString(R.string.internet_error))
                    duration = Toast.LENGTH_LONG
                    show()
                }
            }
        })
    }

    private fun searchNews(query: String?, sortBy: String?) {
        query?.let {
            ConfigRetrofit.getEverythingNews(
                it,
                itemsOnPage,
                currentSearchPage++,
                sortBy
            )
        }
            ?.enqueue(object : Callback<NewsResponse> {
                override fun onResponse(
                    call: Call<NewsResponse>,
                    response: Response<NewsResponse>
                ) {
                    if (response.isSuccessful) {
                        val newsResponse = response.body()
                        newsResponse?.let {
                            recalculatePages(it)
                            newsAdapter.addNewsItems(it.articles)
                            showMoreButton.visibility = View.GONE
                        }
                    }
                }

                override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                    Toast(context).apply {
                        setText(getString(R.string.internet_error))
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