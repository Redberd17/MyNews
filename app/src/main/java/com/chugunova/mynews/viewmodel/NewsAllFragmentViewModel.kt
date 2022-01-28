package com.chugunova.mynews.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chugunova.mynews.R
import com.chugunova.mynews.dao.ArticleDatabase
import com.chugunova.mynews.model.Article
import com.chugunova.mynews.model.NewsRepository
import com.chugunova.mynews.model.NewsRepository.Companion.DEFAULT_ITEMS_ON_PAGE
import com.chugunova.mynews.model.SavedRotationModel
import com.chugunova.mynews.model.api.ApiHelper
import com.chugunova.mynews.model.api.ConfigRetrofit
import com.chugunova.mynews.utils.FilterVariants
import com.chugunova.mynews.utils.LayoutVariants
import com.chugunova.mynews.utils.SortVariants
import com.chugunova.mynews.utils.StringPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

class NewsAllFragmentViewModel(application: Application) : ViewModel() {

    val liveData = MutableLiveData<SavedRotationModel>()
    val articlesLiveData = MutableLiveData<ArrayList<Article>>()
    val toastLiveData = MutableLiveData<Int>()

    companion object {
        var count: Int = -DEFAULT_ITEMS_ON_PAGE
    }

    private val newsRepository = NewsRepository(
            ApiHelper(ConfigRetrofit.apiService),
            ArticleDatabase.getInstance(application).articleDao())

    private var savedRotationModel = SavedRotationModel(
            0,
            0,
            savedQuery = StringPool.EMPTY.value,
            isSearch = false,
            isFilter = false,
            SortVariants.PUBLISHED_AT,
            FilterVariants.DEFAULT,
            LayoutVariants.AS_GRID
    )

    private var articles: ArrayList<Article> = arrayListOf()

    fun loadCountryNews() {
        viewModelScope.launch {
            try {
                savedRotationModel.currentCountryPage++
                val news = withContext(Dispatchers.IO) {
                    newsRepository.requestNews(
                            isOnline(),
                            isSearch = false,
                            StringPool.US.value,
                            savedRotationModel.currentCountryPage,
                            q = StringPool.US.value,
                            0,
                            sortBy = StringPool.EMPTY.value
                    )
                }

                news.let {
                    liveData.postValue(savedRotationModel)
                    articles.addAll(it)
                    articlesLiveData.postValue(articles)
                }
                showToast(news)
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    fun searchNews(
            query: String?,
            pageSize: Int,
            sortBy: SortVariants?,
            filterNews: ((ArrayList<Article>, FilterVariants) -> ArrayList<Article>)?,
            isFilterAction: Boolean,
            isContinue: Boolean
    ) {
        viewModelScope.launch {
            try {
                if (!isContinue) {
                    savedRotationModel.currentSearchPage = 0
                    articles.clear()
                }
                savedRotationModel.currentSearchPage++
                savedRotationModel.isSearch = true
                if (!isFilterAction)
                    savedRotationModel.isFilter = false
                query?.let {
                    val news = withContext(Dispatchers.IO) {
                        newsRepository.requestNews(
                                isOnline(),
                                isSearch = true,
                                StringPool.EMPTY.value,
                                savedRotationModel.currentSearchPage,
                                q = it,
                                pageSize,
                                sortBy?.sortBy
                        )
                    }
                    news.let {
                        if (it.isEmpty()) {
                            savedRotationModel.currentSearchPage = 1
                        } else {
                            val newArticles =
                                    if (filterNews != null)
                                        filterNews(it, savedRotationModel.savedFilterParameter)
                                    else
                                        it
                            if (sortBy != null) {
                                savedRotationModel.savedSortByParameter = sortBy
                            }
                            savedRotationModel.savedQuery = query
                            liveData.postValue(savedRotationModel)
                            articles.addAll(newArticles)
                            articlesLiveData.postValue(articles)
                        }
                    }
                    showToast(news)
                }
            } catch (e: Throwable) {
                println(e)
            }
        }
    }

    fun resetAll() {
        articles = arrayListOf()
        savedRotationModel.currentCountryPage = 0
        savedRotationModel.currentSearchPage = 0
        savedRotationModel.savedSortByParameter = SortVariants.PUBLISHED_AT
        savedRotationModel.savedFilterParameter = FilterVariants.DEFAULT
        savedRotationModel.savedQuery = StringPool.EMPTY.value
        savedRotationModel.isSearch = false
        savedRotationModel.isFilter = false
        count = -DEFAULT_ITEMS_ON_PAGE
        loadCountryNews()
    }

    private suspend fun showToast(news: ArrayList<Article>) {
        withContext(Dispatchers.IO) {
            if (news.isEmpty()) {
                if (isOnline()) {
                    toastLiveData.postValue(R.string.no_content)
                } else {
                    toastLiveData.postValue(R.string.no_matching_results)
                }
                articlesLiveData.postValue(arrayListOf())
            }
        }
    }

    fun changeView(typeOfView: LayoutVariants) {
        savedRotationModel.currentLayoutVariant = typeOfView
        liveData.postValue(savedRotationModel)
    }

    private fun isOnline(): Boolean {
        return try {
            val timeoutMs = 1500
            val sock = Socket()
            val socked: SocketAddress = InetSocketAddress("8.8.8.8", 53)
            sock.connect(socked, timeoutMs)
            sock.close()
            true
        } catch (e: IOException) {
            false
        }
    }
}