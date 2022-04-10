package com.chugunova.mynews.model

import com.chugunova.mynews.dao.ArticleDao
import com.chugunova.mynews.model.api.ApiHelper
import com.chugunova.mynews.viewmodel.NewsAllFragmentViewModel.Companion.count
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewsRepository(private val apiHelper: ApiHelper, private var articleDao: ArticleDao) {

    companion object {
        const val DEFAULT_ITEMS_ON_PAGE = 20
        private const val MAX_AVAILABLE_NEWS = 100
        var availablePages = 0
    }

    private var availablePagesForDownloading: Int = 0

    suspend fun requestNews(isNetworkAvailable: Boolean,
                            isSearch: Boolean,
                            country: String,
                            page: Int,
                            q: String,
                            pageSize: Int,
                            sortBy: String?,
                            token: String): ArrayList<Article>? {

        return if (isNetworkAvailable) {
            count = -DEFAULT_ITEMS_ON_PAGE
            val response = if (isSearch) {
                apiHelper.getEverythingNews(q, pageSize, page, sortBy, token)
            } else {
                apiHelper.getTopHeadlinesNews(country, page, token)
            }
            if (response.isSuccessful) {
                val articles = response.body()?.articles
                if (articles != null) {
                    for (article in articles) {
                        article.typeOfQuery = q
                    }
                    withContext(Dispatchers.IO) {
                        recalculatePages(response.body()!!)
                        if (articleDao.getAllArticles(q).containsAll(articles)) {
                            if (availablePagesForDownloading != availablePages) {
                                articleDao.deleteUnusedArticles(q)
                                articleDao.insertAllArticles(articles)
                            }
                        } else {
                            if (availablePagesForDownloading != availablePages) {
                                articleDao.deleteUnusedArticles(q)
                            }
                            articleDao.insertAllArticles(articles)
                        }
                        availablePages = availablePagesForDownloading
                    }
                }
                articles
            } else {
                null
            }
        } else {
            withContext(Dispatchers.IO) {
                recalculatePages(articleDao.getAllArticles(q) as ArrayList<Article>)
                count += DEFAULT_ITEMS_ON_PAGE
                articleDao.getArticles(q, count) as ArrayList<Article>
            }
        }
    }

    private fun recalculatePages(response: LegacyNewsResponse) {
        availablePagesForDownloading =
                if (response.totalResults > MAX_AVAILABLE_NEWS) {
                    MAX_AVAILABLE_NEWS / DEFAULT_ITEMS_ON_PAGE
                } else {
                    val fullPages = response.totalResults / DEFAULT_ITEMS_ON_PAGE
                    val lost: Int = response.totalResults % DEFAULT_ITEMS_ON_PAGE
                    fullPages + if (lost > 0) 1 else 0
                }
    }

    private fun recalculatePages(articles: ArrayList<Article>) {
        val size = articles.size
        availablePages =
                if (size % DEFAULT_ITEMS_ON_PAGE == 0) {
                    size / DEFAULT_ITEMS_ON_PAGE
                } else {
                    size / DEFAULT_ITEMS_ON_PAGE + 1
                }
    }
}