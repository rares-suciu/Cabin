package dev.noru.cabin.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.noru.cabin.data.local.entity.ArticleEntity
import dev.noru.cabin.data.repository.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReaderViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ArticleRepository
) : ViewModel() {

    private val articleId: String = checkNotNull(savedStateHandle["articleId"])

    val article: StateFlow<ArticleEntity?> = repository.getArticleFlow(articleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun markAsRead() {
        viewModelScope.launch {
            val current = article.value
            if (current != null && current.readAt == null) {
                repository.updateArticle(current.copy(readAt = System.currentTimeMillis()))
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val current = article.value
            if (current != null) {
                repository.updateArticle(current.copy(favorite = !current.favorite))
            }
        }
    }

    fun toggleArchive() {
        viewModelScope.launch {
            val current = article.value
            if (current != null) {
                repository.updateArticle(current.copy(archived = !current.archived))
            }
        }
    }

    fun updateScrollPosition(position: Int) {
        viewModelScope.launch {
            val current = article.value
            if (current != null && current.scrollPosition != position) {
                repository.updateArticle(current.copy(scrollPosition = position))
            }
        }
    }
}
