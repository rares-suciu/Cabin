package dev.noru.cabin.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.noru.cabin.data.image.ImageDownloader
import dev.noru.cabin.data.local.dao.ArticleDao
import dev.noru.cabin.data.local.entity.ArticleStatus
import dev.noru.cabin.data.parsing.ContentParser
import dev.noru.cabin.data.remote.NetworkFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveArticleWorker(
    context: Context,
    params: WorkerParameters,
    private val articleDao: ArticleDao,
    private val networkFetcher: NetworkFetcher,
    private val contentParser: ContentParser,
    private val imageDownloader: ImageDownloader
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val articleId = inputData.getString(KEY_ARTICLE_ID)
            ?: return@withContext Result.failure()

        val article = articleDao.getArticleById(articleId)
            ?: return@withContext Result.failure()

        if (article.status == ArticleStatus.READY || article.status == ArticleStatus.LINK_ONLY) {
            return@withContext Result.success()
        }

        try {
            // Update status to SAVING
            articleDao.updateArticle(article.copy(status = ArticleStatus.SAVING))

            // Fetch HTML
            val fetchResult = networkFetcher.fetchHtml(article.url)

            // Parse Content
            val parsedArticle = contentParser.parse(fetchResult.finalUrl, fetchResult.html)

            if (parsedArticle == null) {
                // Parsing failed, save as LINK_ONLY
                articleDao.updateArticle(
                    article.copy(
                        status = ArticleStatus.LINK_ONLY,
                        canonicalUrl = fetchResult.finalUrl,
                        error = "Extraction failed. Available as link only."
                    )
                )
                return@withContext Result.success()
            }

            // Download images and rewrite HTML
            val processedImages = imageDownloader.processImages(
                articleId = article.id,
                baseUri = fetchResult.finalUrl,
                htmlContent = parsedArticle.contentHtml
            )

            // Save ready article
            articleDao.updateArticle(
                article.copy(
                    canonicalUrl = fetchResult.finalUrl,
                    title = parsedArticle.title,
                    byline = parsedArticle.byline,
                    siteName = parsedArticle.siteName,
                    excerpt = parsedArticle.excerpt,
                    contentHtml = processedImages.html,
                    textContent = parsedArticle.textContent,
                    wordCount = parsedArticle.textContent?.split("\\s+".toRegex())?.size,
                    readingMinutes = parsedArticle.readingTimeMinutes,
                    leadImagePath = processedImages.leadImagePath,
                    status = ArticleStatus.READY,
                    error = null
                )
            )

            Result.success()

        } catch (e: Exception) {
            // Update to FAILED
            articleDao.updateArticle(
                article.copy(
                    status = ArticleStatus.FAILED,
                    error = e.localizedMessage ?: "Unknown error"
                )
            )
            // We want it to be retry-able by WorkManager backoff
            Result.retry()
        }
    }

    companion object {
        const val KEY_ARTICLE_ID = "article_id"
    }
}
