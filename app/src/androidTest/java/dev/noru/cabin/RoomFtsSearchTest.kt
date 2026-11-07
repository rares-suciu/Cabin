package dev.noru.cabin

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.noru.cabin.data.local.db.AppDatabase
import dev.noru.cabin.data.local.dao.ArticleDao
import dev.noru.cabin.data.local.entity.ArticleEntity
import dev.noru.cabin.data.local.entity.ArticleStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RoomFtsSearchTest {

    private lateinit var database: AppDatabase
    private lateinit var articleDao: ArticleDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        articleDao = database.articleDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testFtsSearch() = runBlocking {
        val article1 = ArticleEntity(
            id = UUID.randomUUID().toString(),
            url = "https://example.com/1",
            canonicalUrl = null,
            title = "Kotlin Coroutines",
            byline = null,
            siteName = null,
            excerpt = null,
            lang = null,
            contentHtml = null,
            textContent = "This is a detailed article about how Kotlin Coroutines work under the hood.",
            wordCount = null,
            readingMinutes = null,
            leadImagePath = null,
            savedAt = System.currentTimeMillis(),
            readAt = null,
            archived = false,
            favorite = false,
            scrollPosition = 0,
            status = ArticleStatus.READY,
            error = null
        )

        val article2 = ArticleEntity(
            id = UUID.randomUUID().toString(),
            url = "https://example.com/2",
            canonicalUrl = null,
            title = "Jetpack Compose",
            byline = null,
            siteName = null,
            excerpt = null,
            lang = null,
            contentHtml = null,
            textContent = "Building UI with Jetpack Compose is declarative and fun.",
            wordCount = null,
            readingMinutes = null,
            leadImagePath = null,
            savedAt = System.currentTimeMillis(),
            readAt = null,
            archived = false,
            favorite = false,
            scrollPosition = 0,
            status = ArticleStatus.READY,
            error = null
        )

        articleDao.insertArticle(article1)
        articleDao.insertArticle(article2)

        val searchResult1 = articleDao.searchArticles("Coroutines").first()
        assertEquals(1, searchResult1.size)
        assertEquals(article1.id, searchResult1[0].id)

        val searchResult2 = articleDao.searchArticles("declarative").first()
        assertEquals(1, searchResult2.size)
        assertEquals(article2.id, searchResult2[0].id)

        val searchResult3 = articleDao.searchArticles("Kotlin OR Compose").first()
        assertTrue(searchResult3.size >= 1)
    }
}
