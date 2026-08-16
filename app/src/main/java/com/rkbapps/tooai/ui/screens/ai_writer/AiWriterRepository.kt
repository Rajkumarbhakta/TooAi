
package com.rkbapps.tooai.ui.screens.ai_writer

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.android.datatransport.runtime.backends.BackendFactory
import com.rkbapps.tooai.db.dao.LlmModelDao
import com.rkbapps.tooai.db.entity.LlmModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * One-shot inference for the AI Writer.
 *
 * Deliberately does not depend on [com.rkbapps.tooai.db.dao.ChatDao]: a rewrite triggered from
 * another app's text selection must not create a [com.rkbapps.tooai.db.entity.ChatSession] or
 * persist messages, otherwise every selection would show up in the chat history list.
 *
 * The [Engine] (expensive, holds the model in memory) and the [Conversation] (cheap, holds the
 * turn history) are managed separately so each writer action can start from a clean conversation
 * without paying to reload the model.
 */
class AiWriterRepository @Inject constructor(
    private val llmModelDao: LlmModelDao
) {

    val models: Flow<List<LlmModel>> = llmModelDao.getAllLlmModels()

    /**
     * Loads the model into memory. Expensive — several seconds for a multi-GB model — so it runs
     * on IO and the caller is expected to show progress.
     */
    suspend fun loadEngine(model: LlmModel): Result<Engine> = withContext(Dispatchers.IO) {
        try {
            val engine = Engine(
                engineConfig = EngineConfig(
                    modelPath = model.path,
                    backend = Backend.CPU(),
                    visionBackend = null,
                    audioBackend = null,
                    maxNumTokens = model.maxTokens
                )
            )
            engine.initialize()
            Result.success(engine)
        } catch (e: Exception) {
            Log.e(TAG, "loadEngine failed", e)
            Result.failure(IllegalStateException(e.readableMessage()))
        }
    }

    /** A fresh conversation per action, so actions don't influence each other. */
    suspend fun newConversation(engine: Engine, model: LlmModel): Result<Conversation> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(
                    engine.createConversation(
                        ConversationConfig(
                            samplerConfig = SamplerConfig(
                                temperature = model.temperature,
                                topK = model.topK,
                                topP = model.topP
                            ),
                            systemInstruction = Contents.of(SYSTEM_PROMPT)
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "newConversation failed", e)
                Result.failure(IllegalStateException(e.readableMessage()))
            }
        }

    /** Emits the response as it is generated. Each emission is the full text so far. */
    fun generate(conversation: Conversation, prompt: String): Flow<String> = flow {
        val builder = StringBuilder()
        conversation.sendMessageAsync(Contents.of(Content.Text(prompt))).collect { message ->
            builder.append(message.toString())
            emit(builder.toString())
        }
    }.flowOn(Dispatchers.IO)

    fun cancel(conversation: Conversation) = runCatching { conversation.cancelProcess() }
        .onFailure { Log.e(TAG, "cancel failed", it) }

    fun close(conversation: Conversation) = runCatching { conversation.close() }
        .onFailure { Log.e(TAG, "closing conversation failed", it) }

    fun close(engine: Engine) = runCatching { engine.close() }
        .onFailure { Log.e(TAG, "closing engine failed", it) }

    companion object {
        private const val TAG = "AiWriterRepository"

        /**
         * Terse and output-only. The chat system prompt asks for markdown, which is wrong here —
         * the result is written straight back into a plain text field, where fences and preamble
         * would be pasted literally.
         */
        private const val SYSTEM_PROMPT =
            "You are a writing assistant. You write, rewrite, correct, or summarize text as the " +
                "user asks.\n" +
                "Output ONLY the resulting text. Do not add a preamble, explanation, or commentary. " +
                "Do not wrap the output in quotes or markdown code fences. " +
                "Preserve the original language of the text. " +
                "If the text already satisfies the request, return it unchanged."
    }
}

/**
 * LiteRT appends a long native trace to its exception messages; showing it raw is useless to a
 * user. Mirrors the trimming [com.rkbapps.tooai.ui.screens.chat.ChatRepository] does on init.
 */
internal fun Throwable.readableMessage(): String {
    val raw = message ?: return "Something went wrong!"
    val traceIndex = raw.indexOf("=== Source Location Trace")
    return if (traceIndex == -1) raw else raw.take(traceIndex).trim()
}