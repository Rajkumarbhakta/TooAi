package com.rkbapps.tooai.ui.screens.text_recognitation

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.rkbapps.tooai.db.dao.RecognizedTextDao
import com.rkbapps.tooai.db.entity.RecognizedText
import com.rkbapps.tooai.utils.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

class RecognizedTextRepository @Inject constructor(
    private val recognizedTextDao: RecognizedTextDao,
) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val recognizedTextList: Flow<List<RecognizedText>> = recognizedTextDao.getAllRecognizedText()

    private val _recognitionState = MutableStateFlow<UiState<RecognizedText>>(UiState())
    val recognitionState = _recognitionState.asStateFlow()

    suspend fun recognize(context: Context, imageUri: Uri) = withContext(Dispatchers.IO) {
        _recognitionState.value = UiState(isLoading = true)
        try {
            // fromFilePath reads and decodes the image — must stay off the main thread.
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val text = runRecognizer(inputImage).getOrThrow()
            if (text.text.isBlank()) {
                _recognitionState.value = UiState(error = "No text found in this image.")
                return@withContext
            }
            val recognizedText = RecognizedText(id = 0, content = text.text)
            val id = recognizedTextDao.newRecognizedText(recognizedText)
            _recognitionState.value = UiState(data = recognizedText.copy(id = id))
        } catch (e: Exception) {
            _recognitionState.value =
                UiState(error = e.localizedMessage ?: "Unable to read text from this image.")
        }
    }

    suspend fun deleteRecognizedText(recognizedText: RecognizedText) = withContext(Dispatchers.IO) {
        try {
            recognizedTextDao.deleteRecognizedText(recognizedText)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetState() {
        _recognitionState.value = UiState()
    }

    fun close() {
        recognizer.close()
    }

    private suspend fun runRecognizer(inputImage: InputImage): Result<Text> =
        suspendCancellableCoroutine { cont ->
            recognizer.process(inputImage)
                .addOnSuccessListener { cont.resume(Result.success(it)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
}