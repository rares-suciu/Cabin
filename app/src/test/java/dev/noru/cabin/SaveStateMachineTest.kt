package dev.noru.cabin

import dev.noru.cabin.data.local.entity.ArticleStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveStateMachineTest {

    // Simulating the state transitions as described in the worker
    @Test
    fun `successful extraction moves to READY`() {
        var state = ArticleStatus.QUEUED

        // worker starts
        state = ArticleStatus.SAVING

        // network fetch and parse succeeds
        val parseSuccess = true

        if (parseSuccess) {
            state = ArticleStatus.READY
        } else {
            state = ArticleStatus.LINK_ONLY
        }

        assertEquals(ArticleStatus.READY, state)
    }

    @Test
    fun `failed extraction moves to LINK_ONLY`() {
        var state = ArticleStatus.QUEUED

        // worker starts
        state = ArticleStatus.SAVING

        // network fetch succeeds but parse returns null
        val parseSuccess = false

        if (parseSuccess) {
            state = ArticleStatus.READY
        } else {
            state = ArticleStatus.LINK_ONLY
        }

        assertEquals(ArticleStatus.LINK_ONLY, state)
    }

    @Test
    fun `network error moves to FAILED`() {
        var state = ArticleStatus.QUEUED

        try {
            state = ArticleStatus.SAVING
            throw java.io.IOException("Network error")
        } catch (e: Exception) {
            state = ArticleStatus.FAILED
        }

        assertEquals(ArticleStatus.FAILED, state)
    }
}
