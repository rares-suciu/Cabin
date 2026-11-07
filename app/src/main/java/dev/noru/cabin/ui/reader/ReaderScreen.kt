package dev.noru.cabin.ui.reader

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel()
) {
    val article by viewModel.article.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(article) {
        if (article != null && article?.readAt == null) {
            viewModel.markAsRead()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Can't reliably save scroll here as webview instance is lost, but the real approach
            // requires JS bridge or custom webchromeclient tracking, which is too complex for now.
            // Leaving as is since simple offline loading is the primary feature priority.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article?.title ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (article != null && article?.contentHtml != null) {
            AndroidView(
                modifier = Modifier.padding(padding).fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = false // No JS for safety and offline
                        settings.blockNetworkImage = true // Block network images
                        settings.blockNetworkLoads = true // Block network loads

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val url = request?.url?.toString() ?: return null

                                if (url.startsWith("cabin://local-image/")) {
                                    val parts = url.substring("cabin://local-image/".length).split("/")
                                    if (parts.size == 2) {
                                        val articleId = parts[0]
                                        val fileName = parts[1]
                                        val file = File(context.filesDir, "articles/$articleId/$fileName")
                                        if (file.exists()) {
                                            return WebResourceResponse(
                                                "image/webp",
                                                "UTF-8",
                                                FileInputStream(file)
                                            )
                                        }
                                    }
                                }

                                // Block all other network requests
                                return WebResourceResponse("text/plain", "UTF-8", null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // Only restore scroll position after the page has finished loading
                                article?.let {
                                    view?.scrollY = it.scrollPosition
                                }
                            }
                        }
                    }
                },
                update = { webView ->
                    val customCss = """
                        <style>
                            body { font-family: sans-serif; font-size: 18px; line-height: 1.6; padding: 16px; margin: 0; color: #333; background: #fff; }
                            img { max-width: 100%; height: auto; display: block; margin: 16px auto; }
                            @media (prefers-color-scheme: dark) {
                                body { color: #ddd; background: #121212; }
                            }
                        </style>
                    """.trimIndent()
                    val htmlWithCss = "$customCss${article!!.contentHtml}"
                    webView.loadDataWithBaseURL("cabin://local/", htmlWithCss, "text/html", "UTF-8", null)
                }
            )
        } else {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                if (article == null) {
                    CircularProgressIndicator()
                } else {
                    Text("Article content not available.")
                }
            }
        }
    }
}
