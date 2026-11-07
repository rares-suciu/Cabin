package dev.noru.cabin

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlExtractionTest {

    private fun extractUrl(text: String): String? {
        val urlRegex = "(https?://[^\\s]+)".toRegex()
        var url = urlRegex.find(text)?.value

        if (url != null) {
            val uri = java.net.URI(url)
            val query = uri.query
            if (query != null) {
                val params = query.split("&")
                val filteredParams = params.filter { !it.startsWith("utm_") }
                val newQuery = if (filteredParams.isEmpty()) null else filteredParams.joinToString("&")

                val newUri = java.net.URI(
                    uri.scheme,
                    uri.authority,
                    uri.path,
                    newQuery,
                    uri.fragment
                )
                url = newUri.toString()
            }
        }
        return url
    }

    @Test
    fun `extracts plain url`() {
        val text = "https://example.com/article"
        assertEquals("https://example.com/article", extractUrl(text))
    }

    @Test
    fun `extracts url from feedly style text with title`() {
        val text = "Interesting Article Title https://example.com/article?utm_source=feedly"
        assertEquals("https://example.com/article", extractUrl(text))
    }

    @Test
    fun `strips utm parameters but keeps others`() {
        val text = "https://example.com/article?utm_medium=social&id=123&utm_campaign=test"
        assertEquals("https://example.com/article?id=123", extractUrl(text))
    }

    @Test
    fun `returns null if no url found`() {
        val text = "Just some text without a link"
        assertEquals(null, extractUrl(text))
    }
}
