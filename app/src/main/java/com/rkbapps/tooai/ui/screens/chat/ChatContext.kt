package com.rkbapps.tooai.ui.screens.chat

/*
 * How a chat fits in a model's context window.
 *
 * The window (the model's Max tokens setting) is split three ways:
 *   - a reserve for the next reply, so a reply always has room to finish;
 *   - a budget for history carried into a rebuilt conversation;
 *   - what is left, which new turns grow into before the next rebuild.
 *
 * A conversation is rebuilt when the next message plus the reply reserve would no longer fit. It
 * is refilled with the newest whole exchanges that fit the history budget. That budget is well
 * under the full window on purpose: refilling to the brim would force another rebuild, and
 * another re-read of the whole history, on the very next turn.
 */

private const val REPLY_RESERVE_FRACTION = 0.30f
private const val HISTORY_FRACTION = 0.40f

// Role markers and turn separators the chat template adds around each message.
private const val TEMPLATE_TOKENS_PER_MESSAGE = 8

// Below this an answer cut to fit says too little to be worth keeping.
private const val MIN_TRIMMED_ANSWER_TOKENS = 16
private const val TRIM_MARK = " …"

// The reply shown when a generation was stopped before producing any text.
internal const val STOPPED_PLACEHOLDER = "Stopped."

/**
 * A deliberately high estimate: LiteRT-LM has no call to count tokens before sending. English
 * runs near 4 bytes a token. Scripts like Devanagari take 3 bytes a character and about a token
 * each, so bytes / 3 stays on the safe side for both.
 */
internal fun estimateTokens(text: String): Int =
    text.toByteArray(Charsets.UTF_8).size / 3 + TEMPLATE_TOKENS_PER_MESSAGE

internal class ContextBudget(val maxTokens: Int, private val systemTokens: Int) {

    // Also the cap on a single reply's length.
    val replyReserve: Int = (maxTokens * REPLY_RESERVE_FRACTION).toInt()

    /** Tokens of history to carry over, leaving room for [pendingTokens] about to be sent. */
    fun historyBudget(pendingTokens: Int = 0): Int {
        val room = maxTokens - replyReserve - systemTokens - pendingTokens
        return minOf((maxTokens * HISTORY_FRACTION).toInt(), room).coerceAtLeast(0)
    }

    /** True when a message of [pendingTokens] and a full reply no longer fit after [usedTokens]. */
    fun needsRebuild(usedTokens: Int, pendingTokens: Int): Boolean =
        usedTokens + pendingTokens + replyReserve > maxTokens
}

// A question and the answer it got.
internal data class Exchange(val question: ChatMessage, val answer: ChatMessage) {
    /**
     * What the exchange costs to carry over. The engine's own count for the turn is used when
     * there is one, since the estimate runs high enough to turn away answers that would fit.
     * That count can only overstate (a conversation's first turn also counts the system prompt
     * and carried-over history), so the smaller of the two is taken.
     */
    val tokens: Int
        get() {
            val estimated = estimateTokens(question.message) + estimateTokens(answer.message)
            val measured = answer.statistics?.tokenUsed?.takeIf { it > 0 } ?: return estimated
            return minOf(measured, estimated)
        }

    // The same exchange with its answer cut short to fit [budget], or null if too little would
    // be left. The start is kept: it is where an answer says what it is about.
    fun trimmedTo(budget: Int): Exchange? {
        val room = budget - estimateTokens(question.message) - TEMPLATE_TOKENS_PER_MESSAGE
        if (room < MIN_TRIMMED_ANSWER_TOKENS) return null
        val maxBytes = room * 3 - TRIM_MARK.toByteArray(Charsets.UTF_8).size
        var length = minOf(answer.message.length, maxBytes)
        while (length > 0 && answer.message.take(length).toByteArray(Charsets.UTF_8).size > maxBytes) {
            length = length * 3 / 4
        }
        // Never end on half of a surrogate pair.
        if (length > 0 && answer.message[length - 1].isHighSurrogate()) length--
        if (length == 0) return null
        // Its measured count no longer applies, so the statistics go.
        return copy(answer = answer.copy(message = answer.message.take(length) + TRIM_MARK, statistics = null))
    }
}

/**
 * Pairs each question with its answer. Failed, empty and stopped-before-anything answers are
 * dropped along with their questions: the model should not be shown an error string as
 * something it said.
 */
internal fun completedExchanges(messages: List<ChatMessage>): List<Exchange> {
    val exchanges = mutableListOf<Exchange>()
    var question: ChatMessage? = null
    for (message in messages) {
        when (message.type) {
            ChatType.CHAT -> question = message
            ChatType.ASSISTANT -> {
                val asked = question
                question = null
                val usable = !message.isError && message.message.isNotBlank() &&
                        message.message != STOPPED_PLACEHOLDER
                if (asked != null && usable) exchanges += Exchange(asked, message)
            }
        }
    }
    return exchanges
}

/**
 * The newest exchanges that fit in [budget] tokens, oldest first. Whole exchanges only, so the
 * history always starts with a question and alternates, which chat templates expect. Stops at
 * the first one that doesn't fit rather than skipping it, so the history has no gaps.
 *
 * The newest exchange is what a follow-up most likely refers to. When even that is too long, it
 * is kept with its answer cut short rather than dropped.
 */
internal fun selectHistory(messages: List<ChatMessage>, budget: Int): List<Exchange> {
    val exchanges = completedExchanges(messages)
    val selected = ArrayDeque<Exchange>()
    var used = 0
    for (exchange in exchanges.asReversed()) {
        if (used + exchange.tokens > budget) break
        selected.addFirst(exchange)
        used += exchange.tokens
    }
    if (selected.isEmpty()) {
        exchanges.lastOrNull()?.trimmedTo(budget)?.let { selected.addFirst(it) }
    }
    return selected
}
