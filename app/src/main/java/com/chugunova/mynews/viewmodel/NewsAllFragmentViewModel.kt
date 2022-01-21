package com.chugunova.mynews.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chugunova.mynews.dao.ArticleDatabase
import com.chugunova.mynews.model.Article
import com.chugunova.mynews.model.NewsRepository
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

    fun loadCountryNews() {
        viewModelScope.launch {
            try {
                savedRotationModel.currentCountryPage++
                val news = withContext(Dispatchers.IO) {
                    newsRepository.getTopHeadlinesNews(
                            StringPool.US.value,
                            savedRotationModel.currentCountryPage,
                            isOnline()
                    )
                }

                news.let {
                    liveData.postValue(savedRotationModel)
                    articlesLiveData.postValue(it)
                }

            } catch (e: Exception) {
                println(e)
                //                showToast(R.string.internet_error)
            }
        }
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