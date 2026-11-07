package dev.noru.cabin.ui.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.noru.cabin.data.local.entity.ArticleEntity
import dev.noru.cabin.data.local.entity.ArticleStatus
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToReader: (String) -> Unit,
    onNavigateToFlightReadiness: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel()
) {
    val articles by viewModel.articles.collectAsState()
    val filter by viewModel.currentFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddUrlDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            viewModel.exportData(context, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importData(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cabin") },
                actions = {
                    Button(onClick = { exportLauncher.launch("cabin_export.zip") }) {
                        Text("Export")
                    }
                    Button(onClick = { importLauncher.launch(arrayOf("application/zip")) }) {
                        Text("Import")
                    }
                    Button(onClick = onNavigateToFlightReadiness) {
                        Text("Stats")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddUrlDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = { Text("Search") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterType.values().forEach { type ->
                    FilterChip(
                        selected = filter == type,
                        onClick = { viewModel.setFilter(type) },
                        label = { Text(type.name) }
                    )
                }
            }

            LazyColumn {
                items(articles) { article ->
                    ArticleCard(
                        article = article,
                        onClick = { onNavigateToReader(article.id) },
                        onArchive = { viewModel.archiveArticle(it) },
                        onDelete = { viewModel.deleteArticles(listOf(it.id)) }
                    )
                }
            }
        }

        if (showAddUrlDialog) {
            var url by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddUrlDialog = false },
                title = { Text("Add URL") },
                text = {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.addUrl(url)
                        showAddUrlDialog = false
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddUrlDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ArticleCard(
    article: ArticleEntity,
    onClick: () -> Unit,
    onArchive: (ArticleEntity) -> Unit,
    onDelete: (ArticleEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = article.title ?: article.url, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Status: ${article.status}", style = MaterialTheme.typography.bodySmall)
            if (article.status == ArticleStatus.FAILED) {
                Text(text = "Error: ${article.error}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { onArchive(article) }) { Text("Archive") }
                TextButton(onClick = { onDelete(article) }) { Text("Delete") }
            }
        }
    }
}
