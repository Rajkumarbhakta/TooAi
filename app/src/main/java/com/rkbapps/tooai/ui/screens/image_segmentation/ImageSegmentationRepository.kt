package com.rkbapps.tooai.ui.screens.image_segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.rkbapps.tooai.utils.UiState
import com.rkbapps.tooai.utils.saveImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

class ImageSegmentationRepository @Inject constructor() {

    private val segmenter  = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
    )


    private val _segmentationState = MutableStateFlow<UiState<SegmentationResult>>(UiState())
    val segmentationState = _segmentationState.asStateFlow()

    private val _imageSavingStatus = MutableStateFlow<UiState<Boolean>>(UiState())
    val imageSavingStatus = _imageSavingStatus.asStateFlow()

    suspend fun segment(context: Context, imageUri: Uri) = withContext(Dispatchers.IO) {
        _segmentationState.value = UiState(isLoading = true)
        try {
            val bitmap = decodeSoftwareBitmap(context, imageUri)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // Single segmenter call — result contains both foreground and per-subject data
            val segResult = runSegmenter(segmenter, inputImage).getOrThrow()

            val foregroundBitmap = segResult.foregroundBitmap
            val subjects: List<SubjectResult> = segResult.subjects.mapNotNull { subject ->
                val bmp = subject.bitmap ?: return@mapNotNull null
                SubjectResult(
                    bitmap = bmp,
                    startX = subject.startX,
                    startY = subject.startY
                )
            }

            _segmentationState.value = UiState(
                data = SegmentationResult(
                    foregroundBitmap = foregroundBitmap,
                    subjects = subjects
                )
            )
        } catch (e: Exception) {
            _segmentationState.value = UiState(error = e.message ?: "Unknown error occurred")
        }
    }

    fun close(){
        segmenter.close()
    }

    fun resetState() {
        _segmentationState.value = UiState()
    }

    private suspend fun runSegmenter(
        segmenter: SubjectSegmenter,
        inputImage: InputImage
    ): Result<SubjectSegmentationResult> = suspendCancellableCoroutine { cont ->
        segmenter.process(inputImage)
            .addOnSuccessListener { cont.resume(Result.success(it)) }
            .addOnFailureListener { cont.resume(Result.failure(it)) }
    }

    private fun decodeSoftwareBitmap(context: Context, uri: Uri): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IOException("Cannot open image URI: $uri")

        // Defensive: some devices may still return HARDWARE despite the hint — force a copy.
        return if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false).also { bitmap.recycle() }
        } else {
            bitmap
        }
    }

    suspend fun saveToGallery(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        _imageSavingStatus.value = UiState(isLoading = true)
        try {
            val result  = async {
                saveImage(context,bitmap, "TooAi")
            }.await()
            _imageSavingStatus.value = UiState(data = result)
        }catch (e: Exception){
            _imageSavingStatus.value = UiState(error = e.localizedMessage)
        }
        delay(500.milliseconds)
        _imageSavingStatus.value = UiState()
    }
}

data class SubjectResult(
    val bitmap: Bitmap,
    val startX: Int,
    val startY: Int
)

data class SegmentationResult(
    val foregroundBitmap: Bitmap?,
    val subjects: List<SubjectResult>
)
