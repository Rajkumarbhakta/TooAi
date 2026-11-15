package com.rkbapps.tooai.ui.screens.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatContextTest {

    private fun user(text: String) = ChatMessage(message = text, type = ChatType.CHAT)
    private fun model(text: String, isError: Boolean = false) =
        ChatMessage(message = text, type = ChatType.ASSISTANT, isError = isError)

    // 30 bytes of text is 10 tokens, plus 8 for the template, so each message is 18 tokens
    // and each exchange 36.
    private val thirty = "x".repeat(30)

    @Test
    fun `estimate counts bytes, so non-Latin text is not undercounted`() {
        assertEquals(8 + 4, estimateTokens("abcdefghijkl"))
        // Four Devanagari characters are 12 bytes.
        assertEquals(8 + 4, estimateTokens("नमसत"))
    }

    @Test
    fun `the window is split into reply reserve and a capped history budget`() {
        val budget = ContextBudget(maxTokens = 2000, systemTokens = 150)
        assertEquals(600, budget.replyReserve)
        // 40% of the window, since far more than that is free.
        assertEquals(800, budget.historyBudget())
    }

    @Test
    fun `a long pending message shrinks the history budget`() {
        val budget = ContextBudget(maxTokens = 1000, systemTokens = 150)
        // 1000 - 300 reserve - 150 system - 400 pending leaves 150, under the 400 cap.
        assertEquals(150, budget.historyBudget(pendingTokens = 400))
        // A message too big for the window leaves nothing, never a negative budget.
        assertEquals(0, budget.historyBudget(pendingTokens = 900))
    }

    @Test
    fun `a rebuild is due when the message and a full reply no longer fit`() {
        val budget = ContextBudget(maxTokens = 1000, systemTokens = 150)
        assertFalse(budget.needsRebuild(usedTokens = 600, pendingTokens = 100))
        assertTrue(budget.needsRebuild(usedTokens = 601, pendingTokens = 100))
    }

    @Test
    fun `history built to its budget leaves room for turns before the next rebuild`() {
        val budget = ContextBudget(maxTokens = 2000, systemTokens = 150)
        val afterRebuild = 150 + budget.historyBudget()
        assertFalse(budget.needsRebuild(afterRebuild, pendingTokens = 100))
    }

    @Test
    fun `exchanges pair each question with its answer`() {
        val messages = listOf(user("q1"), model("a1"), user("q2"), model("a2"))
        val exchanges = completedExchanges(messages)
        assertEquals(listOf("q1" to "a1", "q2" to "a2"), exchanges.map { it.question.message to it.answer.message })
    }

    @Test
    fun `failed, empty and stopped answers are dropped with their questions`() {
        val messages = listOf(
            user("q1"), model("Error: boom", isError = true),
            user("q2"), model(""),
            user("q3"), model(STOPPED_PLACEHOLDER),
            user("q4"), model("a4"),
            user("q5")
        )
        val exchanges = completedExchanges(messages)
        assertEquals(listOf("q4"), exchanges.map { it.question.message })
    }

    @Test
    fun `history keeps the newest exchanges that fit, oldest first`() {
        val messages = (1..5).flatMap { listOf(user("$it$thirty"), model("$it$thirty")) }
        // Each exchange is 36 tokens, so 80 holds two.
        val history = selectHistory(messages, budget = 80)
        assertEquals(listOf("4", "5"), history.map { it.question.message.take(1) })
    }

    @Test
    fun `history stops at an exchange that does not fit instead of skipping it`() {
        val messages = listOf(
            user("old"), model("old"),
            user("big"), model("y".repeat(3000)),
            user("new"), model("new")
        )
        // "old" would fit, but taking it would leave a hole where "big" was.
        val history = selectHistory(messages, budget = 60)
        assertEquals(listOf("new"), history.map { it.question.message })
    }

    @Test
    fun `a measured token count is used when it is lower than the estimate`() {
        val answer = model("y".repeat(600)).copy(statistics = ChatStatistics(tokenUsed = 171))
        // The estimate alone would be 8 + 208, well over the measured turn.
        assertEquals(171, Exchange(user("q"), answer).tokens)
    }

    @Test
    fun `a measured count that includes the system prompt does not inflate a short exchange`() {
        val answer = model("144").copy(statistics = ChatStatistics(tokenUsed = 177))
        assertEquals(estimateTokens("q") + estimateTokens("144"), Exchange(user("q"), answer).tokens)
    }

    @Test
    fun `a newest exchange too long to fit is kept with its answer cut short`() {
        val messages = listOf(user("old"), model("old"), user("q"), model("y".repeat(3000)))
        val history = selectHistory(messages, budget = 100)
        assertEquals(1, history.size)
        assertEquals("q", history[0].question.message)
        assertTrue(history[0].answer.message.endsWith("…"))
        assertTrue(history[0].answer.message.length < 3000)
        assertTrue(history[0].tokens <= 100)
    }

    @Test
    fun `trimming never splits a surrogate pair`() {
        val emoji = "\uD83D\uDE00".repeat(400)
        val trimmed = Exchange(user("q"), model(emoji)).trimmedTo(100)!!
        val kept = trimmed.answer.message.removeSuffix(" …")
        assertFalse(kept.last().isHighSurrogate())
    }

    @Test
    fun `no budget means no history`() {
        assertTrue(selectHistory(listOf(user("q"), model("a")), budget = 0).isEmpty())
    }
}
