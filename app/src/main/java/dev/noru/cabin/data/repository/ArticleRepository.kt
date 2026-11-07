package dev.noru.cabin.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dev.noru.cabin.data.local.dao.ArticleDao
import dev.noru.cabin.data.local.entity.ArticleEntity
import dev.noru.cabin.data.local.entity.ArticleStatus
import dev.noru.cabin.worker.SaveArticleWorker
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class ArticleRepository(
    private val context: Context,
    private val articleDao: ArticleDao
) {
    fun getAllArticles(): Flow<List<ArticleEntity>> = articleDao.getAllArticles()
    fun getUnreadArticles(): Flow<List<ArticleEntity>> = articleDao.getUnreadArticles()
    fun getArchivedArticles(): Flow<List<ArticleEntity>> = articleDao.getArchivedArticles()
    fun getFavoriteArticles(): Flow<List<ArticleEntity>> = articleDao.getFavoriteArticles()
    fun searchArticles(query: String): Flow<List<ArticleEntity>> = articleDao.searchArticles(query)

    fun getArticleFlow(id: String): Flow<ArticleEntity?> = articleDao.getArticleFlow(id)

    suspend fun getArticleById(id: String): ArticleEntity? = articleDao.getArticleById(id)

    fun getCountByStatus(status: ArticleStatus): Flow<Int> = articleDao.getCountByStatus(status)

    suspend fun enqueueSave(url: String) {
        val existing = articleDao.getArticleByUrl(url)
        if (existing != null) {
            if (existing.status != ArticleStatus.READY) {
                // If it exists but failed, just requeue it without creating duplicate
                articleDao.updateArticle(existing.copy(status = ArticleStatus.QUEUED, error = null))
                enqueueWorker(existing.id)
            }
            return
        }

        val article = ArticleEntity(
            url = url,
            canonicalUrl = null,
            title = null,
            byline = null,
            siteName = null,
            excerpt = null,
            lang = null,
            contentHtml = null,
            textContent = null,
            wordCount = null,
            readingMinutes = null,
            leadImagePath = null,
            savedAt = System.currentTimeMillis(),
            readAt = null,
            archived = false,
            favorite = false,
            scrollPosition = 0,
            status = ArticleStatus.QUEUED,
            error = null
        )

        articleDao.insertArticle(article)

        enqueueWorker(article.id)
    }

    suspend fun retryFailed() {
        val failed = articleDao.getArticlesByStatus(ArticleStatus.FAILED)
        failed.forEach { article ->
            articleDao.updateArticle(article.copy(status = ArticleStatus.QUEUED, error = null))
            enqueueWorker(article.id)
        }
    }

    private fun enqueueWorker(articleId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SaveArticleWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .setInputData(
                Data.Builder()
                    .putString(SaveArticleWorker.KEY_ARTICLE_ID, articleId)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "save_article_$articleId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    suspend fun deleteArticles(ids: List<String>) {
        // Also cleanup local files
        ids.forEach { id ->
            val dir = java.io.File(context.filesDir, "articles/$id")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
        articleDao.deleteArticles(ids)
    }

    suspend fun updateArticle(article: ArticleEntity) {
        articleDao.updateArticle(article)
    }
}
