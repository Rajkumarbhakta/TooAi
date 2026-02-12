package com.rkbapps.tooai.ui.screens.chat

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.rkbapps.tooai.db.dao.LlmModelDao
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.text.indexOf

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

    suspend fun sendMessage(message: String) {
        val instance = _chatState.value.instance ?: return
        
        val userMessage = ChatMessage(message = message, isFromUser = true)
        val currentMessages = _chatState.value.messages.toMutableList()
        currentMessages.add(userMessage)
        
        // Add placeholder for AI response
        val aiMessage = ChatMessage(message = "", isFromUser = false)
        currentMessages.add(aiMessage)
        
        _chatState.update { it.copy(messages = currentMessages) }
        
        try {
            val responseFlow = instance.conversation.sendMessageAsync(Message.of(message))
            var fullResponse = ""
            
            responseFlow.collect { msg ->
                // Assuming msg.toString() gives text content, needs verification
                fullResponse += msg.toString()
                
                _chatState.update { state ->
                    val messages = state.messages.toMutableList()
                    if (messages.isNotEmpty() && !messages.last().isFromUser) {
                        messages[messages.lastIndex] = messages.last().copy(message = fullResponse)
                    }
                    state.copy(messages = messages)
                }
            }
        } catch (e: Exception) {
             _chatState.update { state ->
                val messages = state.messages.toMutableList()
                if (messages.isNotEmpty() && !messages.last().isFromUser) {
                    messages[messages.lastIndex] = messages.last().copy(message = "Error: ${e.message}")
                }
                state.copy(messages = messages)
            }
        }
    }

    fun getSystemPrompt(): String {
        @SuppressWarnings("JavaTimeDefaultTimeZone")
        val curDateTimeString =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        return "Current date and time given in YYYY-MM-DDTHH:MM:SS format: ${curDateTimeString}. " +
                "You are a model that can do function calling with the following functions"
    }

}