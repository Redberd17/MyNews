package com.chugunova.mynews.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chugunova.mynews.R
import com.chugunova.mynews.dao.ArticleDatabase
import com.chugunova.mynews.model.*
import com.chugunova.mynews.model.NewsRepository.Companion.DEFAULT_ITEMS_ON_PAGE
import com.chugunova.mynews.model.api.ApiHelper
import com.chugunova.mynews.model.api.ConfigRetrofit
import com.chugunova.mynews.service.NewsService
import com.chugunova.mynews.service.UserService
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
import java.net.SocketTimeoutException


class NewsAllFragmentViewModel(application: Application) : ViewModel() {

    val liveData = MutableLiveData<SavedRotationModel>()
    val articlesLiveData = MutableLiveData<ArrayList<Article>>()

    val userLiveData = MutableLiveData<Event<UserResponse>>()
    val toastLiveData = MutableLiveData<Event<Int>>()

    companion object {
        var count: Int = -DEFAULT_ITEMS_ON_PAGE
    }

    private val newsRepository = NewsRepository(
            ApiHelper(ConfigRetrofit.apiService),
            ArticleDatabase.getInstance(application).articleDao())

    private val newsService = NewsService()
    private val userService = UserService()

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

    private lateinit var token: String

    suspend fun saveUserNews(title: String, description: String, url: String, urlToImage: String) {
        viewModelScope.launch {
            val news = newsService.saveUserNews(token, NewsToServer(title, description, url, urlToImage)).body()
            val matchedArticle = news?.let { matchNewsToArticle(it) }
            matchedArticle.let {
                if (it != null) {
                    liveData.postValue(savedRotationModel)
                    articlesLiveData.value =
                            if (articlesLiveData.value == null) {
                                it
                            } else {
                                articlesLiveData.value?.toMutableList()?.apply {
                                    addAll(it)
                                } as ArrayList<Article>
                            }
                }
            }
        }
    }

    private fun matchNewsToArticle(news: NewsToServer): ArrayList<Article> {
        return arrayListOf(Article(null, null, news.author, news.title,
                news.description, news.url, news.urlToImage, news.publishedAt, null))
    }

    suspend fun saveAccount(authUser: AuthenticationUser) {
        viewModelScope.launch {
            try {
                val response = userService.createUser(authUser)
                if (response.isSuccessful) {
                    login(authUser)
                } else {
                    this@NewsAllFragmentViewModel.toastLiveData.value = Event(R.string.user_already_exist)
                }
            } catch (e: SocketTimeoutException) {
                this@NewsAllFragmentViewModel.toastLiveData.value = Event(R.string.no_connection_to_server)
            }
        }
    }

    suspend fun login(authUser: AuthenticationUser) {
        try {
            val response = userService.login(authUser)
            if (response.isSuccessful) {
                response.body()?.let {
                    val userResponse: UserResponse = it
                    this.userLiveData.value = Event(userResponse)
                    token = "Bearer_" + userResponse.token
                }
            } else {
                this.toastLiveData.value = Event(R.string.incorrect_credentials)
            }
        } catch (e: SocketTimeoutException) {
            this.toastLiveData.value = Event(R.string.no_connection_to_server)
        }
    }

    fun chooseNews(position: Int) {
        when (position) {
            0 -> loadUserNews()
            1 -> {
                savedRotationModel.currentCountryPage = 0
                loadCountryNews()
            }
        }
    }

    private fun loadUserNews() {
        viewModelScope.launch {
            val news = ConfigRetrofit.apiService.getAllUserNews(token)
            news.let {
                liveData.postValue(savedRotationModel)
                articlesLiveData.value =
                        if (articlesLiveData.value == null) {
                            it
                        } else {
                            articlesLiveData.value?.toMutableList()?.apply {
                                addAll(it)
                            } as ArrayList<Article>
                        }
            }
        }
    }

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
                            sortBy = StringPool.EMPTY.value,
                            token
                    )
                }

                news.let {
                    liveData.postValue(savedRotationModel)
                    articlesLiveData.value =
                            if (articlesLiveData.value == null) {
                                it
                            } else {
                                articlesLiveData.value?.toMutableList()?.apply {
                                    addAll(it)
                                } as ArrayList<Article>
                            }
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
                    articlesLiveData.value?.clear()
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
                                sortBy?.sortBy,
                                token
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
                            articlesLiveData.value =
                                    if (articlesLiveData.value == null) {
                                        newArticles
                                    } else {
                                        articlesLiveData.value?.toMutableList()?.apply {
                                            addAll(newArticles)
                                        } as ArrayList<Article>
                                    }
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
        articlesLiveData.value?.clear()
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

    fun clearArticleLiveData() {
        articlesLiveData.value?.clear()
    }

    fun clearUserDetails() {
        token = ""
    }

    private fun showToast(news: ArrayList<Article>) {
        if (news.isEmpty()) {
            if (isOnline()) {
                this@NewsAllFragmentViewModel.toastLiveData.value = Event(R.string.no_content)
            } else {
                this@NewsAllFragmentViewModel.toastLiveData.value = Event(R.string.no_matching_results)
            }
            articlesLiveData.postValue(arrayListOf())
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