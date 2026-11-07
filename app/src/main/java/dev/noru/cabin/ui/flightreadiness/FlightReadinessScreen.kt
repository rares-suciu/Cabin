package dev.noru.cabin.ui.flightreadiness

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightReadinessScreen(
    onNavigateBack: () -> Unit,
    viewModel: FlightReadinessViewModel = koinViewModel()
) {
    val readyCount by viewModel.readyCount.collectAsState()
    val queuedCount by viewModel.queuedCount.collectAsState()
    val failedCount by viewModel.failedCount.collectAsState()
    val linkOnlyCount by viewModel.linkOnlyCount.collectAsState()

    val appStorageUsed by viewModel.appStorageUsed.collectAsState()
    val deviceStorageFree by viewModel.deviceStorageFree.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flight Readiness") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Articles Status", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Ready for offline: $readyCount")
            Text("Queued: $queuedCount")
            Text("Link only (Failed extraction): $linkOnlyCount")
            Text("Failed downloads: $failedCount", color = if (failedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.height(24.dp))

            Text("Storage Info", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("App Storage Used: ${formatSize(appStorageUsed)}")
            Text("Device Storage Free: ${formatSize(deviceStorageFree)}")

            Spacer(modifier = Modifier.height(24.dp))

            if (failedCount > 0 || queuedCount > 0) {
                Button(onClick = { viewModel.retryFailed() }) {
                    Text("Retry Failed / Queued")
                }
            }
        }
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
