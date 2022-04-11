package com.chugunova.mynews.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.chugunova.mynews.R
import com.chugunova.mynews.dao.ArticleDatabase
import com.chugunova.mynews.model.*
import com.chugunova.mynews.model.NewsRepository.Companion.DEFAULT_ITEMS_ON_PAGE
import com.chugunova.mynews.model.NewsRepository.Companion.DEFAULT_USER_NEWS_ITEMS_ON_PAGE
import com.chugunova.mynews.model.api.ApiHelper
import com.chugunova.mynews.model.api.ConfigRetrofit
import com.chugunova.mynews.service.NewsService
import com.chugunova.mynews.service.UserService
import com.chugunova.mynews.utils.FilterVariants
import com.chugunova.mynews.utils.LayoutVariants
import com.chugunova.mynews.utils.SortVariants
import com.chugunova.mynews.utils.StringPool
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response


class NewsAllFragmentViewModel(application: Application) : AndroidViewModel(application) {

    val liveData = MutableLiveData<SavedRotationModel>()
    val articlesLiveData = MutableLiveData<ArrayList<Article>>()
    val userNewsLiveData = MutableLiveData<ArrayList<Article>?>()

    val userLiveData = MutableLiveData<Event<UserResponse>>()
    val toastLiveData = MutableLiveData<Event<Int>>()

    companion object {
        var count: Int = -DEFAULT_ITEMS_ON_PAGE
    }

    private val newsRepository = NewsRepository(
        ApiHelper(ConfigRetrofit.apiService),
        ArticleDatabase.getInstance(application).articleDao()
    )

    private val newsService = NewsService()
    private val userService = UserService()

    var availableUserNewsPages = 0

    var savedRotationModel = SavedRotationModel(
        0,
        0,
        0,
        0,
        savedQuery = StringPool.EMPTY.value,
        isSearch = false,
        isFilter = false,
        isUserNews = true,
        SortVariants.PUBLISHED_AT,
        FilterVariants.DEFAULT,
        LayoutVariants.AS_GRID
    )

    private lateinit var token: String

    suspend fun deleteUserNews(news: Article) {
        viewModelScope.launch {
            val response: Response<Unit>? = news.id?.let { newsService.deleteUserNews(token, it) }
            if (response != null) {
                if (response.isSuccessful) {
                    this@NewsAllFragmentViewModel.toastLiveData.value =
                        Event(R.string.news_successfully_deleted)
                    userNewsLiveData.value?.remove(news)
                    userNewsLiveData.postValue(userNewsLiveData.value)
                } else if (response.code() == 401) {
                    this@NewsAllFragmentViewModel.toastLiveData.value =
                        Event(R.string.token_is_expired)
                }
            }
        }
    }

    suspend fun saveUserNews(newsRequest: NewsRequest) {
        viewModelScope.launch {
            val newsResponse: Response<NewsResponse> = newsService.saveUserNews(token, newsRequest)
            if (newsResponse.isSuccessful) {
                this@NewsAllFragmentViewModel.toastLiveData.value =
                    Event(R.string.news_successfully_created)
            }
            proceedNewsResponse(newsResponse)
        }
    }

    suspend fun updateUserNews(newsId: Long, newsRequest: NewsRequest) {
        viewModelScope.launch {
            val newsResponse = newsService.updateUserNews(token, newsId, newsRequest)
            proceedNewsResponse(newsResponse)
        }
    }

    private fun proceedNewsResponse(newsResponse: Response<NewsResponse>) {
        if (newsResponse.isSuccessful && newsResponse.body() != null) {
            val convertedNews = convertNewsToArticle(newsResponse.body()!!)
            userNewsLiveData.value =
                if (userNewsLiveData.value == null) {
                    convertedNews
                } else {
                    userNewsLiveData.value?.toMutableList()?.apply {
                        addAll(convertedNews)
                    } as ArrayList<Article>?
                }
        } else if (newsResponse.code() == 401) {
            this@NewsAllFragmentViewModel.toastLiveData.value = Event(R.string.token_is_expired)
        }
    }

    private fun convertNewsToArticle(news: NewsResponse): ArrayList<Article> {
        return arrayListOf(
            Article(
                news.id, null, news.author, news.title,
                news.description, news.url, news.urlToImage, news.publishedAt
            )
        )
    }

    suspend fun saveAccount(authUser: AuthenticationUser) {
        viewModelScope.launch {
            try {
                val response = userService.createUser(authUser)
                if (response.isSuccessful) {
                    this@NewsAllFragmentViewModel.toastLiveData.value =
                        Event(R.string.user_successfully_created)
                    login(authUser)
                } else {
                    this@NewsAllFragmentViewModel.toastLiveData.value =
                        Event(R.string.user_already_exist)
                }
            } catch (e: SocketTimeoutException) {
                this@NewsAllFragmentViewModel.toastLiveData.value =
                    Event(R.string.no_connection_to_server)
            }
        }
    }

    suspend fun login(authUser: AuthenticationUser) {
        try {
            if (isOnline()) {
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
            } else {
                this.toastLiveData.value = Event(R.string.no_internet)
            }
        } catch (e: SocketTimeoutException) {
            this.toastLiveData.value = Event(R.string.no_connection_to_server)
        }
    }

    fun chooseNews() {
        when (savedRotationModel.tabLayout) {
            0 -> {
                loadUserNews(false)
            }
            1 -> {
                loadCountryNews(false)
            }
        }
    }

    fun loadUserNews(isShowMoreButtonPressed: Boolean) {
        viewModelScope.launch {
            if (userNewsLiveData.value.isNullOrEmpty() || isShowMoreButtonPressed) {
                savedRotationModel.currentUserNewsPage++
                val response = ConfigRetrofit.apiService.getUserNewsPaginate(
                    token,
                    DEFAULT_USER_NEWS_ITEMS_ON_PAGE,
                    savedRotationModel.currentUserNewsPage
                )
                if (response.isSuccessful) {
                    val news = response.body()
                    if (news != null) {
                        recalculateUserNewsPages(news)
                        liveData.postValue(savedRotationModel)
                        userNewsLiveData.value =
                            if (userNewsLiveData.value == null) {
                                news.articles
                            } else {
                                userNewsLiveData.value?.toMutableList()?.apply {
                                    addAll(news.articles)
                                } as ArrayList<Article>
                            }
                    }
                } else if (response.code() == 401) {
                    this@NewsAllFragmentViewModel.toastLiveData.value =
                        Event(R.string.token_is_expired)
                }
            } else {
                userNewsLiveData.value = userNewsLiveData.value
            }
        }
    }

    fun loadCountryNews(isShowMoreButtonPressed: Boolean) {
        val myExecutor = Executors.newSingleThreadExecutor()
        val myHandler = Handler(Looper.getMainLooper())
        if (articlesLiveData.value.isNullOrEmpty() || isShowMoreButtonPressed) {
            myExecutor.execute {
                viewModelScope.launch {
                    savedRotationModel.currentCountryPage++
                    val news = newsRepository.requestNews(
                        isOnline(),
                        isSearch = false,
                        StringPool.US.value,
                        savedRotationModel.currentCountryPage,
                        q = StringPool.US.value,
                        0,
                        sortBy = StringPool.EMPTY.value,
                        token
                    )
                    myHandler.post {
                        if (news == null) {
                            this@NewsAllFragmentViewModel.toastLiveData.value =
                                Event(R.string.token_is_expired)
                            savedRotationModel.currentCountryPage = 0
                            savedRotationModel.tabLayout = 0
                        } else {
                            liveData.postValue(savedRotationModel)
                            articlesLiveData.value =
                                if (articlesLiveData.value == null) {
                                    news
                                } else {
                                    articlesLiveData.value?.toMutableList()?.apply {
                                        addAll(news)
                                    } as ArrayList<Article>
                                }
                        }
                    }
                }
            }
        } else {
            articlesLiveData.value = articlesLiveData.value
        }
    }

    private fun recalculateUserNewsPages(response: LegacyNewsResponse) {
        val fullPages = response.totalResults / DEFAULT_USER_NEWS_ITEMS_ON_PAGE
        val lost: Int = response.totalResults % DEFAULT_USER_NEWS_ITEMS_ON_PAGE
        availableUserNewsPages = fullPages + if (lost > 0) 1 else 0
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
                    if (news == null) {
                        this@NewsAllFragmentViewModel.toastLiveData.value =
                            Event(R.string.token_is_expired)
                    } else {
                        if (news.isEmpty()) {
                            savedRotationModel.currentSearchPage = 1
                        } else {
                            val newArticles =
                                if (filterNews != null)
                                    filterNews(news, savedRotationModel.savedFilterParameter)
                                else
                                    news
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
                        showToast(news)
                    }
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
        loadCountryNews(false)
    }

    private fun clearArticleLiveData() {
        userNewsLiveData.value?.clear()
        articlesLiveData.value?.clear()
    }

    fun clearUserDetails() {
        userNewsLiveData.value?.clear()
        articlesLiveData.value?.clear()
        savedRotationModel = SavedRotationModel(
            0,
            0,
            0,
            0,
            savedQuery = StringPool.EMPTY.value,
            isSearch = false,
            isFilter = false,
            isUserNews = true,
            SortVariants.PUBLISHED_AT,
            FilterVariants.DEFAULT,
            LayoutVariants.AS_GRID
        )
        token = ""
    }

    private fun showToast(news: ArrayList<Article>) {
        if (news.isEmpty()) {
            if (isOnline()) {
                this@NewsAllFragmentViewModel.toastLiveData.value = Event(R.string.no_content)
            } else {
                this@NewsAllFragmentViewModel.toastLiveData.value =
                    Event(R.string.no_matching_results)
            }
            articlesLiveData.postValue(arrayListOf())
        }
    }

    private fun executeInBackground() {
        val myExecutor = Executors.newSingleThreadExecutor()
        val myHandler = Handler(Looper.getMainLooper())
    }

    fun changeView(typeOfView: LayoutVariants) {
        savedRotationModel.currentLayoutVariant = typeOfView
        liveData.postValue(savedRotationModel)
    }

    private fun isOnline(): Boolean {
        val cm =
            getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = cm.activeNetwork ?: return false
        val netInfo = cm.getNetworkCapabilities(networkCapabilities)
        return netInfo != null && netInfo.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && netInfo.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}