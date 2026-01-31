package com.rkbapps.tooai.ui.screens.doc_scanner

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.rkbapps.tooai.db.entity.DocumentScans
import com.rkbapps.tooai.models.docScanner
import com.rkbapps.tooai.utils.ScanModes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentScannerViewModel @Inject constructor(
    private val repository: DocumentScannerRepository,
    ): ViewModel() {

    val documents = repository.documents.stateIn(
        viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val docSavingState = repository.docSavingState



    fun startScan(
        activity: Activity,
        onScanResult: (IntentSenderRequest) -> Unit,
        onScanError: (Exception) -> Unit
    ){
        repository.startScan(activity = activity,onScanResult = onScanResult, onScanError = onScanError)
    }

    fun saveDocument(context: Context, uri: Uri){
        viewModelScope.launch {
            repository.saveDocument(context, uri)
        }
    }

    fun deleteDocument(context: Context,documentScan: DocumentScans){
        viewModelScope.launch {
            repository.deleteDocument(context,documentScan)
        }
    }







}