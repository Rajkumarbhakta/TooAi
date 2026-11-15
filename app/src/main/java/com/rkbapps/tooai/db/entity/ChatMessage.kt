package com.rkbapps.tooai.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"])
    ]
)
data class ChatMessage(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val sender: String, // "USER" or "ASSISTANT"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeToFirstToken: Float? = null,
    val prefillSpeed: Float? = null,
    val decodeSpeed: Float? = null,
    val totalLatency: Float? = null,
    val tokenUsed: Int? = null,
    val isError: Boolean = false,
    // The display name of the model that wrote a reply, as it was at the time. A name rather
    // than an id, so the label survives the model being deleted. Null on the user's messages.
    val modelName: String? = null
)
