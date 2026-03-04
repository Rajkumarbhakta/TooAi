package com.rkbapps.tooai.ui.screens.doc_scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.FileProvider
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.rkbapps.tooai.BuildConfig
import com.rkbapps.tooai.db.dao.DocumentScansDao
import com.rkbapps.tooai.db.entity.DocumentScans
import com.rkbapps.tooai.utils.ScanModes
import com.rkbapps.tooai.utils.UiState
import com.rkbapps.tooai.utils.savePdfToDocuments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DocumentScannerRepository @Inject constructor(
    private val documentScanDao: DocumentScansDao,
) {

    private val options = GmsDocumentScannerOptions.Builder()
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
        )
        .setGalleryImportAllowed(true)
        .setScannerMode(ScanModes.FULL_MODE)
    private val client = GmsDocumentScanning.getClient(options.build())


    val documents = documentScanDao.getAllDocumentScans()

    private val _docSavingState = MutableStateFlow<UiState<DocumentScans?>>(UiState())
    val docSavingState = _docSavingState.asStateFlow()

    fun startScan(
        activity: Activity,
        onScanResult: (IntentSenderRequest) -> Unit,
        onScanError: (Exception) -> Unit
    ) {
        try {
            client.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender: IntentSender ->
                    val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                    onScanResult(intentSenderRequest)
                }
                .addOnFailureListener() { e: Exception ->
                    onScanError(e)
                }
        }catch (e: Exception){
            onScanError(e)
        }
    }




    suspend fun saveDocument(context: Context, uri: Uri) = withContext(Dispatchers.IO){
        _docSavingState.value = UiState(isLoading = true)
        val doc = savePdfToDocuments(context, sourceUri = uri, "TooAi")
        if (doc != null){
            val scanDoc = DocumentScans(
                id = 0,
                path = doc.path,
                title = doc.title,
                timeMillis = doc.timeMillis
            )
            documentScanDao.newDocumentScan(scanDoc)
            _docSavingState.value = UiState(data = scanDoc)
        }else{
            _docSavingState.value = UiState(error = "Unable to save PDF.")
        }
        delay(1000)
        _docSavingState.value = UiState()
    }

    suspend fun deleteDocument(context: Context,documentScan: DocumentScans){
        try {
            context.contentResolver.delete(Uri.parse(documentScan.path), null, null) > 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        documentScanDao.deleteDocumentScan(documentScan)
    }

}