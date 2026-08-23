package com.rkbapps.tooai.ui.screens.image_segmentation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageSegmentationViewModel @Inject constructor(
    private val repository: ImageSegmentationRepository
) : ViewModel() {

    val segmentationState = repository.segmentationState
    val imageSavingStatus = repository.imageSavingStatus

    fun segment(context: Context, uri: Uri) {
        viewModelScope.launch {
            repository.segment(context, uri)
        }
    }

    fun resetState() {
        repository.resetState()
    }

    fun saveToGallery(context: Context, bitmap: Bitmap){
        viewModelScope.launch {
            repository.saveToGallery(context,bitmap)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.close() // releases native SubjectSegmenter resources
    }
}
