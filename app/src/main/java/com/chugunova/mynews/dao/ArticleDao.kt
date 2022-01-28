package com.chugunova.mynews.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chugunova.mynews.model.Article

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE typeOfQuery = :typeOfQuery LIMIT :count,20")
    fun getArticles(typeOfQuery: String, count: Int): List<Article>

    @Query("SELECT author, content, description, publishedAt, title, typeOfQuery, url, urlToImage FROM articles WHERE typeOfQuery = :typeOfQuery")
    fun getAllArticles(typeOfQuery: String): List<Article>

    @Insert
    fun insertAllArticles(articles: List<Article>)

    @Query("DELETE from articles WHERE typeOfQuery = :typeOfQuery")
    fun deleteUnusedArticles(typeOfQuery: String)

}