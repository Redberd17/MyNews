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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugunova.mynews.R
import com.chugunova.mynews.data.api.ApiHelper
import com.chugunova.mynews.data.api.ConfigRetrofit
import com.chugunova.mynews.data.model.Article
import com.chugunova.mynews.data.model.NewsResponse
import com.chugunova.mynews.data.model.SavedRotationModel
import com.chugunova.mynews.data.repository.NewsRepository
import com.chugunova.mynews.fullscreenfragment.FullscreenFragment
import com.chugunova.mynews.utils.FilterVariants
import com.chugunova.mynews.utils.LayoutVariants
import com.chugunova.mynews.utils.SortVariants
import com.chugunova.mynews.utils.StringPool
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainScreenFragment : Fragment() {

    private lateinit var newsAdapter: NewsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var showMoreButton: FloatingActionButton
    private lateinit var searchView: SearchView
    private lateinit var sortDialog: BottomSheetDialog
    private lateinit var filterDialog: BottomSheetDialog
    private lateinit var progressBar: ProgressBar

    private lateinit var newsRepository: NewsRepository

    private var availablePages = ZERO
    private var currentCountryPage = ONE
    private var currentSearchPage = ONE
    private var savedQuery = StringPool.EMPTY.value
    private var savedSortByParameter = SortVariants.PUBLISHED_AT
    private var isSearch = false
    private var isFilter = false
    private var currentLayoutVariant = LayoutVariants.AS_GRID
    private var savedFilterParameter = FilterVariants.DEFAULT
    private var articles = ArrayList<Article>()

    companion object {
        fun newInstance() = MainScreenFragment()
        private const val DEFAULT_CURRENT_SEARCH_PAGE = 1
        private const val SCROLL_VERTICALLY_DIRECTION = 1
        private const val ROW_NUMBER = 2
        private const val DEFAULT_ITEMS_ON_PAGE = 20
        private const val FILTERING_ITEMS_ON_PAGE = 100
        private const val MAX_AVAILABLE_NEWS = 100
        private const val ZERO = 0
        private const val ONE = 1
        const val NEWS_URL_STRING = "newsUrl"
        const val SAVED_ROTATION_MODEL_STRING = "savedRotationModel"
        const val MAIN_SCREEN_FRAGMENT_STRING = "mainScreenFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        newsRepository = NewsRepository(ApiHelper(ConfigRetrofit.apiService))
        newsAdapter = NewsAdapter { item -> showFullScreenNewsFragment(item) }
        savedInstanceState?.apply {
            getParcelable<SavedRotationModel>(SAVED_ROTATION_MODEL_STRING)?.let { savedModel ->
                articles = savedModel.articles
                newsAdapter.updateList(articles)
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
        if (articles.isEmpty()) {
            loadCountryNews()
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(SCROLL_VERTICALLY_DIRECTION)
                    && if (isSearch)
                        currentSearchPage < availablePages
                    else currentCountryPage <= availablePages
                ) {
                    if (!isFilter)
                        showMoreButton()
                } else {
                    hideMoreButton()
                }
            }
        })
        showMoreButton.setOnClickListener {
            if (savedQuery.isNotEmpty()) searchNews(
                savedQuery,
                DEFAULT_ITEMS_ON_PAGE,
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
                currentSearchPage = DEFAULT_CURRENT_SEARCH_PAGE
                query?.let { savedQuery = query }
                articles.clear()
                newsAdapter.updateList(articles)
                val performFilter = savedFilterParameter != FilterVariants.DEFAULT
                searchNews(
                    query,
                    if (performFilter)
                        FILTERING_ITEMS_ON_PAGE
                    else
                        DEFAULT_ITEMS_ON_PAGE,
                    savedSortByParameter.sortBy,
                    if (performFilter)
                        ::filterNews
                    else
                        null,
                    performFilter
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        searchView.setOnCloseListener {
            currentSearchPage = DEFAULT_CURRENT_SEARCH_PAGE
            false
        }
        sortItem.setOnMenuItemClickListener {
            if (isSearch) {
                showSortDialog()
            } else {
                showToast(R.string.sortUnavailable)
            }
            false
        }
        filterItem.setOnMenuItemClickListener {
            if (isSearch) {
                showFilterDialog()
            } else {
                showToast(R.string.filterUnavailable)
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
            articles,
            availablePages,
            currentCountryPage,
            currentSearchPage,
            savedQuery,
            savedSortByParameter,
            isSearch,
            isFilter,
            savedFilterParameter,
            currentLayoutVariant
        )
        outState.apply {
            putParcelable(
                SAVED_ROTATION_MODEL_STRING,
                savedRotationModel
            )
        }
    }

    private fun showFullScreenNewsFragment(position: Int) {
        val activity = context as AppCompatActivity
        val bundle = Bundle().apply {
            putString(
                NEWS_URL_STRING,
                articles[position].url
            )
        }
        val mainScreenFragment =
            activity.supportFragmentManager.findFragmentByTag(MAIN_SCREEN_FRAGMENT_STRING) as MainScreenFragment
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
        val reset = filterDialog.findViewById<LinearLayout>(R.id.resetFilter)
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
        reset?.setOnClickListener {
            performFilterAction(FilterVariants.DEFAULT, filterDialog)
            highlightFilterAction(today, thisWeek, thisMonth)
        }
        filterDialog.show()
    }

    private fun highlightFilterAction(
        today: LinearLayout?,
        thisWeek: LinearLayout?,
        thisMonth: LinearLayout?,
    ) {
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

    private fun showToast(text: Int) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).apply {
            show()
        }
    }

    private fun chooseLayoutManager() {
        when (currentLayoutVariant) {
            LayoutVariants.AS_GRID -> {
                recyclerView.layoutManager = GridLayoutManager(context, ROW_NUMBER)
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
                    layoutManager = GridLayoutManager(context, ROW_NUMBER)
                    adapter = newsAdapter
                }
            }
        }
    }

    private fun resetAll() {
        articles.clear()
        newsAdapter.updateList(articles)
        currentCountryPage = ONE
        currentSearchPage = ONE
        savedSortByParameter = SortVariants.PUBLISHED_AT
        savedFilterParameter = FilterVariants.DEFAULT
        savedQuery = StringPool.EMPTY.value
        isSearch = false
        isFilter = false
        loadCountryNews()
    }

    private fun performSortAction(sortBy: SortVariants, bottomSheetDialog: BottomSheetDialog) {
        currentSearchPage = DEFAULT_CURRENT_SEARCH_PAGE
        articles.clear()
        newsAdapter.updateList(articles)
        savedSortByParameter = sortBy
        savedFilterParameter = FilterVariants.DEFAULT
        searchNews(
            savedQuery,
            DEFAULT_ITEMS_ON_PAGE,
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
        currentSearchPage = DEFAULT_CURRENT_SEARCH_PAGE
        val doFilter = filterBy != FilterVariants.DEFAULT
        searchNews(
            savedQuery,
            if (doFilter)
                FILTERING_ITEMS_ON_PAGE
            else
                DEFAULT_ITEMS_ON_PAGE,
            savedSortByParameter.sortBy,
            if (doFilter)
                ::filterNews
            else
                null,
            doFilter
        )
        articles.clear()
        newsAdapter.updateList(articles)
        hideMoreButton()
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
                    FilterVariants.TODAY -> date.dayOfYear.compareTo(currentDate.dayOfYear) == ZERO
                    FilterVariants.THIS_WEEK -> date.isBefore(currentDate) && date.isAfter(
                        currentDate.minusWeeks(ONE.toLong())
                    )
                    FilterVariants.THIS_MONTH -> date.isBefore(currentDate) && date.isAfter(
                        currentDate.minusMonths(ONE.toLong())
                    )
                    FilterVariants.DEFAULT -> false
                }
            }.collect(Collectors.toList())
        if (filteredNews.isEmpty())
            showToast(R.string.no_matching_results)
        return filteredNews as ArrayList<Article>
    }

    private fun clearFocus() {
        val view = requireActivity().currentFocus
        view?.clearFocus()
    }

    private fun loadCountryNews() {
        lifecycleScope.launch {
            showProgressBar()
            try {
                val news = withContext(Dispatchers.IO) {
                    newsRepository.getTopHeadlinesNews(StringPool.US.value, currentCountryPage++)
                }
                news.let {
                    recalculatePages(it)
                    articles.addAll(it.articles)
                    newsAdapter.updateList(articles)
                    hideMoreButton()
                }
            } catch (e: Throwable) {
                println(e)
                showToast(R.string.internet_error)
            } finally {
                hideProgressBar()
            }
        }
    }

    private fun searchNews(
        query: String?,
        pageSize: Int,
        sortBy: String?,
        filterNews: ((ArrayList<Article>, FilterVariants) -> ArrayList<Article>)?,
        isFilterAction: Boolean
    ) {
        lifecycleScope.launch {
            showProgressBar()
            try {
                isSearch = true
                if (!isFilterAction)
                    isFilter = false
                query?.let {
                    val news = withContext(Dispatchers.IO) {
                        newsRepository.getEverythingNews(it, pageSize, currentSearchPage++, sortBy)
                    }
                    news.let {
                        if (it.articles.isEmpty()) {
                            currentSearchPage = ONE
                            showToast(R.string.no_content)
                        } else {
                            recalculatePages(it)
                            val newArticles =
                                if (filterNews != null)
                                    filterNews(it.articles, savedFilterParameter)
                                else
                                    it.articles
                            articles.addAll(newArticles)
                            if (newArticles.isNotEmpty())
                                newsAdapter.updateList(articles)
                        }
                        hideMoreButton()
                    }
                }
            } catch (e: Throwable) {
                println(e)
                showToast(R.string.internet_error)
            } finally {
                hideProgressBar()
            }
        }
    }

    private fun showProgressBar() {
        if (isSearch && articles.isEmpty() ||
            !isSearch && articles.isEmpty() ||
            isFilter && articles.isNotEmpty()
        )
            progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }

    private fun showMoreButton() {
        showMoreButton.visibility = View.VISIBLE
    }

    private fun hideMoreButton() {
        showMoreButton.visibility = View.GONE
    }

    private fun recalculatePages(response: NewsResponse) {
        val fullPages: Int =
            if (response.totalResults > MAX_AVAILABLE_NEWS) MAX_AVAILABLE_NEWS / DEFAULT_ITEMS_ON_PAGE
            else response.totalResults / DEFAULT_ITEMS_ON_PAGE
        val lost: Int = response.totalResults % DEFAULT_ITEMS_ON_PAGE
        availablePages = fullPages + if (lost > 0) 1 else 0
    }
}