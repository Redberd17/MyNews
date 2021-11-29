package com.chugunova.mynews.mainscreenfragment

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.ColorInt
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
import com.chugunova.mynews.model.NewsResponse
import com.chugunova.mynews.model.SavedRotationModel
import com.chugunova.mynews.utils.FilterVariants
import com.chugunova.mynews.utils.LayoutVariants
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
    private lateinit var sortDialog: BottomSheetDialog
    private lateinit var filterDialog: BottomSheetDialog
    private lateinit var progressBar: ProgressBar

    private var availablePages = zeroValue
    private var currentCountryPage = oneValue
    private var currentSearchPage = oneValue
    private var savedQuery = StringPool.EMPTY.value
    private var savedSortByParameter = SortVariants.PUBLISHED_AT
    private var isSearch = false
    private var isFilter = false
    private var currentLayoutVariant = LayoutVariants.AS_GRID
    private lateinit var savedFilterParameter: FilterVariants

    companion object {
        fun newInstance() = MainScreenFragment()
        private const val defaultCurrentSearchPage = 1
        private const val scrollVerticallyDirection = 1
        private const val rowNumber = 2
        private const val defaultItemsOnPage = 20
        private const val filteringItemsOnPage = 100
        private const val maxAvailableNews = 100
        private const val zeroValue = 0
        private const val oneValue = 1
        const val newsUrlString = "newsUrl"
        const val savedRotationModelString = "savedRotationModel"
        const val mainScreenFragmentString = "mainScreenFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        newsAdapter = NewsAdapter { item -> showFullScreenNewsFragment(item) }
        savedInstanceState?.apply {
            getParcelable<SavedRotationModel>(savedRotationModelString)?.let { savedModel ->
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
        progressBar = view.findViewById(R.id.mainProgressBar)
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
            if (isSearch) {
                showSortDialog()
            } else {
                showToast(getString(R.string.sortUnavailable))
            }
            false
        }
        filterItem.setOnMenuItemClickListener {
            if (isSearch) {
                showFilterDialog()
            } else {
                showToast(getString(R.string.filterUnavailable))
            }
            false
        }
        changeView.setOnMenuItemClickListener {
            changeLayoutManager()
            false
        }
        resetAllItem.setOnMenuItemClickListener {
            searchView.setQuery(StringPool.EMPTY.value, false)
            searchItem.collapseActionView()
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
            putParcelable(
                savedRotationModelString,
                savedRotationModel
            )
        }
    }

    private fun showFullScreenNewsFragment(position: Int) {
        val activity = context as AppCompatActivity
        val bundle = Bundle().apply {
            putString(
                newsUrlString,
                newsAdapter.getNewsItems()[position].url
            )
        }
        val mainScreenFragment =
            activity.supportFragmentManager.findFragmentByTag(mainScreenFragmentString) as MainScreenFragment
        activity.supportFragmentManager.beginTransaction().apply {
            replace(
                mainScreenFragment.id,
                FullscreenFragment.newInstance().apply { arguments = bundle })
            addToBackStack(null)
            commit()
        }
    }

    private fun showSortDialog() {
        if (::sortDialog.isInitialized) {
            highlightSortAction(
                sortDialog.findViewById(R.id.relevancy),
                sortDialog.findViewById(R.id.popularity),
                sortDialog.findViewById(R.id.publishedAt)
            )
            sortDialog.show()
            return
        }
        sortDialog = BottomSheetDialog(requireContext())
        sortDialog.setContentView(R.layout.sort_bottom_sheet)
        val relevancy = sortDialog.findViewById<LinearLayout>(R.id.relevancy)
        val popularity = sortDialog.findViewById<LinearLayout>(R.id.popularity)
        val publishedAt = sortDialog.findViewById<LinearLayout>(R.id.publishedAt)
        highlightSortAction(relevancy, popularity, publishedAt)
        relevancy?.setOnClickListener {
            performSortAction(SortVariants.RELEVANCY, sortDialog)
            highlightSortAction(relevancy, popularity, publishedAt)
        }
        popularity?.setOnClickListener {
            performSortAction(SortVariants.POPULARITY, sortDialog)
            highlightSortAction(relevancy, popularity, publishedAt)
        }
        publishedAt?.setOnClickListener {
            performSortAction(SortVariants.PUBLISHED_AT, sortDialog)
            highlightSortAction(relevancy, popularity, publishedAt)
        }
        sortDialog.show()
    }

    private fun highlightSortAction(
        relevancy: LinearLayout?,
        popularity: LinearLayout?,
        publishedAt: LinearLayout?
    ) {
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(R.attr.bottomSheetStyle, typedValue, true)
        @ColorInt val color = typedValue.data
        when (savedSortByParameter) {
            SortVariants.RELEVANCY -> {
                relevancy?.background = requireContext().getDrawable(R.color.colorAccent)
                popularity?.setBackgroundColor(color)
                publishedAt?.setBackgroundColor(color)
            }
            SortVariants.POPULARITY -> {
                relevancy?.setBackgroundColor(color)
                popularity?.background = requireContext().getDrawable(R.color.colorAccent)
                publishedAt?.setBackgroundColor(color)
            }
            SortVariants.PUBLISHED_AT -> {
                relevancy?.setBackgroundColor(color)
                popularity?.setBackgroundColor(color)
                publishedAt?.background = requireContext().getDrawable(R.color.colorAccent)
            }
        }
    }

    private fun showFilterDialog() {
        if (::filterDialog.isInitialized) {
            highlightFilterAction(
                filterDialog.findViewById(R.id.today),
                filterDialog.findViewById(R.id.thisWeek),
                filterDialog.findViewById(R.id.thisMonth)
            )
            filterDialog.show()
            return
        }
        filterDialog = BottomSheetDialog(requireContext())
        filterDialog.setContentView(R.layout.filter_bottom_sheet)
        val today = filterDialog.findViewById<LinearLayout>(R.id.today)
        val thisWeek = filterDialog.findViewById<LinearLayout>(R.id.thisWeek)
        val thisMonth = filterDialog.findViewById<LinearLayout>(R.id.thisMonth)
        highlightFilterAction(today, thisWeek, thisMonth)
        today?.setOnClickListener {
            performFilterAction(FilterVariants.TODAY, filterDialog)
            highlightFilterAction(today, thisWeek, thisMonth)
        }
        thisWeek?.setOnClickListener {
            performFilterAction(FilterVariants.THIS_WEEK, filterDialog)
            highlightFilterAction(today, thisWeek, thisMonth)
        }
        thisMonth?.setOnClickListener {
            performFilterAction(FilterVariants.THIS_MONTH, filterDialog)
            highlightFilterAction(today, thisWeek, thisMonth)
        }
        filterDialog.show()
    }

    private fun highlightFilterAction(
        today: LinearLayout?,
        thisWeek: LinearLayout?,
        thisMonth: LinearLayout?,
    ) {
        if (!::savedFilterParameter.isInitialized) {
            return
        }
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(R.attr.bottomSheetStyle, typedValue, true)
        @ColorInt val color = typedValue.data
        when (savedFilterParameter) {
            FilterVariants.TODAY -> {
                today?.background = requireContext().getDrawable(R.color.colorAccent)
                thisWeek?.setBackgroundColor(color)
                thisMonth?.setBackgroundColor(color)
            }
            FilterVariants.THIS_WEEK -> {
                today?.setBackgroundColor(color)
                thisWeek?.background = requireContext().getDrawable(R.color.colorAccent)
                thisMonth?.setBackgroundColor(color)
            }
            FilterVariants.THIS_MONTH -> {
                today?.setBackgroundColor(color)
                thisWeek?.setBackgroundColor(color)
                thisMonth?.background = requireContext().getDrawable(R.color.colorAccent)
            }
            FilterVariants.DEFAULT -> {
                today?.setBackgroundColor(color)
                thisWeek?.setBackgroundColor(color)
                thisMonth?.setBackgroundColor(color)
            }
        }
    }

    private fun showToast(text: String) {
        Toast(context).apply {
            duration = Toast.LENGTH_SHORT
            setText(text)
            show()
        }
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
        currentCountryPage = oneValue
        currentSearchPage = oneValue
        savedSortByParameter = SortVariants.PUBLISHED_AT
        savedFilterParameter = FilterVariants.DEFAULT
        savedQuery = StringPool.EMPTY.value
        isSearch = false
        isFilter = false
        loadCountryNews()
    }

    private fun performSortAction(sortBy: SortVariants, bottomSheetDialog: BottomSheetDialog) {
        currentSearchPage = defaultCurrentSearchPage
        newsAdapter.deleteNewsItems()
        savedSortByParameter = sortBy
        savedFilterParameter = FilterVariants.DEFAULT
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
                    FilterVariants.TODAY -> date.dayOfYear.compareTo(currentDate.dayOfYear) == zeroValue
                    FilterVariants.THIS_WEEK -> date.isBefore(currentDate) && date.isAfter(
                        currentDate.minusWeeks(oneValue.toLong())
                    )
                    FilterVariants.THIS_MONTH -> date.isBefore(currentDate) && date.isAfter(
                        currentDate.minusMonths(oneValue.toLong())
                    )
                    FilterVariants.DEFAULT -> false
                }
            }.collect(Collectors.toList())
        if (filteredNews.isEmpty())
            showToast(getString(R.string.no_matching_results))
        return filteredNews as ArrayList<Article>
    }

    private fun clearFocus() {
        val view = requireActivity().currentFocus
        view?.clearFocus()
    }

    private fun loadCountryNews() {
        showProgressBar()
        val news = ConfigRetrofit.getTopHeadlinesNews(StringPool.US.value, currentCountryPage++)
        news.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(
                call: Call<NewsResponse>,
                response: Response<NewsResponse>
            ) {
                hideProgressBar()
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
                hideProgressBar()
                showToast(getString(R.string.internet_error))
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
        showProgressBar()
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
                hideProgressBar()
                if (response.isSuccessful) {
                    val newsResponse = response.body()
                    newsResponse?.let {
                        if (newsResponse.articles.isEmpty()) {
                            currentSearchPage = oneValue
                            showToast(getString(R.string.no_content))
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
                hideProgressBar()
                showToast(getString(R.string.internet_error))
            }
        })
    }

    private fun showProgressBar() {
        if (newsAdapter.getNewsItems().isEmpty())
            progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }

    private fun recalculatePages(response: NewsResponse) {
        val fullPages: Int =
            if (response.totalResults > maxAvailableNews) maxAvailableNews / defaultItemsOnPage
            else response.totalResults / defaultItemsOnPage
        val lost: Int = response.totalResults % defaultItemsOnPage
        availablePages = fullPages + if (lost > 0) 1 else 0
    }
}