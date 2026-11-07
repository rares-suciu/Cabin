package dev.noru.cabin.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class NetworkFetcher(private val client: OkHttpClient) {

    @Throws(IOException::class)
    suspend fun fetchHtml(url: String): FetchResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }

            // Limit to 5MB as per requirements
            val contentLength = response.body?.contentLength() ?: -1L
            if (contentLength > 5 * 1024 * 1024) {
                throw IOException("File too large (> 5MB)")
            }

            val bodyString = response.body?.string() ?: ""
            if (bodyString.length > 5 * 1024 * 1024) {
                throw IOException("File too large (> 5MB)")
            }

            val finalUrl = response.request.url.toString()
            FetchResult(finalUrl, bodyString)
        }
    }

    data class FetchResult(
        val finalUrl: String,
        val html: String
    )
}
