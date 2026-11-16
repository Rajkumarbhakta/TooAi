package com.rkbapps.tooai.ui.screens.chat

import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.ModelBackend
import com.rkbapps.tooai.utils.Prompts
import com.rkbapps.tooai.utils.UiState
import java.util.UUID

data class ChatState(
    val llmModel: LlmModel? = null,
    val instance: LlmModelInstance? = null,
    val modelInitializingStatus: UiState<Boolean> = UiState(),
    val isChatRunning: Boolean = false,
    val isResponding: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val sessionId: String? = null,
    val sessionTitle: String? = null,
    val currentPromptType: Prompts? = null,
    // The chat's model was deleted, or its file is gone. The history still shows.
    val modelUnavailable: Boolean = false
)

// backend is where the model actually loaded, which is CPU when a GPU load fell back.
data class LlmModelInstance(
    val engine: Engine,
    val conversation: Conversation,
    val backend: ModelBackend = ModelBackend.CPU
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: ChatType,
    val timestamp: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isResponding: Boolean = false,
    val statistics: ChatStatistics? = null,
    // A failed reply. Shown in the chat, but never handed back to the model as history.
    val isError: Boolean = false,
    // The model that wrote a reply. A chat can switch models, so this is per message.
    val modelName: String? = null
)

data class ChatStatistics(
    val timeToFirstToken: Float? = null,
    val prefillSpeed: Float? = null,
    val decodeSpeed: Float? = null,
    val totalLatency: Float? = null,
    val tokenUsed: Int? = null
)


enum class ChatType{
    CHAT,
    ASSISTANT
}
