package dev.noru.cabin

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class ImageResolutionTest {

    @Test
    fun `resolves relative urls in img tags`() {
        val html = """<img src="/images/test.jpg">"""
        val baseUri = "https://example.com/article"

        val document = Jsoup.parse(html, baseUri)
        val img = document.selectFirst("img")
        val absUrl = img?.attr("abs:src")

        assertEquals("https://example.com/images/test.jpg", absUrl)
    }

    @Test
    fun `extracts data-src lazy load attribute`() {
        val html = """<img data-src="https://example.com/lazy.jpg">"""
        val baseUri = "https://example.com/article"

        val document = Jsoup.parse(html, baseUri)
        val img = document.selectFirst("img")
        val dataSrc = img?.attr("abs:data-src")

        assertEquals("https://example.com/lazy.jpg", dataSrc)
    }

    @Test
    fun `resolves srcset in picture source`() {
        val html = """
            <picture>
                <source srcset="/img/small.jpg 400w, /img/large.jpg 800w">
                <img src="/img/fallback.jpg">
            </picture>
        """.trimIndent()
        val baseUri = "https://example.com"

        val document = Jsoup.parse(html, baseUri)
        val source = document.selectFirst("source")
        val srcset = source?.attr("abs:srcset") ?: ""

        // Jsoup abs: resolves the first part if it recognizes it, but we can do it manually in logic
        val firstUrlPart = srcset.split(",").firstOrNull()?.trim()?.split(" ")?.firstOrNull() ?: ""
        val absoluteUrl = URI(baseUri).resolve(firstUrlPart).toString()

        assertEquals("https://example.com/img/small.jpg", absoluteUrl)
    }
}
