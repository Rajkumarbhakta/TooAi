package com.rkbapps.tooai.ui.screens.chat

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    suspend fun initializeModel(modelId: Long){

        _chatState.update { it.copy(modelInitializingStatus = UiState(isLoading = true)) }

        val llmModel = try {
            llmModelDao.getLlmModelById(modelId)
        }catch (e: Exception){
            null
        }
        if (llmModel==null){
            _chatState.update { it.copy(modelInitializingStatus = UiState(error = "Model not found")) }
            return
        }else{
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
                title = "Chat ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}",
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
        }catch (e: Exception){
            val index = e.message?.indexOf("=== Source Location Trace")
            if (index==null){
                _chatState.update {
                    it.copy(
                        modelInitializingStatus = UiState(error = "Something went wrong!")
                    )
                }
            }else{
                val message = e.message?.take(index) ?:"Unknown Error"
                _chatState.update {
                    it.copy(
                        modelInitializingStatus = UiState(error = message)
                    )
                }
            }
        }

    }

    suspend fun loadSession(sessionId: String) {
        _chatState.update { it.copy(modelInitializingStatus = UiState(isLoading = true)) }

        val session = chatDao.getSessionById(sessionId)
        if (session == null) {
            _chatState.update { it.copy(modelInitializingStatus = UiState(error = "Session not found")) }
            return
        }

        val llmModel = try {
            llmModelDao.getLlmModelById(session.modelId)
        } catch (e: Exception) {
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
            val messagesEntity = chatDao.getMessagesForSession(sessionId).first()
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
                        totalLatency = entity.totalLatency
                    )
                )
            }
            
            // Construct history for system prompt
            val historyBuilder = StringBuilder(getSystemPrompt())
            if (messages.isNotEmpty()) {
                historyBuilder.append("\n\nPrevious Chat History:\n")
                messages.forEach { msg ->
                    val role = if (msg.type == ChatType.CHAT) "User" else "Model"
                    historyBuilder.append("$role: ${msg.message}\n")
                }
            }

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
            if (index==null){
                _chatState.update {
                    it.copy(
                        modelInitializingStatus = UiState(error = "Something went wrong! ${e.message}")
                    )
                }
            }else{
                val message = e.message?.take(index) ?:"Unknown Error"
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

        _chatState.update { it.copy(
            isChatRunning = true,
            isResponding = true
        ) }
        
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
        message: String, 
        instance: LlmModelInstance, 
        isRetry: Boolean,
        aiMessageId: String,
        sessionId: String
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
                    if (cause == null) {
                        val curTs = System.currentTimeMillis()
                        val stats = calculateStatistics(start, firstTokenTs, curTs, prefillTokens, decodeTokens, true)
                        
                        _chatState.update { state ->
                            val messages = state.messages.toMutableList()
                            val msgIndex = messages.indexOfFirst { it.id == aiMessageId }
                            if (msgIndex != -1) {
                                val updatedMsg = messages[msgIndex].copy(
                                    message = fullResponse,
                                    statistics = stats
                                )
                                messages[msgIndex] = updatedMsg
                                
                                // Update DB with final response
                                updateMessageInDb(updatedMsg, sessionId)
                            }
                            state.copy(messages = messages, isResponding = false)
                        }
                    } else {
                        if (!isRetry) {
                           // If it's not a retry, let catch handle it
                        } else {
                           _chatState.update { it.copy(isResponding = false) }
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
             if (!isRetry) {
                 resetConversation(instance)
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
                    state.copy(messages = messages, isResponding = false)
                }
             }
        }
    }

    private fun resetConversation(instance: LlmModelInstance) {
        val llmModel = _chatState.value.llmModel ?: return
        try {
            instance.conversation.close()
            
            // When resetting, we start fresh to clear context. 
            // We do NOT replay history here to avoid immediately hitting the limit again.
            val newConversation = instance.engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        temperature = llmModel.temperature,
                        topK = llmModel.topK,
                        topP = llmModel.topP
                    ),
                    systemMessage = Message.of(getSystemPrompt())
                )
            )
            instance.conversation = newConversation
            _chatState.update { it.copy(instance = instance) }
            
        } catch (e: Exception) {
            // Log error
        }
    }
    
    private fun saveMessageToDb(chatMessage: ChatMessage, sessionId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val entity = ChatMessageEntity(
                id = chatMessage.id,
                sessionId = sessionId,
                sender = if (chatMessage.type == ChatType.CHAT) "USER" else "ASSISTANT",
                content = chatMessage.message,
                timestamp = chatMessage.timestamp,
                timeToFirstToken = chatMessage.statistics?.timeToFirstToken,
                prefillSpeed = chatMessage.statistics?.prefillSpeed,
                decodeSpeed = chatMessage.statistics?.decodeSpeed,
                totalLatency = chatMessage.statistics?.totalLatency
            )
            chatDao.insertMessage(entity)
        }
    }

    private fun updateMessageInDb(chatMessage: ChatMessage, sessionId: String, isError: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
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
                isError = isError
            )
            chatDao.updateMessage(entity)
        }
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
        val prefillSpeed = if (timeToFirstToken != null && timeToFirstToken > 0) prefillTokens / timeToFirstToken else null
        
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
        return "Current date and time given in YYYY-MM-DDTHH:MM:SS format: ${curDateTimeString}. " +
                "You are a model that can do function calling with the following functions"
    }

}