package com.rkbapps.tooai.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.rkbapps.tooai.db.entity.DocumentScans
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentScansDao {

    @Insert
    suspend fun newDocumentScan(documentScans: DocumentScans): Long

    @Delete
    suspend fun deleteDocumentScan(documentScans: DocumentScans)

    @Query("select * from documentscans")
    fun getAllDocumentScans(): Flow<List<DocumentScans>>



}