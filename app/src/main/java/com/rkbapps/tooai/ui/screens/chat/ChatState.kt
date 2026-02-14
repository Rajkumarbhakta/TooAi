package com.rkbapps.tooai.ui.screens.chat

import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.UiState
import java.util.UUID

data class ChatState(
    val llmModel: LlmModel? = null,
    val instance: LlmModelInstance? = null,
    val modelInitializingStatus: UiState<Boolean> = UiState(),
    val isChatRunning: Boolean = false,
    val messages: List<ChatMessage> = emptyList()
)

data class LlmModelInstance(val engine: Engine, var conversation: Conversation)

data class ChatMessage(
    val id: UUID = UUID.randomUUID(),
    val message: String,
    val type: ChatType,
    val timestamp: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isResponding: Boolean = false,
    val statistics: ChatStatistics? = null
)

data class ChatStatistics(
    val timeToFirstToken: Float? = null,
    val prefillSpeed: Float? = null,
    val decodeSpeed: Float? = null,
    val totalLatency: Float? = null
)


enum class ChatType{
    CHAT,
    ASSISTANT
}
