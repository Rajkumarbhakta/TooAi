package com.rkbapps.tooai.ui.screens.text_recognitation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkbapps.tooai.db.entity.RecognizedText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecognizedTextViewModel @Inject constructor(
    private val repository: RecognizedTextRepository,
) : ViewModel() {

    val recognizedTextList = repository.recognizedTextList.stateIn(
        viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val recognitionState = repository.recognitionState

    // Image chosen but not yet recognized. Held here so it survives configuration changes.
    private val _pendingImage = MutableStateFlow<Uri?>(null)
    val pendingImage = _pendingImage.asStateFlow()

    // A history row the user tapped. Null while the result dialog is showing a fresh scan.
    private val _selectedHistoryItem = MutableStateFlow<RecognizedText?>(null)
    val selectedHistoryItem = _selectedHistoryItem.asStateFlow()

    fun onImagePicked(uri: Uri) {
        _pendingImage.value = uri
        repository.resetState()
    }

    fun clearPendingImage() {
        _pendingImage.value = null
        repository.resetState()
    }

    fun recognize(context: Context, uri: Uri) {
        viewModelScope.launch {
            repository.recognize(context, uri)
        }
    }

    fun onHistoryItemSelected(recognizedText: RecognizedText) {
        _selectedHistoryItem.value = recognizedText
    }

    fun dismissResult() {
        _selectedHistoryItem.value = null
        repository.resetState()
    }

    fun delete(recognizedText: RecognizedText) {
        viewModelScope.launch {
            repository.deleteRecognizedText(recognizedText)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.close() // releases the native ML Kit TextRecognizer
    }
}