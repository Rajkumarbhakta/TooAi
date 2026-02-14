package com.rkbapps.tooai.ui.screens.chat

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.rkbapps.tooai.db.dao.LlmModelDao
import com.rkbapps.tooai.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val llmModelDao: LlmModelDao
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
            _chatState.update {
                it.copy(
                    instance = LlmModelInstance(engine = engine, conversation = conversation),
                    modelInitializingStatus = UiState(data = true)
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

    @OptIn(ExperimentalApi::class)
    suspend fun sendMessage(message: String) {
        val instance = _chatState.value.instance ?: return

        _chatState.update { it.copy(
            isChatRunning = true,
        ) }
        
        val userMessage =
            ChatMessage(
                message = message,
                type = ChatType.CHAT,
                completedAt = System.currentTimeMillis()
            )
        val currentMessages = _chatState.value.messages.toMutableList()
        currentMessages.add(userMessage)
        
        // Add placeholder for AI response
        val aiMessage = ChatMessage(
            message = "",
            type = ChatType.ASSISTANT,
            isResponding = true
        )
        currentMessages.add(aiMessage)
        
        _chatState.update { it.copy(messages = currentMessages) }
        
        try {

            var fullResponse = ""
            val start = System.currentTimeMillis()
            var firstTokenTs = 0L
            var prefillTokens = 0
            var decodeTokens = 0
            var firstRun = true

            instance.conversation.sendMessageAsync(
                    Message.of(Content.Text(message)),
                ).catch { e ->
                    // Handle flow errors
                    throw e
                }.onCompletion { cause ->
                    if (cause == null) {
                        val curTs = System.currentTimeMillis()
                        val stats = calculateStatistics(start, firstTokenTs, curTs, prefillTokens, decodeTokens, true)
                        _chatState.update { state ->
                            val messages = state.messages.toMutableList()
                            if (messages.isNotEmpty() && messages.last().type == ChatType.ASSISTANT) {
                                messages[messages.lastIndex] = messages.last().copy(
                                    message = fullResponse,
                                    statistics = stats,
                                    completedAt = System.currentTimeMillis(),
                                    isResponding = false,
                                )
                            }
                            state.copy(messages = messages, isChatRunning = false)
                        }
                    } else {
                        _chatState.update { it.copy(isChatRunning = false) }
                    }

                }.collect { msg ->
                    val curTs = System.currentTimeMillis()
                    if (firstRun) {
                        firstTokenTs = curTs
                        prefillTokens = instance.conversation.getBenchmarkInfo().lastPrefillTokenCount
                        firstRun = false
                    } else {
                        decodeTokens++
                    }

                    // Assuming msg.toString() gives text content, needs verification
                    fullResponse += msg.toString()
                    
                    _chatState.update { state ->
                        val messages = state.messages.toMutableList()
                        if (messages.isNotEmpty() && messages.last().type == ChatType.ASSISTANT) {
                            messages[messages.lastIndex] = messages.last().copy(
                                message = fullResponse,
                                isResponding = false,
                            )
                        }
                        state.copy(messages = messages)
                    }
                }

        } catch (e: Exception) {
             _chatState.update { state ->
                val messages = state.messages.toMutableList()
                if (messages.isNotEmpty() && messages.last().type == ChatType.ASSISTANT) {
                    messages[messages.lastIndex] = messages.last().copy(message = "Error: ${e.message}")
                }
                state.copy(messages = messages, isChatRunning = false)
            }
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