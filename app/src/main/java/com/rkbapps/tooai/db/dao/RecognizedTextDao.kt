package com.rkbapps.tooai.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.rkbapps.tooai.db.entity.RecognizedText
import kotlinx.coroutines.flow.Flow

@Dao
interface RecognizedTextDao {

    @Insert
    suspend fun newRecognizedText(recognizedText: RecognizedText): Long

    @Delete
    suspend fun deleteRecognizedText(recognizedText: RecognizedText)

    @Query("select * from recognizedtext")
    fun getAllRecognizedText(): Flow<List<RecognizedText>>

}