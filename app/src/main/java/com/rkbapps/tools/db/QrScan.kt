package com.rkbapps.tools.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class QrScan(

    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val displayValue: String?,
    val rawVale: String?,
    val format: Int?,
    val valueType: Int?,
    val timeMillis:Long
)
