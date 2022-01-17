package com.chugunova.mynews.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.chugunova.mynews.model.NewsRepository
import com.chugunova.mynews.model.NewsResponse
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

class NewsAllFragmentViewModel(
    application: Application
) :
    AndroidViewModel(application) {

    val liveData = MutableLiveData<SavedRotationModel>()

    private val newsRepository = NewsRepository(ApiHelper(ConfigRetrofit.apiService))
    private var savedRotationModel = SavedRotationModel(
        arrayListOf(),
        0,
        0,
        0,
        savedQuery = StringPool.EMPTY.value,
        isSearch = false,
        isFilter = false,
        SortVariants.PUBLISHED_AT,
        FilterVariants.DEFAULT,
        LayoutVariants.AS_GRID
    )

    companion object {
        private const val DEFAULT_ITEMS_ON_PAGE = 20
        private const val MAX_AVAILABLE_NEWS = 100
    }

    fun loadCountryNews() {
        viewModelScope.launch {
            try {
                savedRotationModel.currentCountryPage++
                val news = withContext(Dispatchers.IO) {
                    newsRepository.getTopHeadlinesNews(
                        StringPool.US.value,
                        savedRotationModel.currentCountryPage
                    )
                }

                news.let {
                    recalculatePages(it)
                    for (article in it.articles) {
                        article.typeOfQuery = "us"
                    }

                    savedRotationModel.articles.addAll(it.articles)
                    liveData.postValue(savedRotationModel)
                }

            } catch (e: Exception) {
                println(e)
                //                showToast(R.string.internet_error)
            }
        }
    }

    private fun recalculatePages(response: NewsResponse) {
        val fullPages: Int =
            if (response.totalResults > MAX_AVAILABLE_NEWS) MAX_AVAILABLE_NEWS / DEFAULT_ITEMS_ON_PAGE
            else response.totalResults / DEFAULT_ITEMS_ON_PAGE
        val lost: Int = response.totalResults % DEFAULT_ITEMS_ON_PAGE
        savedRotationModel.availablePages = fullPages + if (lost > 0) 1 else 0
    }

}