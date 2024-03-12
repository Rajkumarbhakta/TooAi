package com.rkbapps.tools.repository

import com.rkbapps.tools.db.dao.RecognizedTextDao
import com.rkbapps.tools.db.entity.RecognizedText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import javax.inject.Inject

class RecognizedTextRepository @Inject constructor(private val recognizedTextDao: RecognizedTextDao) {

    private val _recognizedTextList = MutableStateFlow<List<RecognizedText>>(emptyList())
    val recognizedTextList: StateFlow<List<RecognizedText>> = _recognizedTextList

    suspend fun insertRecognizedText(recognizedText: RecognizedText): Boolean {
        return try {
            val id = recognizedTextDao.newRecognizedText(recognizedText)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteRecognizedText(recognizedText: RecognizedText): Boolean {
        return try {
            recognizedTextDao.deleteRecognizedText(recognizedText)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getAllRecognizedText() {
        try {
            _recognizedTextList.emitAll(recognizedTextDao.getAllRecognizedText())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}