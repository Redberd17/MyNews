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
import kotlinx.android.synthetic.main.main_fragment.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

class NewsAllFragment : Fragment() {

    private val layout = R.layout.main_fragment

    private lateinit var mNewsAllFragmentViewModel: NewsAllFragmentViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var showMoreButton: FloatingActionButton
    private lateinit var searchView: SearchView
    private lateinit var sortDialog: BottomSheetDialog
    private lateinit var filterDialog: BottomSheetDialog
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: Menu

    private var position = 0

    private val newsAdapter: NewsAdapter = NewsAdapter { item -> showFullScreenNewsFragment(item) }

    companion object {
        fun newInstance() = NewsAllFragment()
        private const val SCROLL_VERTICALLY_DIRECTION = 1
        private const val ROW_NUMBER = 2
        private const val DEFAULT_ITEMS_ON_PAGE = 20
        private const val FILTERING_ITEMS_ON_PAGE = 100
        private const val ZERO = 0
        private const val ONE = 1
        const val NEWS_URL_STRING = "newsUrl"
        val TAG = NewsAllFragment::class.java.name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        //get view model from provider
        mNewsAllFragmentViewModel = ViewModelProvider(requireActivity(),
                NewsAllFragmentFactory(requireActivity().application)
        )[NewsAllFragmentViewModel::class.java]
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNewsAllFragmentViewModel.toastLiveData.observe(requireActivity(), {})

        mNewsAllFragmentViewModel.articlesLiveData.observe(viewLifecycleOwner, { articles ->
            newsAdapter.updateList(articles)
            if (articles.isEmpty()) {
                mNewsAllFragmentViewModel.toastLiveData.value?.let { toast -> showToast(toast) }
            }
        })

        mNewsAllFragmentViewModel.liveData.observe(viewLifecycleOwner, {
            hideProgressBar()
            hideMoreButton()
        })

        recyclerView = view.findViewById(R.id.recyclerView)
        showMoreButton = view.findViewById(R.id.showMoreButton)
        progressBar = view.findViewById(R.id.mainProgressBar)

        recyclerView.apply {
            chooseLayoutManager()
            adapter = newsAdapter
        }

        if (mNewsAllFragmentViewModel.articlesLiveData.value == null) {
            showProgressBar()
            hideMoreButton()
            NewsAllFragmentViewModel.count = -NewsRepository.DEFAULT_ITEMS_ON_PAGE
            mNewsAllFragmentViewModel.chooseNews(position)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    // need to get user news here
                    position = 0
                    mNewsAllFragmentViewModel.chooseNews(position)
                    setupUserToolbar()
                }
                if (tab?.position == 1) {
                    // need to get world news here
                    position = 1
                    mNewsAllFragmentViewModel.chooseNews(position)
                    setupWorldToolbar()
                }
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
                        && if (mNewsAllFragmentViewModel.liveData.value?.isSearch == true)
                            mNewsAllFragmentViewModel.liveData.value?.let { it.currentSearchPage < availablePages } == true
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
            if (mNewsAllFragmentViewModel.liveData.value?.savedQuery?.isNotEmpty() == true) {
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
                mNewsAllFragmentViewModel.loadCountryNews()
            }
        }
    }

    private fun setupUserToolbar() {
        toolbar.findItem(R.id.search).isVisible = false
        toolbar.findItem(R.id.sort).isVisible = false
        toolbar.findItem(R.id.filter).isVisible = false
        toolbar.findItem(R.id.changeView).isVisible = false
        toolbar.findItem(R.id.resetAll).isVisible = false

        toolbar.findItem(R.id.addUserNews).isVisible = true
    }

    private fun setupWorldToolbar() {
        toolbar.findItem(R.id.search).isVisible = true
        toolbar.findItem(R.id.sort).isVisible = true
        toolbar.findItem(R.id.filter).isVisible = true
        toolbar.findItem(R.id.changeView).isVisible = true
        toolbar.findItem(R.id.resetAll).isVisible = true

        toolbar.findItem(R.id.addUserNews).isVisible = false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        toolbar = menu
        inflater.inflate(R.menu.menu, menu)
        val searchItem = menu.findItem(R.id.search)
        val sortItem = menu.findItem(R.id.sort)
        val filterItem = menu.findItem(R.id.filter)
        val changeView = menu.findItem(R.id.changeView)
        val resetAllItem = menu.findItem(R.id.resetAll)
        val addNewsItem = menu.findItem(R.id.addUserNews)
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
            requireActivity().supportFragmentManager.findFragmentByTag(TAG)?.let { oldFragment ->
                activity?.supportFragmentManager
                        ?.beginTransaction()
                        ?.replace(oldFragment.id, AddUserNewsFragment.newInstance(), AddUserNewsFragment.TAG)
                        ?.addToBackStack(null)
                        ?.commit()
            }
            false
        }
    }

    private fun showFullScreenNewsFragment(position: Int) {
        val activity = context as AppCompatActivity
        val bundle = Bundle().apply {
            putString(
                    NEWS_URL_STRING,
                    mNewsAllFragmentViewModel.articlesLiveData.value?.get(position)?.url
            )
        }
        val newAllFragment =
                activity.supportFragmentManager.findFragmentByTag(TAG) as NewsAllFragment
        activity.supportFragmentManager.beginTransaction().apply {
            replace(
                    newAllFragment.id,
                    NewsDetailFragment.newInstance().apply { arguments = bundle })
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