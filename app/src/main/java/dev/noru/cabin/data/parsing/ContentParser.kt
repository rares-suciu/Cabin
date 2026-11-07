package dev.noru.cabin.data.parsing

import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

class ContentParser {

    fun parse(url: String, html: String): ParsedArticle? {
        val readability = Readability4J(url, html)
        val article = readability.parse()

        if (article.content == null) {
            return null
        }

        val sanitizedHtml = sanitizeHtml(url, article.content!!)

        return ParsedArticle(
            title = article.title,
            byline = article.byline,
            excerpt = article.excerpt,
            siteName = null, // Readability4J article does not always expose siteName in all versions, or extract it explicitly
            contentHtml = sanitizedHtml.outerHtml(),
            textContent = article.textContent,
            readingTimeMinutes = calculateReadingTime(article.textContent)
        )
    }

    private fun sanitizeHtml(baseUri: String, htmlContent: String): Document {
        // Drop scripts, iframes, forms. Allow pictures, images.
        val safelist = Safelist.relaxed()
            .addTags("picture", "source")
            .addAttributes("source", "srcset", "media", "type")
            .addAttributes("img", "data-src", "lazy-load", "srcset", "sizes")
            .removeTags("script", "iframe", "form", "object", "embed")

        val cleanHtml = Jsoup.clean(htmlContent, baseUri, safelist)

        // Parse the cleaned HTML back to Document to resolve absolute URLs and handle images easily
        val doc = Jsoup.parse(cleanHtml, baseUri)

        // Remove tracking pixels
        doc.select("img[width=1], img[height=1], img[src*='doubleclick.net'], img[src*='google-analytics.com']").remove()

        return doc
    }

    private fun calculateReadingTime(text: String?): Int {
        if (text.isNullOrEmpty()) return 0
        val wordCount = text.split("\\s+".toRegex()).count { it.isNotBlank() }
        return maxOf(1, wordCount / 200) // Assuming ~200 wpm
    }

    data class ParsedArticle(
        val title: String?,
        val byline: String?,
        val excerpt: String?,
        val siteName: String?,
        val contentHtml: String,
        val textContent: String?,
        val readingTimeMinutes: Int
    )
}
