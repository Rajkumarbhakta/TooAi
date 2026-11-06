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

    // The image being read, shown while recognition runs. Held here so it survives rotation.
    private val _pendingImage = MutableStateFlow<Uri?>(null)
    val pendingImage = _pendingImage.asStateFlow()

    fun recognize(context: Context, uri: Uri) {
        _pendingImage.value = uri
        viewModelScope.launch {
            repository.recognize(context, uri)
        }
    }

    fun dismissResult() {
        _pendingImage.value = null
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
