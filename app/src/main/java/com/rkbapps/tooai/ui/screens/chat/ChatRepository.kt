package com.rkbapps.tooai.ui.screens.chat

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.rkbapps.tooai.db.dao.ChatDao
import com.rkbapps.tooai.db.dao.LlmModelDao
import com.rkbapps.tooai.db.entity.ChatSession
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.Prompts
import com.rkbapps.tooai.utils.chatTemplateOverride
import com.rkbapps.tooai.utils.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import com.rkbapps.tooai.db.entity.ChatMessage as ChatMessageEntity

class ChatRepository @Inject constructor(
    private val llmModelDao: LlmModelDao,
    private val chatDao: ChatDao
) {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState = _chatState.asStateFlow()

    val llmModels = llmModelDao.getAllLlmModels()

    // Tokens the current conversation holds. An estimate when the conversation is built, then
    // the engine's own counts once turns have run. See ChatContext.kt for how the window is split.
    private var contextTokens = 0
    private var contextMeasured = false

    // A cancelled conversation rejects every later message, and closing it while the native
    // decode unwinds crashes, so it is replaced just before the next send.
    private var isConversationCancelled = false

    // A chat's model can be deleted, or its file can disappear while the row stays (cleared
    // storage, for one). Neither can be loaded, so both count as unavailable.
    private suspend fun findUsableModel(modelId: Long): LlmModel? =
        llmModelDao.getLlmModelById(modelId)?.takeIf { File(it.path).exists() }

    // Set as soon as a load is asked for, before it waits its turn, so the UI locks at once.
    fun markModelLoading() {
        _chatState.update { it.copy(modelInitializingStatus = UiState(isLoading = true)) }
    }

    // For a fresh chat. The session is created once the model has loaded.
    suspend fun startWithModel(modelId: Long) {
        val model = findUsableModel(modelId)
        if (model == null) {
            _chatState.update { it.copy(modelUnavailable = true, modelInitializingStatus = UiState()) }
            return
        }
        loadModel(model)
    }

    // For an existing chat. The history loads first and shows whatever happens to the model.
    suspend fun loadSession(sessionId: String) {
        val session = chatDao.getSessionById(sessionId)
        if (session == null) {
            _chatState.update { it.copy(modelInitializingStatus = UiState(error = "This chat no longer exists.")) }
            return
        }
        val messages = chatDao.getMessagesForSession(sessionId).map { it.toChatMessage() }
        _chatState.update {
            it.copy(sessionId = session.id, sessionTitle = session.title, messages = messages)
        }

        val model = findUsableModel(session.modelId)
        if (model == null) {
            _chatState.update { it.copy(modelUnavailable = true, modelInitializingStatus = UiState()) }
            return
        }
        loadModel(model)
    }

    // The previous model is released before the new one loads: two models don't fit in memory
    // together. The caller must already have stopped any reply. See ChatViewModel.switchModel.
    suspend fun switchModel(modelId: Long) {
        val current = _chatState.value
        if (current.instance != null && current.llmModel?.id == modelId) {
            _chatState.update { it.copy(modelInitializingStatus = UiState(data = true)) }
            return
        }
        val model = findUsableModel(modelId)
        releaseModel()
        if (model == null) {
            _chatState.update {
                it.copy(
                    llmModel = null,
                    modelInitializingStatus = UiState(error = "That model's file is missing. Choose another model.")
                )
            }
            return
        }
        loadModel(model)
    }

    private suspend fun loadModel(model: LlmModel) {
        _chatState.update {
            it.copy(
                llmModel = model,
                modelUnavailable = false,
                modelInitializingStatus = UiState(isLoading = true)
            )
        }
        val instance = try {
            openInstance(model, _chatState.value.messages)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ChatRepository", "loadModel: ${e.message}", e)
            _chatState.update {
                it.copy(
                    llmModel = null,
                    modelInitializingStatus = UiState(error = "Couldn't load ${model.displayName}. ${e.readableMessage()}")
                )
            }
            return
        }

        // Non-cancellable so a loaded engine always ends up in the state, where the ViewModel's
        // teardown can find and free it. The chat's model is saved only now, so a failed load
        // never leaves the chat pointing at a model that doesn't work.
        withContext(NonCancellable) {
            val sessionId = _chatState.value.sessionId
            val newSession = if (sessionId == null) {
                createSession(model.id)
            } else {
                chatDao.updateSessionModel(sessionId, model.id)
                null
            }
            isConversationCancelled = false
            _chatState.update {
                it.copy(
                    instance = instance,
                    modelInitializingStatus = UiState(data = true),
                    sessionId = newSession?.id ?: it.sessionId,
                    sessionTitle = newSession?.title ?: it.sessionTitle
                )
            }
        }
    }

    private suspend fun openInstance(model: LlmModel, messages: List<ChatMessage>): LlmModelInstance {
        val engine = Engine(
            EngineConfig(
                modelPath = model.path,
                backend = Backend.CPU(),
                visionBackend = null,
                audioBackend = null,
                maxNumTokens = model.maxTokens
            )
        )
        try {
            engine.initialize()
            // initialize() blocks and can't be interrupted. If the chat closed while it ran,
            // nothing else holds this engine, so it is freed here.
            currentCoroutineContext().ensureActive()
            return LlmModelInstance(engine, buildConversation(engine, model, messages))
        } catch (e: Throwable) {
            runCatching { engine.close() }
            throw e
        }
    }

    // Only safe once no decode is running on this model. The ViewModel waits for that first.
    fun releaseModel() {
        val instance = _chatState.value.instance ?: return
        _chatState.update { it.copy(instance = null) }
        runCatching { instance.conversation.close() }
        runCatching { instance.engine.close() }
    }

    private fun budgetFor(model: LlmModel) =
        ContextBudget(maxTokens = model.maxTokens, systemTokens = estimateTokens(getSystemPrompt()))

    /**
     * Starts a conversation that already holds the newest history that fits, leaving room for a
     * message of [pendingTokens] that is about to be sent.
     *
     * History goes in as real user and model turns. Pasted into the system prompt as a
     * "User: ... Model: ..." transcript, small models copied that format into their replies.
     */
    private fun buildConversation(
        engine: Engine,
        model: LlmModel,
        messages: List<ChatMessage>,
        pendingTokens: Int = 0
    ): Conversation {
        val budget = budgetFor(model)
        val history = selectHistory(messages, budget.historyBudget(pendingTokens))
        val conversation = engine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(getSystemPrompt()),
                initialMessages = history.flatMap {
                    listOf(Message.user(it.question.message), Message.model(it.answer.message))
                },
                samplerConfig = SamplerConfig(
                    temperature = model.temperature,
                    topK = model.topK,
                    topP = model.topP
                ),
                // The pre-send check keeps this much room free, so a reply can always finish.
                maxOutputToken = budget.replyReserve,
                // Null keeps the model's own template. See ChatTemplates.kt.
                chatTemplate = chatTemplateOverride(model.path)
            )
        )
        contextTokens = estimateTokens(getSystemPrompt()) + history.sumOf { it.tokens }
        contextMeasured = false
        Log.d(
            "ChatRepository",
            "Conversation built: ${history.size} of ${completedExchanges(messages).size} exchanges, " +
                    "~$contextTokens of ${model.maxTokens} tokens"
        )
        return conversation
    }

    // The first turn's count covers everything prefilled so far: the system prompt, the
    // carried-over history and the message itself. Later counts are that turn alone.
    private fun recordTurn(tokens: Int) {
        contextTokens = if (contextMeasured) contextTokens + tokens else tokens
        contextMeasured = true
        Log.d("ChatRepository", "Turn used $tokens tokens, context now $contextTokens")
    }

    private fun ChatMessageEntity.toChatMessage() = ChatMessage(
        id = id,
        message = content,
        type = if (sender == "USER") ChatType.CHAT else ChatType.ASSISTANT,
        timestamp = timestamp,
        isError = isError,
        statistics = ChatStatistics(
            timeToFirstToken = timeToFirstToken,
            prefillSpeed = prefillSpeed,
            decodeSpeed = decodeSpeed,
            totalLatency = totalLatency,
            tokenUsed = tokenUsed
        )
    )

    // LiteRT appends a native stack trace to its messages.
    private fun Exception.readableMessage(): String =
        message?.substringBefore("=== Source Location Trace")?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Unknown error"

    private suspend fun createSession(modelId: Long): ChatSession {
        val session = ChatSession(
            modelId = modelId,
            title = "Chat ${
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            }",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertSession(session)
        return session
    }

    // Reuses the loaded engine so the model doesn't reload.
    suspend fun startNewChat() {
        val state = _chatState.value
        val instance = state.instance ?: return
        val llmModel = state.llmModel ?: return
        if (state.isChatRunning || state.messages.isEmpty()) return

        try {
            instance.conversation.close()
            val conversation = buildConversation(instance.engine, llmModel, emptyList())
            val session = createSession(llmModel.id)
            isConversationCancelled = false
            _chatState.update {
                it.copy(
                    instance = instance.copy(conversation = conversation),
                    sessionId = session.id,
                    sessionTitle = session.title,
                    messages = emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "startNewChat: ${e.localizedMessage}", e)
            _chatState.update {
                it.copy(modelInitializingStatus = UiState(error = "Couldn't start a new chat"))
            }
        }
    }

    @OptIn(ExperimentalApi::class)
    suspend fun sendMessage(message: String) {
        val message = if (chatState.value.currentPromptType!=null){
            (chatState.value.currentPromptType?.prompt?:"") + message
        }else{
            message
        }
        Log.d("ChatRepository", "sendMessage: $message")
        // Checked before sending, while the size of this message can still be allowed for. A
        // cancelled conversation is replaced here too.
        val pendingTokens = estimateTokens(message)
        val loaded = _chatState.value.instance
        val model = _chatState.value.llmModel
        if (loaded != null && model != null &&
            (isConversationCancelled || budgetFor(model).needsRebuild(contextTokens, pendingTokens))
        ) {
            withContext(Dispatchers.IO) { rebuildConversation(loaded, model, pendingTokens) }
            isConversationCancelled = false
        }
        val instance = _chatState.value.instance ?: return
        val sessionId = _chatState.value.sessionId ?: return

        _chatState.update {
            it.copy(isChatRunning = true, isResponding = true)
        }

        val userMessageId = UUID.randomUUID().toString()
        val userMessage = ChatMessage(id = userMessageId, message = message, type = ChatType.CHAT)

        // Save user message to DB
        saveMessageToDb(userMessage, sessionId)

        val currentMessages = _chatState.value.messages.toMutableList()
        currentMessages.add(userMessage)

        // Add placeholder for AI response
        val aiMessageId = UUID.randomUUID().toString()
        val aiMessage = ChatMessage(id = aiMessageId, message = "", type = ChatType.ASSISTANT)

        // Save initial AI message to DB (empty)
        saveMessageToDb(aiMessage, sessionId)

        currentMessages.add(aiMessage)

        _chatState.update { it.copy(messages = currentMessages) }

        generateResponse(message, instance, false, aiMessageId, sessionId)
    }

    fun cancelGeneration() {
        _chatState.value.instance?.conversation?.cancelProcess()
        isConversationCancelled = true
    }

    fun selectAPredefinePrompt(prompt: Prompts?){
        _chatState.update {
            it.copy(currentPromptType = prompt)
        }
    }

    @OptIn(ExperimentalApi::class)
    private suspend fun generateResponse(
        message: String, instance: LlmModelInstance,
        isRetry: Boolean, aiMessageId: String, sessionId: String
    ) {
        try {
            var fullResponse = ""
            val start = System.currentTimeMillis()
            var firstTokenTs = 0L
            var prefillTokens = 0
            var decodeTokens = 0
            var firstRun = true

            val currentConversation = instance.conversation

            currentConversation.sendMessageAsync(
                Message.of(Content.Text(message)),
            ).catch { e ->
                throw e
            }.onCompletion { cause ->
                Log.d("ChatRepository", "generateResponse: $cause")
                if (cause == null) {
                    val curTs = System.currentTimeMillis()
                    val stats = calculateStatistics(start, firstTokenTs, curTs, prefillTokens, decodeTokens, true)
                    val benchmarkInfo = instance.conversation.getBenchmarkInfo()
                    val tokenUsedForCurrentChat = (benchmarkInfo.lastPrefillTokenCount + benchmarkInfo.lastDecodeTokenCount)
                    recordTurn(tokenUsedForCurrentChat)
                    // updated
                    _chatState.update { state ->
                        val messages = state.messages.toMutableList()
                        val msgIndex = messages.indexOfFirst { it.id == aiMessageId }
                        if (msgIndex != -1) {
                            val updatedMsg = messages[msgIndex].copy(
                                message = fullResponse,
                                statistics = stats.copy(tokenUsed = tokenUsedForCurrentChat)
                            )
                            messages[msgIndex] = updatedMsg
                            // Update DB with final response
                            updateMessageInDb(updatedMsg, sessionId)
                        }
                        state.copy(messages = messages, isResponding = false, isChatRunning = false)
                    }
                } else {
                    if (!isRetry) {
                        // If it's not a retry, let catch handle it
                    } else {
                        _chatState.update { it.copy(isResponding = false, isChatRunning = false) }
                    }
                }

            }.collect { msg ->
                val curTs = System.currentTimeMillis()
                if (firstRun) {
                    firstTokenTs = curTs
                    prefillTokens = currentConversation.getBenchmarkInfo().lastPrefillTokenCount
                    firstRun = false
                } else {
                    decodeTokens++
                }

                fullResponse += msg.toString()

                _chatState.update { state ->
                    val messages = state.messages.toMutableList()
                    val msgIndex = messages.indexOfFirst { it.id == aiMessageId }
                    if (msgIndex != -1) {
                        messages[msgIndex] = messages[msgIndex].copy(
                            message = fullResponse
                        )
                    }
                    state.copy(messages = messages)
                }
            }
        } catch (e: CancellationException) {
            // Must stay ahead of the catch below, which would retry a stopped reply.
            withContext(NonCancellable) { keepStoppedResponse(aiMessageId, sessionId) }
            throw e
        } catch (e: Exception) {
            Log.e("ChatRepository", "generateResponse: ${e.localizedMessage}",e)
            if (!isRetry) {
                generateResponse(message, instance, true, aiMessageId, sessionId)
            } else {
                _chatState.update { state ->
                    val messages = state.messages.toMutableList()
                    val msgIndex = messages.indexOfFirst { it.id == aiMessageId }
                    if (msgIndex != -1) {
                        val errorMsg = messages[msgIndex].copy(message = "Error: ${e.message}", isError = true)
                        messages[msgIndex] = errorMsg
                        updateMessageInDb(errorMsg, sessionId, isError = true)
                    }
                    state.copy(messages = messages, isResponding = false, isChatRunning = false)
                }
            }
        }
    }


    private suspend fun keepStoppedResponse(aiMessageId: String, sessionId: String) {
        _chatState.update { state ->
            val messages = state.messages.toMutableList()
            val msgIndex = messages.indexOfFirst { it.id == aiMessageId }
            if (msgIndex != -1) {
                val stopped = messages[msgIndex].copy(
                    message = messages[msgIndex].message.ifEmpty { STOPPED_PLACEHOLDER }
                )
                messages[msgIndex] = stopped
                updateMessageInDb(stopped, sessionId)
            }
            state.copy(messages = messages, isResponding = false, isChatRunning = false)
        }
    }

    // Closes the conversation and starts one holding the newest history that fits.
    private fun rebuildConversation(instance: LlmModelInstance, model: LlmModel, pendingTokens: Int) {
        try {
            instance.conversation.close()
            val conversation =
                buildConversation(instance.engine, model, _chatState.value.messages, pendingTokens)
            _chatState.update {
                it.copy(instance = it.instance?.copy(conversation = conversation))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error rebuilding the conversation: ${e.message}", e)
        }
    }

    private suspend fun saveMessageToDb(chatMessage: ChatMessage, sessionId: String) =
        withContext(Dispatchers.IO) {
            val entity = ChatMessageEntity(
                id = chatMessage.id,
                sessionId = sessionId,
                sender = if (chatMessage.type == ChatType.CHAT) "USER" else "ASSISTANT",
                content = chatMessage.message,
                timestamp = chatMessage.timestamp,
                timeToFirstToken = chatMessage.statistics?.timeToFirstToken,
                prefillSpeed = chatMessage.statistics?.prefillSpeed,
                decodeSpeed = chatMessage.statistics?.decodeSpeed,
                totalLatency = chatMessage.statistics?.totalLatency,
                tokenUsed = chatMessage.statistics?.tokenUsed
            )
            chatDao.insertMessage(entity)
        }

    private suspend fun updateMessageInDb(
        chatMessage: ChatMessage,
        sessionId: String,
        isError: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val entity = ChatMessageEntity(
            id = chatMessage.id,
            sessionId = sessionId,
            sender = if (chatMessage.type == ChatType.CHAT) "USER" else "ASSISTANT",
            content = chatMessage.message,
            timestamp = chatMessage.timestamp,
            timeToFirstToken = chatMessage.statistics?.timeToFirstToken,
            prefillSpeed = chatMessage.statistics?.prefillSpeed,
            decodeSpeed = chatMessage.statistics?.decodeSpeed,
            totalLatency = chatMessage.statistics?.totalLatency,
            tokenUsed = chatMessage.statistics?.tokenUsed,
            isError = isError
        )
        chatDao.updateMessage(entity)
    }


    private fun calculateStatistics(
        start: Long,
        firstTokenTs: Long,
        curTs: Long,
        prefillTokens: Int,
        decodeTokens: Int,
        isComplete: Boolean
    ): ChatStatistics {
        val timeToFirstToken = if (firstTokenTs > 0) (firstTokenTs - start) / 1000f else null
        val prefillSpeed =
            if (timeToFirstToken != null && timeToFirstToken > 0) prefillTokens / timeToFirstToken else null

        var decodeSpeed: Float? = null
        var totalLatency: Float? = null

        if (isComplete) {
            val decodeDuration = if (firstTokenTs > 0) (curTs - firstTokenTs) / 1000f else 0f
            if (decodeDuration > 0 && decodeTokens > 0) {
                decodeSpeed = decodeTokens / decodeDuration
            }
            totalLatency = (curTs - start) / 1000f
        }

        return ChatStatistics(
            timeToFirstToken = timeToFirstToken,
            prefillSpeed = prefillSpeed,
            decodeSpeed = decodeSpeed,
            totalLatency = totalLatency
        )
    }



    fun getSystemPrompt(): String {
        @SuppressWarnings("JavaTimeDefaultTimeZone")
        val curDateTimeString =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

        return """
            |You are a helpful AI assistant for TooAi mobile app developed by RKEXUS.
            |Date/Time: $curDateTimeString ( in yyyy-MM-dd'T'HH:mm:ss format )
            |
            |Guidelines:
            |- Be concise and direct
            |- Don't give any link/url inside chat
            |- Use markdown formatting
            |- Keep paragraphs short
            |- Use lists and bullet points
            |- Ask clarifying questions if needed
            |- Acknowledge limitations
            |
            |Maintain context across the conversation.
        """.trimMargin()
    }

}