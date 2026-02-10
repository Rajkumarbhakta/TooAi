package com.rkbapps.tooai.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LlmModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val displayName: String,
    val sizeInBytes: Long,
    val path: String,
    val fileLocation: String,
    val maxTokens: Int,
    val topK: Int,
    val topP: Double,
    val temperature: Double,
    val createdAt: Long,
)
