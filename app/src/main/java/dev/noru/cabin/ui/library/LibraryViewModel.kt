package dev.noru.cabin.ui.library

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.noru.cabin.data.local.entity.ArticleEntity
import dev.noru.cabin.data.repository.ArticleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class FilterType {
    ALL, UNREAD, ARCHIVED, FAVORITES
}

class LibraryViewModel(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _currentFilter = MutableStateFlow(FilterType.UNREAD)
    val currentFilter: StateFlow<FilterType> = _currentFilter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val articles: StateFlow<List<ArticleEntity>> = kotlinx.coroutines.flow.combine(_currentFilter, _searchQuery) { filter, query ->
        Pair(filter, query)
    }.flatMapLatest { (filter, query) ->
        if (query.isNotBlank()) {
            repository.searchArticles(query)
        } else {
            when (filter) {
                FilterType.ALL -> repository.getAllArticles()
                FilterType.UNREAD -> repository.getUnreadArticles()
                FilterType.ARCHIVED -> repository.getArchivedArticles()
                FilterType.FAVORITES -> repository.getFavoriteArticles()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filterType: FilterType) {
        _currentFilter.value = filterType
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun archiveArticle(article: ArticleEntity) {
        viewModelScope.launch {
            repository.updateArticle(article.copy(archived = true))
        }
    }

    fun unarchiveArticle(article: ArticleEntity) {
        viewModelScope.launch {
            repository.updateArticle(article.copy(archived = false))
        }
    }

    fun toggleFavorite(article: ArticleEntity) {
        viewModelScope.launch {
            repository.updateArticle(article.copy(favorite = !article.favorite))
        }
    }

    fun deleteArticles(ids: List<String>) {
        viewModelScope.launch {
            repository.deleteArticles(ids)
        }
    }

    fun addUrl(url: String) {
        viewModelScope.launch {
            repository.enqueueSave(url)
        }
    }

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        ZipOutputStream(os).use { zos ->
                            // Zip database
                            val dbFile = context.getDatabasePath("cabin.db")
                            if (dbFile.exists()) {
                                zos.putNextEntry(ZipEntry("cabin.db"))
                                FileInputStream(dbFile).use { it.copyTo(zos) }
                                zos.closeEntry()
                            }

                            // Zip images
                            val articlesDir = File(context.filesDir, "articles")
                            if (articlesDir.exists()) {
                                articlesDir.walkTopDown().filter { it.isFile }.forEach { file ->
                                    val entryName = "articles/" + file.relativeTo(articlesDir).path
                                    zos.putNextEntry(ZipEntry(entryName))
                                    FileInputStream(file).use { it.copyTo(zos) }
                                    zos.closeEntry()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Export", "Export failed", e)
                }
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        ZipInputStream(input).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                if (entry.name == "cabin.db") {
                                    val dbFile = context.getDatabasePath("cabin.db")
                                    FileOutputStream(dbFile).use { zis.copyTo(it) }
                                } else if (entry.name.startsWith("articles/")) {
                                    val outFile = File(context.filesDir, entry.name)
                                    outFile.parentFile?.mkdirs()
                                    FileOutputStream(outFile).use { zis.copyTo(it) }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Import", "Import failed", e)
                }
            }
        }
    }
}
