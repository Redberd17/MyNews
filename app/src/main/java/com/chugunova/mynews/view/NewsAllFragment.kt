package com.chugunova.mynews.view

import android.annotation.SuppressLint
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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugunova.mynews.R
import com.chugunova.mynews.mainscreenfragment.NewsAdapter
import com.chugunova.mynews.model.Article
import com.chugunova.mynews.model.NewsRepository
import com.chugunova.mynews.model.NewsRepository.Companion.availablePages
import com.chugunova.mynews.utils.FilterVariants
import com.chugunova.mynews.utils.LayoutVariants
import com.chugunova.mynews.utils.SortVariants
import com.chugunova.mynews.utils.StringPool
import com.chugunova.mynews.viewmodel.NewsAllFragmentFactory
import com.chugunova.mynews.viewmodel.NewsAllFragmentViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
import kotlinx.android.synthetic.main.main_fragment.tabLayout

class NewsAllFragment : Fragment() {

    private val layout = R.layout.main_fragment

    private lateinit var mNewsAllFragmentViewModel: NewsAllFragmentViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var showMoreButton: FloatingActionButton
    private lateinit var searchView: SearchView
    private lateinit var sortDialog: BottomSheetDialog
    private lateinit var filterDialog: BottomSheetDialog
    private lateinit var progressBar: ProgressBar
    private var toolbar: Menu? = null

    private var tabPosition = 0

    private val newsAdapter: NewsAdapter = NewsAdapter { item -> showFullScreenNewsFragment(item) }

    companion object {
        fun newInstance() = NewsAllFragment()
        private const val SCROLL_VERTICALLY_DIRECTION = 1
        private const val ROW_NUMBER = 2
        private const val DEFAULT_ITEMS_ON_PAGE = 20
        private const val FILTERING_ITEMS_ON_PAGE = 100
        private const val ZERO = 0
        private const val ONE = 1
        const val EDIT_NEWS = "editNews"
        const val SAVED_NEWS = "savedNews"
        const val AUTHOR_IS_EQUALS = "authorIsEquals"
        const val TAB_IS_SELECTED = "tabSelected"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mNewsAllFragmentViewModel = ViewModelProvider(
            requireActivity(),
            NewsAllFragmentFactory(requireActivity().application)
        )[NewsAllFragmentViewModel::class.java]
        mNewsAllFragmentViewModel.toastLiveData.observe(this, { it ->
            it.getContentIfNotHandled()?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                if (it == R.string.token_is_expired) {
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.container, LoginFragment.newInstance())
                        .commit()
                }
            }
        })
        mNewsAllFragmentViewModel.articlesLiveData.observe(requireActivity(), { articles ->
            articles.let { newsAdapter.updateList(it) }
        })
        mNewsAllFragmentViewModel.userNewsLiveData.observe(requireActivity(), { articles ->
            if (articles != null && tabPosition == 0) {
                newsAdapter.updateList(articles)
            }
        })
        mNewsAllFragmentViewModel.liveData.observe(this, {
            val tab: TabLayout.Tab? = tabLayout.getTabAt(it.tabLayout)
            tabLayout.selectTab(tab)

            hideProgressBar()
            hideMoreButton()
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*if (savedInstanceState != null) {
            savedInstanceState.putInt(TAB_IS_SELECTED, mNewsAllFragmentViewModel.savedRotationModel.tabLayout)
            tabPosition = savedInstanceState.getInt(TAB_IS_SELECTED)
        }*/

        tabPosition = mNewsAllFragmentViewModel.savedRotationModel.tabLayout

        recyclerView = view.findViewById(R.id.recyclerView)
        showMoreButton = view.findViewById(R.id.showMoreButton)
        progressBar = view.findViewById(R.id.mainProgressBar)

        recyclerView.apply {
            chooseLayoutManager()
            adapter = newsAdapter
        }

        showProgressBar()
        hideMoreButton()
        NewsAllFragmentViewModel.count = -NewsRepository.DEFAULT_ITEMS_ON_PAGE
        mNewsAllFragmentViewModel.chooseNews()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    hideMoreButton()
                    mNewsAllFragmentViewModel.savedRotationModel.tabLayout = 0
                    tabPosition = mNewsAllFragmentViewModel.savedRotationModel.tabLayout
                    mNewsAllFragmentViewModel.savedRotationModel.isUserNews = true
                    if (toolbar != null) {
                        setupUserToolbar()
                    }
                }
                if (tab?.position == 1) {
                    hideMoreButton()
                    mNewsAllFragmentViewModel.savedRotationModel.tabLayout = 1
                    tabPosition = mNewsAllFragmentViewModel.savedRotationModel.tabLayout
                    mNewsAllFragmentViewModel.savedRotationModel.isUserNews = false
                    if (toolbar != null) {
                        setupWorldToolbar()
                    }
                }

                mNewsAllFragmentViewModel.chooseNews()
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Handle tab reselect
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Handle tab unselect
            }
        })

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(SCROLL_VERTICALLY_DIRECTION)
                    && if (mNewsAllFragmentViewModel.liveData.value?.isSearch == true && mNewsAllFragmentViewModel.liveData.value?.isUserNews == false)
                        mNewsAllFragmentViewModel.liveData.value?.let { it.currentSearchPage < availablePages } == true
                    else if (mNewsAllFragmentViewModel.liveData.value?.isUserNews == true)
                        mNewsAllFragmentViewModel.liveData.value?.let { it.currentUserNewsPage < mNewsAllFragmentViewModel.availableUserNewsPages } == true
                    else mNewsAllFragmentViewModel.liveData.value?.let { it.currentCountryPage < availablePages } == true
                ) {
                    if (mNewsAllFragmentViewModel.liveData.value?.isFilter != true)
                        showMoreButton()
                } else {
                    hideMoreButton()
                }
            }
        })


        showMoreButton.setOnClickListener {
            if (mNewsAllFragmentViewModel.liveData.value?.isUserNews == true) {
                showProgressBar()
                hideMoreButton()
                mNewsAllFragmentViewModel.loadUserNews(true)
            } else if (mNewsAllFragmentViewModel.liveData.value?.savedQuery?.isNotEmpty() == true) {
                showProgressBar()
                hideMoreButton()
                mNewsAllFragmentViewModel.searchNews(
                    mNewsAllFragmentViewModel.liveData.value?.savedQuery,
                    DEFAULT_ITEMS_ON_PAGE,
                    mNewsAllFragmentViewModel.liveData.value?.savedSortByParameter,
                    filterNews = null,
                    isFilterAction = false,
                    isContinue = true
                )
            } else {
                hideMoreButton()
                mNewsAllFragmentViewModel.loadCountryNews(true)
            }
        }
    }

    private fun setupUserToolbar() {
        toolbar?.findItem(R.id.search)?.isVisible = false
        toolbar?.findItem(R.id.sort)?.isVisible = false
        toolbar?.findItem(R.id.filter)?.isVisible = false
        toolbar?.findItem(R.id.resetAll)?.isVisible = false
        toolbar?.findItem(R.id.addUserNews)?.isVisible = true
    }

    private fun setupWorldToolbar() {
        toolbar?.findItem(R.id.search)?.isVisible = true
        toolbar?.findItem(R.id.sort)?.isVisible = true
        toolbar?.findItem(R.id.filter)?.isVisible = true
        toolbar?.findItem(R.id.resetAll)?.isVisible = true
        toolbar?.findItem(R.id.addUserNews)?.isVisible = false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        toolbar = menu
        inflater.inflate(R.menu.menu_main_screen, menu)
        val searchItem = menu.findItem(R.id.search)
        val sortItem = menu.findItem(R.id.sort)
        val filterItem = menu.findItem(R.id.filter)
        val changeView = menu.findItem(R.id.changeView)
        val resetAllItem = menu.findItem(R.id.resetAll)
        val addNewsItem = menu.findItem(R.id.addUserNews)
        val logout = menu.findItem(R.id.logout)

        if (tabPosition == 0) {
            setupUserToolbar()
        } else if (tabPosition == 1) {
            setupWorldToolbar()
        }

        searchView = searchItem?.actionView as SearchView
        searchView.setOnSearchClickListener {
            searchView.setQuery(mNewsAllFragmentViewModel.liveData.value?.savedQuery, false)
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideMoreButton()
                val performFilter =
                    mNewsAllFragmentViewModel.liveData.value?.savedFilterParameter != FilterVariants.DEFAULT
                NewsAllFragmentViewModel.count = -NewsRepository.DEFAULT_ITEMS_ON_PAGE
                mNewsAllFragmentViewModel.searchNews(
                    query,
                    if (performFilter)
                        FILTERING_ITEMS_ON_PAGE
                    else
                        DEFAULT_ITEMS_ON_PAGE,
                    mNewsAllFragmentViewModel.liveData.value?.savedSortByParameter,
                    if (performFilter)
                        ::filterNews
                    else
                        null,
                    performFilter,
                    isContinue = false
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        searchView.setOnCloseListener {
            false
        }
        sortItem.setOnMenuItemClickListener {
            if (mNewsAllFragmentViewModel.liveData.value?.isSearch == true) {
                showSortDialog()
            } else {
                showToast(R.string.sortUnavailable)
            }
            false
        }
        filterItem.setOnMenuItemClickListener {
            if (mNewsAllFragmentViewModel.liveData.value?.isSearch == true) {
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
            mNewsAllFragmentViewModel.resetAll()
            false
        }
        addNewsItem.setOnMenuItemClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, AddUserNewsFragment.newInstance())
                .addToBackStack(null)
                .commit()
            false
        }
        logout.setOnMenuItemClickListener {
            searchItem.collapseActionView()
            mNewsAllFragmentViewModel.clearUserDetails()
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, LoginFragment.newInstance())
                .commit()
            true
        }
    }

    private fun showFullScreenNewsFragment(position: Int) {
        val activity = context as AppCompatActivity
        val bundle = Bundle().apply {
            val fullscreenNews =
                if (tabPosition == 0) mNewsAllFragmentViewModel.userNewsLiveData.value?.get(position) else
                    mNewsAllFragmentViewModel.articlesLiveData.value?.get(position)
            putParcelable(EDIT_NEWS, fullscreenNews)
            putBoolean(
                AUTHOR_IS_EQUALS, fullscreenNews?.author
                    ?.equals(mNewsAllFragmentViewModel.userLiveData.value?.peekContent()?.username) == true
            )
        }
        activity.supportFragmentManager.beginTransaction().apply {
            replace(R.id.container, NewsDetailFragment.newInstance().apply { arguments = bundle })
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

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun highlightSortAction(
        relevancy: LinearLayout?,
        popularity: LinearLayout?,
        publishedAt: LinearLayout?
    ) {
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(R.attr.bottomSheetStyle, typedValue, true)
        @ColorInt val color = typedValue.data
        when (mNewsAllFragmentViewModel.liveData.value?.savedSortByParameter) {
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

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun highlightFilterAction(
        today: LinearLayout?,
        thisWeek: LinearLayout?,
        thisMonth: LinearLayout?,
    ) {
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(R.attr.bottomSheetStyle, typedValue, true)
        @ColorInt val color = typedValue.data
        when (mNewsAllFragmentViewModel.liveData.value?.savedFilterParameter) {
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
        context?.let {
            Toast.makeText(it, text, Toast.LENGTH_SHORT).apply {
                show()
            }
        }
    }

    private fun chooseLayoutManager() {
        if (mNewsAllFragmentViewModel.liveData.value?.currentLayoutVariant == LayoutVariants.AS_LIST) {
            recyclerView.layoutManager = LinearLayoutManager(context)
        } else {
            recyclerView.layoutManager = GridLayoutManager(context, ROW_NUMBER)
        }
    }

    private fun changeLayoutManager() {
        when (mNewsAllFragmentViewModel.liveData.value?.currentLayoutVariant) {
            LayoutVariants.AS_GRID -> {
                mNewsAllFragmentViewModel.changeView(LayoutVariants.AS_LIST)
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = newsAdapter
                }
            }
            LayoutVariants.AS_LIST -> {
                mNewsAllFragmentViewModel.changeView(LayoutVariants.AS_GRID)
                recyclerView.apply {
                    layoutManager = GridLayoutManager(context, ROW_NUMBER)
                    adapter = newsAdapter
                }
            }
        }
    }

    private fun performSortAction(sortBy: SortVariants, bottomSheetDialog: BottomSheetDialog) {
        mNewsAllFragmentViewModel.searchNews(
            mNewsAllFragmentViewModel.liveData.value?.savedQuery,
            DEFAULT_ITEMS_ON_PAGE,
            sortBy,
            filterNews = null,
            isFilterAction = false,
            isContinue = false
        )
        bottomSheetDialog.dismiss()
        clearFocus()
    }

    private fun performFilterAction(
        filterBy: FilterVariants,
        bottomSheetDialog: BottomSheetDialog
    ) {
        val doFilter = filterBy != FilterVariants.DEFAULT
        mNewsAllFragmentViewModel.searchNews(
            mNewsAllFragmentViewModel.liveData.value?.savedQuery,
            if (doFilter)
                FILTERING_ITEMS_ON_PAGE
            else
                DEFAULT_ITEMS_ON_PAGE,
            mNewsAllFragmentViewModel.liveData.value?.savedSortByParameter,
            if (doFilter)
                ::filterNews
            else
                null,
            doFilter,
            isContinue = false
        )
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

    private fun showProgressBar() {
        if (mNewsAllFragmentViewModel.liveData.value?.isSearch == true && mNewsAllFragmentViewModel.articlesLiveData.value?.isEmpty() == true ||
            mNewsAllFragmentViewModel.liveData.value?.isSearch != true && mNewsAllFragmentViewModel.articlesLiveData.value?.isEmpty() == true ||
            mNewsAllFragmentViewModel.liveData.value?.isFilter == true && mNewsAllFragmentViewModel.articlesLiveData.value?.isNotEmpty() == true
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

}