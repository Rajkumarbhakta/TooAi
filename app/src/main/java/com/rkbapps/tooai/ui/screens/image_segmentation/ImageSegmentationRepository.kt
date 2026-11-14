package com.rkbapps.tooai.ui.screens.image_segmentation

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.rkbapps.tooai.BuildConfig
import com.rkbapps.tooai.utils.UiState
import com.rkbapps.tooai.utils.saveImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.graphics.createBitmap

private const val SAVE_FOLDER = "TooAi"
private const val RECENT_CUTOUT_COUNT = 10
private const val MAX_DIMENSION = 4096

class ImageSegmentationRepository @Inject constructor() {

    // Per-subject bitmaps only. With these enabled, the segmenter's own foreground bitmap came
    // back as noise on a Samsung S-series phone, so the combined cut-out is drawn from the subjects.
    private val segmenter  = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder()
            .enableMultipleSubjects(
                SubjectSegmenterOptions.SubjectResultOptions.Builder()
                    .enableSubjectBitmap()
                    .build()
            )
            .build()
    )


    private val _segmentationState = MutableStateFlow<UiState<SegmentationResult>>(UiState())
    val segmentationState = _segmentationState.asStateFlow()

    private val _imageSavingStatus = MutableStateFlow<UiState<Boolean>>(UiState())
    val imageSavingStatus = _imageSavingStatus.asStateFlow()

    private val _recentCutouts = MutableStateFlow<List<Uri>>(emptyList())
    val recentCutouts = _recentCutouts.asStateFlow()

    suspend fun segment(context: Context, imageUri: Uri) = withContext(Dispatchers.IO) {
        _segmentationState.value = UiState(isLoading = true)
        try {
            val startTime = SystemClock.elapsedRealtime()
            val bitmap = decodeSoftwareBitmap(context, imageUri)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            val segResult = runSegmenter(segmenter, inputImage).getOrThrow()

            val subjects: List<SubjectResult> = segResult.subjects.mapNotNull { subject ->
                val bmp = subject.bitmap ?: return@mapNotNull null
                SubjectResult(
                    bitmap = bmp,
                    startX = subject.startX,
                    startY = subject.startY
                )
            }
            if (subjects.isEmpty()) {
                _segmentationState.value = UiState(error = "No clear subject found. Try a photo with one main subject.")
                return@withContext
            }
            val foregroundBitmap = createBitmap(bitmap.width, bitmap.height, config = Bitmap.Config.ARGB_8888)
            Canvas(foregroundBitmap).apply {
                subjects.forEach { drawBitmap(it.bitmap, it.startX.toFloat(), it.startY.toFloat(), null) }
            }

            _segmentationState.value = UiState(
                data = SegmentationResult(
                    source = bitmap,
                    foregroundBitmap = foregroundBitmap,
                    subjects = subjects,
                    durationMillis = SystemClock.elapsedRealtime() - startTime
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

    // ImageDecoder applies the EXIF rotation, so camera photos aren't segmented sideways.
    // BitmapFactory, the only option before Android 9, ignores it. Both cap the long edge at
    // MAX_DIMENSION: the result is drawn on screen, and larger bitmaps can exceed the GPU's
    // texture limit as well as memory.
    private fun decodeSoftwareBitmap(context: Context, uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longEdge = maxOf(info.size.width, info.size.height)
                if (longEdge > MAX_DIMENSION) {
                    val scale = MAX_DIMENSION.toFloat() / longEdge
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt(),
                        (info.size.height * scale).toInt()
                    )
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_DIMENSION) sampleSize *= 2
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleSize
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IOException("Cannot open image URI: $uri")
    }

    suspend fun saveToGallery(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        _imageSavingStatus.value = UiState(isLoading = true)
        try {
            val result  = async {
                saveImage(context,bitmap, SAVE_FOLDER)
            }.await()
            _imageSavingStatus.value = UiState(data = result)
            if (result) loadRecentCutouts(context)
        }catch (e: Exception){
            _imageSavingStatus.value = UiState(error = e.localizedMessage)
        }
        delay(500.milliseconds)
        _imageSavingStatus.value = UiState()
    }

    // The PNGs this app saved to Pictures/TooAi, newest first. An app can list its own media
    // without a permission from Android 10, which is also when saveImage starts writing there.
    suspend fun loadRecentCutouts(context: Context) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext
        val uris = mutableListOf<Uri>()
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Images.Media.MIME_TYPE} = ?",
                arrayOf("Pictures/$SAVE_FOLDER%", "image/png"),
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext() && uris.size < RECENT_CUTOUT_COUNT) {
                    uris += ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idColumn)
                    )
                }
            }
            _recentCutouts.value = uris
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Shares go through the cache so nothing is added to the gallery until the user saves.
    suspend fun writeShareFile(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.cacheDir, "cutouts").apply { mkdirs() }
            directory.listFiles()?.forEach { it.delete() }
            val file = File(directory, "cutout_${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class SubjectResult(
    val bitmap: Bitmap,
    val startX: Int,
    val startY: Int
)

data class SegmentationResult(
    val source: Bitmap,
    val foregroundBitmap: Bitmap,
    val subjects: List<SubjectResult>,
    val durationMillis: Long
)
