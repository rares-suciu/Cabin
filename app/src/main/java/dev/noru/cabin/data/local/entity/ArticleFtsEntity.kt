package dev.noru.cabin.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "articles_fts")
@Fts4(contentEntity = ArticleEntity::class)
data class ArticleFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int,
    val textContent: String?
)
