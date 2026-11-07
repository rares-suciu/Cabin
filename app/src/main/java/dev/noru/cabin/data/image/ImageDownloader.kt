package dev.noru.cabin.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.security.MessageDigest

class ImageDownloader(
    private val context: Context,
    private val client: OkHttpClient
) {
    suspend fun processImages(
        articleId: String,
        baseUri: String,
        htmlContent: String
    ): ProcessedImagesResult = withContext(Dispatchers.IO) {
        val articleDir = File(context.filesDir, "articles/$articleId")
        if (!articleDir.exists()) {
            articleDir.mkdirs()
        }

        val document = Jsoup.parse(htmlContent, baseUri)
        val imageElements = document.select("img")

        var leadImagePath: String? = null

        for ((index, img) in imageElements.withIndex()) {
            var src = img.attr("abs:src")
            if (src.isEmpty()) src = img.attr("abs:data-src")

            if (src.isEmpty()) continue

            try {
                val localFileName = downloadAndScaleImage(src, articleDir)
                if (localFileName != null) {
                    // Update HTML to point to local file path handled by WebView interceptor
                    val localUri = "cabin://local-image/$articleId/$localFileName"
                    img.attr("src", localUri)
                    img.removeAttr("srcset")
                    img.removeAttr("data-src")

                    if (leadImagePath == null && index == 0) {
                        leadImagePath = File(articleDir, localFileName).absolutePath
                    }
                }
            } catch (e: Exception) {
                // Ignore download failures for individual images
                e.printStackTrace()
            }
        }

        // Handle <source> tags in <picture>
        document.select("source").forEach { source ->
            var srcset = source.attr("abs:srcset")
            if (srcset.isNotEmpty()) {
                 // Simplistic handling: take the first url in srcset
                 val firstUrl = srcset.split(",").firstOrNull()?.trim()?.split(" ")?.firstOrNull()
                 if (firstUrl != null) {
                      try {
                          val absoluteUrl = URI(baseUri).resolve(firstUrl).toString()
                          val localFileName = downloadAndScaleImage(absoluteUrl, articleDir)
                          if (localFileName != null) {
                              val localUri = "cabin://local-image/$articleId/$localFileName"
                              source.attr("srcset", localUri)
                          }
                      } catch (e: Exception) {
                          e.printStackTrace()
                      }
                 }
            }
        }

        ProcessedImagesResult(
            html = document.outerHtml(),
            leadImagePath = leadImagePath
        )
    }

    private suspend fun downloadAndScaleImage(url: String, dir: File): String? {
        val request = Request.Builder().url(url).build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null

            val contentType = response.header("Content-Type")
            if (contentType == null || !contentType.startsWith("image/")) return null

            val contentLength = response.body?.contentLength() ?: -1L
            if (contentLength > 10 * 1024 * 1024) return null // 10MB cap

            val inputStream = response.body?.byteStream() ?: return null

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            // We need to read into byte array because we decode twice
            val bytes = inputStream.readBytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            var inSampleSize = 1
            if (options.outWidth > 1280) {
                inSampleSize = options.outWidth / 1280
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

            val hash = hashUrl(url)
            val fileName = "$hash.webp"
            val file = File(dir, fileName)

            FileOutputStream(file).use { out ->
                // Compress to WebP
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                }
            }
            bitmap.recycle()
            fileName
        }
    }

    private fun hashUrl(url: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(url.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    data class ProcessedImagesResult(
        val html: String,
        val leadImagePath: String?
    )
}
