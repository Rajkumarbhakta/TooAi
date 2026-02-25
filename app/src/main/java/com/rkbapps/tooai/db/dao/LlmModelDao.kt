package com.rkbapps.tooai.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rkbapps.tooai.db.entity.LlmModel
import kotlinx.coroutines.flow.Flow

@Dao
interface LlmModelDao {

    @Insert
    suspend fun insertLlmModel(llmModel: LlmModel)

    @Update
    suspend fun updateLlmModel(llmModel: LlmModel)

    @Delete
    suspend fun deleteLlmModel(llmModel: LlmModel)

    @Query("SELECT * FROM llmmodel")
    fun getAllLlmModels(): Flow<List<LlmModel>>


    @Query("SELECT * FROM llmmodel WHERE id = :id")
    suspend fun getLlmModelById(id: Long): LlmModel



}