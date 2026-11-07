package dev.noru.cabin.ui.flightreadiness

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.noru.cabin.data.local.entity.ArticleStatus
import dev.noru.cabin.data.repository.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class FlightReadinessViewModel(
    private val context: Context,
    private val repository: ArticleRepository
) : ViewModel() {

    val readyCount = repository.getCountByStatus(ArticleStatus.READY).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val queuedCount = repository.getCountByStatus(ArticleStatus.QUEUED).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val failedCount = repository.getCountByStatus(ArticleStatus.FAILED).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val linkOnlyCount = repository.getCountByStatus(ArticleStatus.LINK_ONLY).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _appStorageUsed = MutableStateFlow(0L)
    val appStorageUsed: StateFlow<Long> = _appStorageUsed

    private val _deviceStorageFree = MutableStateFlow(0L)
    val deviceStorageFree: StateFlow<Long> = _deviceStorageFree

    init {
        calculateStorage()
    }

    fun calculateStorage() {
        viewModelScope.launch {
            val appDir = File(context.filesDir, "articles")
            _appStorageUsed.value = appDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

            val stat = StatFs(Environment.getDataDirectory().path)
            _deviceStorageFree.value = stat.availableBlocksLong * stat.blockSizeLong
        }
    }

    fun retryFailed() {
        viewModelScope.launch {
            repository.retryFailed()
        }
    }
}
