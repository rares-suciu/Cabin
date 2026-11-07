package dev.noru.cabin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ArticleStatus {
    QUEUED, SAVING, READY, FAILED, LINK_ONLY
}

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val url: String,
    val canonicalUrl: String?,
    val title: String?,
    val byline: String?,
    val siteName: String?,
    val excerpt: String?,
    val lang: String?,
    val contentHtml: String?,
    val textContent: String?,
    val wordCount: Int?,
    val readingMinutes: Int?,
    val leadImagePath: String?,
    val savedAt: Long,
    val readAt: Long?,
    val archived: Boolean,
    val favorite: Boolean,
    val scrollPosition: Int,
    val status: ArticleStatus,
    val error: String?
)
