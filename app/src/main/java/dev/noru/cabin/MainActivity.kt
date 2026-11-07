package dev.noru.cabin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.noru.cabin.data.repository.ArticleRepository
import dev.noru.cabin.ui.flightreadiness.FlightReadinessScreen
import dev.noru.cabin.ui.library.LibraryScreen
import dev.noru.cabin.ui.reader.ReaderScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val repository: ArticleRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            CabinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CabinApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text != null) {
                val url = extractUrl(text)
                if (url != null) {
                    // Use a GlobalScope or custom scope for fire-and-forget since we call finish()
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.enqueueSave(url)
                    }
                    Toast.makeText(this@MainActivity, "Saved for offline", Toast.LENGTH_SHORT).show()
                    finish() // Exit immediately as per requirements
                }
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val urlRegex = "(https?://[^\\s]+)".toRegex()
        var url = urlRegex.find(text)?.value

        if (url != null) {
            // Strip common tracking params
            val uri = android.net.Uri.parse(url)
            val builder = uri.buildUpon().clearQuery()
            for (param in uri.queryParameterNames) {
                if (!param.startsWith("utm_")) {
                    builder.appendQueryParameter(param, uri.getQueryParameter(param))
                }
            }
            url = builder.build().toString()
        }
        return url
    }
}

@Composable
fun CabinTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
fun CabinApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                onNavigateToReader = { id -> navController.navigate("reader/$id") },
                onNavigateToFlightReadiness = { navController.navigate("flight_readiness") }
            )
        }
        composable(
            route = "reader/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.StringType })
        ) {
            ReaderScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("flight_readiness") {
            FlightReadinessScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
