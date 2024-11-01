package com.rkbapps.tooai.repository

import com.rkbapps.tooai.db.dao.RecognizedTextDao
import com.rkbapps.tooai.db.entity.RecognizedText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import javax.inject.Inject

class RecognizedTextRepository @Inject constructor(private val recognizedTextDao: RecognizedTextDao) {

    private val _recognizedTextList = MutableStateFlow<List<RecognizedText>>(emptyList())
    val recognizedTextList: StateFlow<List<RecognizedText>> = _recognizedTextList

    suspend fun insertRecognizedText(recognizedText: RecognizedText): Boolean {
        return try {
            recognizedTextDao.newRecognizedText(recognizedText)
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