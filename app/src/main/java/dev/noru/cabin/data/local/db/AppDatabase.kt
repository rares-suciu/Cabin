package dev.noru.cabin.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.noru.cabin.data.local.dao.ArticleDao
import dev.noru.cabin.data.local.entity.ArticleEntity
import dev.noru.cabin.data.local.entity.ArticleFtsEntity

@Database(
    entities = [ArticleEntity::class, ArticleFtsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
}
