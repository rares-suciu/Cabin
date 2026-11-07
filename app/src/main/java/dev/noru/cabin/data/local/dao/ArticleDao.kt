package dev.noru.cabin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.noru.cabin.data.local.entity.ArticleEntity
import dev.noru.cabin.data.local.entity.ArticleStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Update
    suspend fun updateArticle(article: ArticleEntity)

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE id = :id")
    fun getArticleFlow(id: String): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles WHERE url = :url OR canonicalUrl = :url LIMIT 1")
    suspend fun getArticleByUrl(url: String): ArticleEntity?

    @Query("SELECT * FROM articles ORDER BY savedAt DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE archived = 0 ORDER BY savedAt DESC")
    fun getUnreadArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE archived = 1 ORDER BY savedAt DESC")
    fun getArchivedArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE favorite = 1 ORDER BY savedAt DESC")
    fun getFavoriteArticles(): Flow<List<ArticleEntity>>

    @Query("""
        SELECT articles.* FROM articles
        JOIN articles_fts ON articles.rowid = articles_fts.rowid
        WHERE articles_fts MATCH :query
        ORDER BY savedAt DESC
    """)
    fun searchArticles(query: String): Flow<List<ArticleEntity>>

    @Query("DELETE FROM articles WHERE id IN (:ids)")
    suspend fun deleteArticles(ids: List<String>)

    @Query("SELECT COUNT(*) FROM articles WHERE status = :status")
    fun getCountByStatus(status: ArticleStatus): Flow<Int>

    @Query("SELECT * FROM articles WHERE status = :status")
    suspend fun getArticlesByStatus(status: ArticleStatus): List<ArticleEntity>
}
