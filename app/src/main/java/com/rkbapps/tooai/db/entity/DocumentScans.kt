package com.rkbapps.tooai.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class DocumentScans (
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val path: String,
    val title: String,
    val timeMillis: Long
)