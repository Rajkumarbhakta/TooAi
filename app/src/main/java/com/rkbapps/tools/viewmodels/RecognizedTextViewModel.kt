package com.rkbapps.tools.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkbapps.tools.db.entity.RecognizedText
import com.rkbapps.tools.repository.RecognizedTextRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecognizedTextViewModel @Inject constructor(private val repository: RecognizedTextRepository) :
    ViewModel() {

    val recognizedTextList: StateFlow<List<RecognizedText>> = repository.recognizedTextList


    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllRecognizedText()
        }
    }


    fun insert(recognizedText: RecognizedText) {
        viewModelScope.async(Dispatchers.IO) {
            repository.insertRecognizedText(recognizedText)
        }.let {

        }
    }

    fun delete(recognizedText: RecognizedText) {
        viewModelScope.async(Dispatchers.IO) {
            repository.deleteRecognizedText(recognizedText)
        }.let {

        }
    }


}