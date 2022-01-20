package com.chugunova.mynews.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chugunova.mynews.model.Article

@Database(entities = [Article::class], version = 1, exportSchema = false)
abstract class ArticleDatabase : RoomDatabase() {

    abstract fun articleDao(): ArticleDao

    companion object {
        private val sLock = Any()
        @Volatile
        private var instance: ArticleDatabase? = null

        fun getInstance(context: Context): ArticleDatabase {
            synchronized(sLock) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.applicationContext,
                            ArticleDatabase::class.java, "database").build()
                }
                return instance as ArticleDatabase
            }
        }
    }
}