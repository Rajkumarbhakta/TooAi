package com.rkbapps.tools.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RecognizedText(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val content: String
)