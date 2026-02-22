package com.rkbapps.tooai.ui.screens.chat

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.rkbapps.tooai.db.dao.ChatDao
import com.rkbapps.tooai.db.dao.LlmModelDao
import com.rkbapps.tooai.db.entity.ChatSession
import com.rkbapps.tooai.utils.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import com.rkbapps.tooai.db.entity.ChatMessage as ChatMessageEntity

class ChatRepository @Inject constructor(
    private val llmModelDao: LlmModelDao,
    private val chatDao: ChatDao
) {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState = _chatState.asStateFlow()

    val llmModels = llmModelDao.getAllLlmModels()

    companion object {
        // Context management constants
        private const val CONTEXT_THRESHOLD_PERCENTAGE = 0.75f // Reduce context at 75% of max tokens
        private const val MIN_MESSAGES_TO_KEEP = 2 // Keep at least the last 2 message pairs
        private var totalTokensUsed = 0
    }

    //for fresh chat
    suspend fun initializeModel(modelId: Long) {

        _chatState.update { it.copy(modelInitializingStatus = UiState(isLoading = true)) }

        val llmModel = try {
            llmModelDao.getLlmModelById(modelId)
        } catch (_: Exception) {
            null
        }
        if (llmModel == null) {
            _chatState.update { it.copy(modelInitializingStatus = UiState(error = "Model not found")) }
            return
        } else {
            _chatState.update { it.copy(llmModel = llmModel) }
        }

        val engineConfig = EngineConfig(
            modelPath = llmModel.path,
            backend = Backend.CPU,
            visionBackend = null,
            audioBackend = null,
            maxNumTokens = llmModel.maxTokens
        )

        try {
            val engine = Engine(engineConfig = engineConfig)
            engine.initialize()
            val conversation = engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        temperature = llmModel.temperature,
                        topK = llmModel.topK,
                        topP = llmModel.topP
                    ),
                    systemMessage = Message.of(getSystemPrompt())
                )
            )

            // Create a new session
            val newSessionId = UUID.randomUUID().toString()
            val session = ChatSession(
                id = newSessionId,
                modelId = modelId,
                title = "Chat ${
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                }",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            chatDao.insertSession(session)

            _chatState.update {
                it.copy(
                    instance = LlmModelInstance(engine = engine, conversation = conversation),
                    modelInitializingStatus = UiState(data = true),
                    sessionId = newSessionId
                )
            }
        } catch (e: Exception) {
            val index = e.message?.indexOf("=== Source Location Trace")
            if (index == null) {
                _chatState.update {
                    it.copy(modelInitializingStatus = UiState(error = "Something went wrong!"))
                }
            } else {
                val message = e.message?.take(index) ?: "Unknown Error"
                _chatState.update { it.copy(modelInitializingStatus = UiState(error = message)) }
            }
        }

    }

    //for old chats
    suspend fun loadSession(sessionId: String,modelId: Long? = null) {
        _chatState.update { it.copy(modelInitializingStatus = UiState(isLoading = true)) }

        modelId?.let { chatDao.updateSessionModel(sessionId,it) }

        val session = chatDao.getSessionById(sessionId)
        if (session == null) {
            _chatState.update { it.copy(modelInitializingStatus = UiState(error = "Session not found")) }
            return
        }

        val llmModel = try {
            llmModelDao.getLlmModelById(session.modelId)
        } catch (_: Exception) {
            null
        }

        if (llmModel == null) {
            _chatState.update { it.copy(modelInitializingStatus = UiState(error = "Model not found")) }
            return
        } else {
            _chatState.update { it.copy(llmModel = llmModel) }
        }

        // Initialize engine and conversation
        val engineConfig = EngineConfig(
            modelPath = llmModel.path,
            backend = Backend.CPU,
            visionBackend = null,
            audioBackend = null,
            maxNumTokens = llmModel.maxTokens
        )

        try {
            val engine = Engine(engineConfig = engineConfig)
            engine.initialize()

            // Load existing messages
            val messagesEntity = chatDao.getMessagesForSession(sessionId)
            val messages = messagesEntity.map { entity ->
                ChatMessage(
                    id = entity.id,
                    message = entity.content,
                    type = if (entity.sender == "USER") ChatType.CHAT else ChatType.ASSISTANT,
                    timestamp = entity.timestamp,
                    statistics = ChatStatistics(
                        timeToFirstToken = entity.timeToFirstToken,
                        prefillSpeed = entity.prefillSpeed,
                        decodeSpeed = entity.decodeSpeed,
                        totalLatency = entity.totalLatency,
                        tokenUsed = entity.tokenUsed
                    )
                )
            }

            // Construct history for system prompt
            val historyBuilder = StringBuilder(getSystemPrompt())
            if (messages.isNotEmpty()) {
                historyBuilder.append("\n\nPrevious Chat History:\n")
                (if (messages.size > 2) messages.takeLast(2) else messages).forEach { msg ->
                    val role = if (msg.type == ChatType.CHAT) "User" else "Model"
                    historyBuilder.append("$role: ${msg.message}\n")
                }
            }
            Log.d("ChatRepository", "History: $historyBuilder")

            val conversation = engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        temperature = llmModel.temperature,
                        topK = llmModel.topK,
                        topP = llmModel.topP
                    ),
                    systemMessage = Message.of(historyBuilder.toString())
                )
            )

            _chatState.update {
                it.copy(
                    instance = LlmModelInstance(engine = engine, conversation = conversation),
                    modelInitializingStatus = UiState(data = true),
                    sessionId = sessionId,
                    messages = messages
                )
            }

        } catch (e: Exception) {
            val index = e.message?.indexOf("=== Source Location Trace")
            if (index == null) {
                _chatState.update {
                    it.copy(
                        modelInitializingStatus = UiState(error = "Something went wrong! ${e.message}")
                    )
                }
            } else {
                val message = e.message?.take(index) ?: "Unknown Error"
                _chatState.update {
                    it.copy(
                        modelInitializingStatus = UiState(error = message)
                    )
                }
            }
        }
    }


    @OptIn(ExperimentalApi::class)
    suspend fun sendMessage(message: String) {
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
                    // Check if context is approaching limit and reduce if necessary
                    checkAndManageContextSize(instance,tokenUsedForCurrentChat)
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
        } catch (e: Exception) {
            Log.e("ChatRepository", "generateResponse: ${e.localizedMessage}",e)
            if (!isRetry) {
                generateResponse(message, instance, true, aiMessageId, sessionId)
            } else {
                _chatState.update { state ->
                    val messages = state.messages.toMutableList()
                    val msgIndex = messages.indexOfFirst { it.id == aiMessageId }
                    if (msgIndex != -1) {
                        val errorMsg = messages[msgIndex].copy(message = "Error: ${e.message}")
                        messages[msgIndex] = errorMsg
                        updateMessageInDb(errorMsg, sessionId, isError = true)
                    }
                    state.copy(messages = messages, isResponding = false, isChatRunning = false)
                }
            }
        }
    }


    /**
     * Checks if context size is approaching the limit and reduces chat history if needed.
     * This prevents the model from generating weird responses due to context overflow.
     */
    @OptIn(ExperimentalApi::class)
    private fun checkAndManageContextSize(instance: LlmModelInstance,tokenUsed:Int) {
        val llmModel = _chatState.value.llmModel ?: return
        val maxTokens = llmModel.maxTokens
        val contextThreshold = (maxTokens * CONTEXT_THRESHOLD_PERCENTAGE).toInt()

        try {
            totalTokensUsed+=tokenUsed
            Log.d("ChatRepository", "Context Check - Tokens Used: $totalTokensUsed, Threshold: $contextThreshold, Max: $maxTokens")
            // If tokens exceed threshold, reduce context
            if (totalTokensUsed >= contextThreshold) {
                Log.d("ChatRepository", "Context threshold exceeded! Reducing chat history...")
                reduceContextAndReinitialize(instance)
            }
        } catch (e: Exception) {
            Log.w("ChatRepository", "Error checking context size: ${e.message}")
        }
    }

    /**
     * Reduces chat history by removing older messages and reinitialized the conversation.
     * Keeps only the most recent messages to maintain conversation context.
     */
    private fun reduceContextAndReinitialize(instance: LlmModelInstance) {
        val llmModel = _chatState.value.llmModel ?: return
        val currentMessages = _chatState.value.messages.toMutableList()

        try {
            // Keep only recent messages (at least MIN_MESSAGES_TO_KEEP message pairs)
            val messagesToKeep = if (currentMessages.size > MIN_MESSAGES_TO_KEEP) {
                currentMessages.takeLast(MIN_MESSAGES_TO_KEEP)
            } else {
                currentMessages
            }

            Log.d("ChatRepository", "Reducing context: Keeping ${messagesToKeep.size} messages out of ${currentMessages.size}")

            // Close old conversation
            instance.conversation.close()

            // Build new system prompt with reduced history
            val historyBuilder = StringBuilder(getSystemPrompt())
            if (messagesToKeep.isNotEmpty()) {
                historyBuilder.append("\n\nPrevious Chat History:\n")
                messagesToKeep.forEach { msg ->
                    val role = if (msg.type == ChatType.CHAT) "User" else "Model"
                    historyBuilder.append("$role: ${msg.message}\n")
                }
            }

            // Create new conversation with reduced context
            val conversation = instance.engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        temperature = llmModel.temperature,
                        topK = llmModel.topK,
                        topP = llmModel.topP
                    ),
                    systemMessage = Message.of(historyBuilder.toString())
                )
            )

            totalTokensUsed = 0
            _chatState.update {
                it.copy(instance = it.instance?.copy(conversation = conversation))
            }
            Log.d("ChatRepository", "Context reinitialized with ${messagesToKeep.size} messages")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error reducing context: ${e.message}", e)
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
            |- Don't give any link/url
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