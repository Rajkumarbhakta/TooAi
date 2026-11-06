package com.rkbapps.tooai.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RecognizedText(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val content: String,
    // 0 for rows saved before the column existed.
    @ColumnInfo(defaultValue = "0")
    val timeMillis: Long = 0
)