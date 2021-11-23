package com.chugunova.mynews.mainscreenfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugunova.mynews.R
import com.chugunova.mynews.api.ConfigRetrofit
import com.chugunova.mynews.fullscreenfragment.FullscreenFragment
import com.chugunova.mynews.model.Article
import com.chugunova.mynews.model.ArticlesWrapper
import com.chugunova.mynews.model.NewsResponse
import com.chugunova.mynews.model.SavedRotationModel
import com.chugunova.mynews.utils.FilterVariants
import com.chugunova.mynews.utils.LayoutVariants
import com.chugunova.mynews.utils.NumberPool
import com.chugunova.mynews.utils.SortVariants
import com.chugunova.mynews.utils.StringPool
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainScreenFragment : Fragment() {

    private lateinit var newsAdapter: NewsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var showMoreButton: FloatingActionButton
    private lateinit var searchView: SearchView

    private var availablePages: Int = NumberPool.ZERO.value
    private var currentCountryPage: Int = NumberPool.ONE.value
    private var currentSearchPage: Int = NumberPool.ONE.value
    private var savedQuery: String = StringPool.EMPTY.value
    private var savedSortByParameter = SortVariants.PUBLISHED_AT
    private var isSearch: Boolean = false
    private var isFilter: Boolean = false
    private var currentLayoutVariant: LayoutVariants = LayoutVariants.AS_GRID
    private lateinit var savedFilterParameter: FilterVariants

    private val defaultCurrentSearchPage: Int = NumberPool.ONE.value
    private val scrollVerticallyDirection: Int = NumberPool.ONE.value
    private val rowNumber: Int = NumberPool.TWO.value
    private val defaultItemsOnPage: Int = NumberPool.TWENTY.value
    private val filteringItemsOnPage: Int = NumberPool.HUNDRED.value
    private val maxAvailableNews: Int = NumberPool.HUNDRED.value

    companion object {
        fun newInstance() = MainScreenFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        newsAdapter = NewsAdapter { item -> showFullScreenNewsFragment(item) }
        savedInstanceState?.apply {
            getSerializable(getString(R.string.saved_rotation_model))?.let { savedModel ->
                savedModel as SavedRotationModel
                newsAdapter.addNewsItems(savedModel.articles)
                availablePages = savedModel.availablePages
                currentCountryPage = savedModel.currentCountryPage
                currentSearchPage = savedModel.currentSearchPage
                savedQuery = savedModel.savedQuery
                savedSortByParameter = savedModel.savedSortByParameter
                isSearch = savedModel.isSearch
                isFilter = savedModel.isFilter
                savedModel.savedFilterParameter?.let {
                    savedFilterParameter = it
                }
                currentLayoutVariant = savedModel.currentLayoutVariant
            }
        }
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
            chooseLayoutManager()
            adapter = newsAdapter
        }
        showMoreButton = view.findViewById(R.id.showMoreButton)
        if (newsAdapter.getNewsItems().isEmpty()) {
            loadCountryNews()
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(scrollVerticallyDirection)
                    && if (isSearch)
                        currentSearchPage < availablePages
                    else currentCountryPage <= availablePages
                ) {
                    if (!isFilter)
                        showMoreButton.visibility = View.VISIBLE
                } else {
                    showMoreButton.visibility = View.GONE
                }
            }
        })
        showMoreButton.setOnClickListener {
            if (savedQuery.isNotEmpty()) searchNews(
                savedQuery,
                defaultItemsOnPage,
                savedSortByParameter.sortBy,
                null,
                false
            ) else loadCountryNews()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu, menu)
        val searchItem = menu.findItem(R.id.search)
        val sortItem = menu.findItem(R.id.sort)
        val filterItem = menu.findItem(R.id.filter)
        val changeView = menu.findItem(R.id.changeView)
        val resetAllItem = menu.findItem(R.id.resetAll)
        searchView = searchItem?.actionView as SearchView
        searchView.setOnSearchClickListener {
            searchView.setQuery(savedQuery, false)
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchPage = defaultCurrentSearchPage
                query?.let { savedQuery = query }
                newsAdapter.deleteNewsItems()
                searchNews(
                    query,
                    defaultItemsOnPage,
                    savedSortByParameter.sortBy,
                    null,
                    false
                )
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
        filterItem.setOnMenuItemClickListener {
            showFilterDialog()
            false
        }
        changeView.setOnMenuItemClickListener {
            changeLayoutManager()
            false
        }
        resetAllItem.setOnMenuItemClickListener {
            resetAll()
            false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val savedRotationModel = SavedRotationModel(
            newsAdapter.getNewsItems(),
            availablePages,
            currentCountryPage,
            currentSearchPage,
            savedQuery,
            savedSortByParameter,
            isSearch,
            isFilter,
            if (::savedFilterParameter.isInitialized) savedFilterParameter else null,
            currentLayoutVariant
        )
        outState.apply {
            putSerializable(
                getString(R.string.saved_rotation_model),
                savedRotationModel
            )
        }
    }

    private fun showFullScreenNewsFragment(position: Int) {
        val activity = context as AppCompatActivity
        val bundle = Bundle().apply {
            putSerializable(
                getString(R.string.news_items),
                ArticlesWrapper(newsAdapter.getNewsItems())
            )
            putInt(getString(R.string.position), position)
        }
        val mainScreenFragment =
            activity.supportFragmentManager.findFragmentByTag(getString(R.string.main_screen_fragment)) as MainScreenFragment
        activity.supportFragmentManager.beginTransaction().apply {
            replace(
                mainScreenFragment.id,
                FullscreenFragment.newInstance().apply { arguments = bundle })
            addToBackStack(null)
            commit()
        }
    }

    private fun showSortDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.sort_bottom_sheet)
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

    private fun showFilterDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.filter_bottom_sheet)
        val today = bottomSheetDialog.findViewById<LinearLayout>(R.id.today)
        val thisWeek = bottomSheetDialog.findViewById<LinearLayout>(R.id.thisWeek)
        val thisMonth = bottomSheetDialog.findViewById<LinearLayout>(R.id.thisMonth)
        today?.setOnClickListener {
            performFilterAction(FilterVariants.TODAY, bottomSheetDialog)
        }
        thisWeek?.setOnClickListener {
            performFilterAction(FilterVariants.THIS_WEEK, bottomSheetDialog)
        }
        thisMonth?.setOnClickListener {
            performFilterAction(FilterVariants.THIS_MONTH, bottomSheetDialog)
        }
        bottomSheetDialog.show()
    }

    private fun chooseLayoutManager() {
        when (currentLayoutVariant) {
            LayoutVariants.AS_GRID -> {
                recyclerView.layoutManager = GridLayoutManager(context, rowNumber)
            }
            LayoutVariants.AS_LIST -> {
                recyclerView.layoutManager = LinearLayoutManager(context)
            }
        }
    }

    private fun changeLayoutManager() {
        when (currentLayoutVariant) {
            LayoutVariants.AS_GRID -> {
                currentLayoutVariant = LayoutVariants.AS_LIST
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = newsAdapter
                }
            }
            LayoutVariants.AS_LIST -> {
                currentLayoutVariant = LayoutVariants.AS_GRID
                recyclerView.apply {
                    layoutManager = GridLayoutManager(context, rowNumber)
                    adapter = newsAdapter
                }
            }
        }
    }

    private fun resetAll() {
        newsAdapter.deleteNewsItems()
        currentCountryPage = NumberPool.ONE.value
        currentSearchPage = NumberPool.ONE.value
        savedSortByParameter = SortVariants.PUBLISHED_AT
        savedQuery = StringPool.EMPTY.value
        isSearch = false
        isFilter = false
        loadCountryNews()
    }

    private fun performSortAction(sortBy: SortVariants, bottomSheetDialog: BottomSheetDialog) {
        currentSearchPage = defaultCurrentSearchPage
        newsAdapter.deleteNewsItems()
        savedSortByParameter = sortBy
        searchNews(
            savedQuery,
            defaultItemsOnPage,
            savedSortByParameter.sortBy,
            null,
            false
        )
        bottomSheetDialog.dismiss()
        clearFocus()
    }

    private fun performFilterAction(
        filterBy: FilterVariants,
        bottomSheetDialog: BottomSheetDialog
    ) {
        isFilter = true
        savedFilterParameter = filterBy
        currentSearchPage = defaultCurrentSearchPage
        searchNews(
            savedQuery,
            filteringItemsOnPage,
            savedSortByParameter.sortBy,
            ::filterNews,
            true
        )
        newsAdapter.deleteNewsItems()
        showMoreButton.visibility = View.GONE
        bottomSheetDialog.dismiss()
        clearFocus()
    }

    private fun filterNews(
        newsItems: ArrayList<Article>,
        filterBy: FilterVariants
    ): ArrayList<Article> {
        val currentDate = LocalDateTime.now()
        val filteredNews = newsItems.stream()
            .filter { newsItem ->
                val date = LocalDateTime.parse(
                    newsItem.publishedAt,
                    DateTimeFormatter.ofPattern(StringPool.ISO_DATE_TIME.value)
                )
                when (filterBy) {
                    FilterVariants.TODAY -> date.dayOfYear.compareTo(currentDate.dayOfYear) == NumberPool.ZERO.value
                    FilterVariants.THIS_WEEK -> date.isBefore(currentDate) && date.isAfter(
                        currentDate.minusWeeks(NumberPool.ONE.value.toLong())
                    )
                    FilterVariants.THIS_MONTH -> date.isBefore(currentDate) && date.isAfter(
                        currentDate.minusMonths(NumberPool.ONE.value.toLong())
                    )
                }
            }.collect(Collectors.toList())
        return filteredNews as java.util.ArrayList<Article>
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

    private fun searchNews(
        query: String?,
        pageSize: Int,
        sortBy: String?,
        filterNews: ((ArrayList<Article>, FilterVariants) -> ArrayList<Article>)?,
        isFilterAction: Boolean
    ) {
        isSearch = true
        if (!isFilterAction)
            isFilter = false
        query?.let {
            ConfigRetrofit.getEverythingNews(
                it,
                pageSize,
                currentSearchPage++,
                sortBy
            )
        }?.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(
                call: Call<NewsResponse>,
                response: Response<NewsResponse>
            ) {
                if (response.isSuccessful) {
                    val newsResponse = response.body()
                    newsResponse?.let {
                        if (newsResponse.articles.isEmpty()) {
                            currentSearchPage = NumberPool.ONE.value
                            Toast(context).apply {
                                setText(getString(R.string.no_content))
                                duration = Toast.LENGTH_LONG
                                show()
                            }
                        } else {
                            newsResponse.let {
                                recalculatePages(it)
                                newsAdapter.addNewsItems(
                                    if (filterNews != null) filterNews(
                                        it.articles,
                                        savedFilterParameter
                                    ) else it.articles
                                )
                            }
                        }
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
        val fullPages: Int =
            if (response.totalResults > maxAvailableNews) maxAvailableNews / defaultItemsOnPage
            else response.totalResults / defaultItemsOnPage
        val lost: Int = response.totalResults % defaultItemsOnPage
        availablePages = fullPages + if (lost > 0) 1 else 0
    }
}