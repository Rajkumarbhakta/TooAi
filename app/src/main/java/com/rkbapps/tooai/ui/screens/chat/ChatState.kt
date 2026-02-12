package com.rkbapps.tooai.ui.screens.chat

import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.UiState

data class ChatState(
    val llmModel: LlmModel? = null,
    val instance: LlmModelInstance? = null,
    val modelInitializingStatus: UiState<Boolean> = UiState(),
    val messages: List<ChatMessage> = emptyList()
)

data class LlmModelInstance(val engine: Engine, var conversation: Conversation)

data class ChatMessage(
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
