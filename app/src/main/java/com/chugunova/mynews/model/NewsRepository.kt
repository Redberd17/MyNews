package com.chugunova.mynews.model

import androidx.annotation.WorkerThread
import com.chugunova.mynews.dao.ArticleDao
import com.chugunova.mynews.model.api.ApiHelper
import com.chugunova.mynews.utils.StringPool

class NewsRepository(private val apiHelper: ApiHelper, private var articleDao: ArticleDao) {

    companion object {
        private const val DEFAULT_ITEMS_ON_PAGE = 20
        private const val MAX_AVAILABLE_NEWS = 100
        var availablePages = 0
    }

    private var count: Int = -DEFAULT_ITEMS_ON_PAGE
    private var availablePagesForDownloading: Int = 0

    suspend fun getEverythingNews(
            q: String,
            pageSize: Int,
            page: Int,
            sortBy: String?
    ): NewsResponse {
        return apiHelper.getEverythingNews(q, pageSize, page, sortBy)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun getTopHeadlinesNews(country: String, page: Int, isNetworkAvailable: Boolean): ArrayList<Article> {

        return if (isNetworkAvailable) {
            val response = apiHelper.getTopHeadlinesNews(country, page)
            val articles = response.articles
            for (article in articles) {
                article.typeOfQuery = StringPool.US.value
            }
            recalculatePages(response)
            if (articleDao.getAllArticles(StringPool.US.value).containsAll(articles)) {
                if (availablePagesForDownloading != availablePages) {
                    articleDao.deleteUnusedArticles(StringPool.US.value)
                }
            } else {
                articleDao.insertAllArticles(articles)
            }
            availablePages = availablePagesForDownloading
            articles
        } else {
            recalculatePages(articleDao.getAllArticles(StringPool.US.value) as ArrayList<Article>)
            count += DEFAULT_ITEMS_ON_PAGE
            articleDao.getArticles(StringPool.US.value, count) as ArrayList<Article>
        }
    }

    private fun recalculatePages(response: NewsResponse) {
        val fullPages: Int =
                if (response.totalResults > MAX_AVAILABLE_NEWS) MAX_AVAILABLE_NEWS / DEFAULT_ITEMS_ON_PAGE
                else response.totalResults / DEFAULT_ITEMS_ON_PAGE
        val lost: Int = response.totalResults % DEFAULT_ITEMS_ON_PAGE
        availablePagesForDownloading = fullPages + if (lost > 0) 1 else 0
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