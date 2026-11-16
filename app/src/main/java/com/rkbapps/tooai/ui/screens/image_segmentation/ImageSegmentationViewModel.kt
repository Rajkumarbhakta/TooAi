package com.rkbapps.tooai.ui.screens.image_segmentation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.rkbapps.tooai.ui.screens.sticker.StickerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.graphics.createBitmap

@HiltViewModel
class ImageSegmentationViewModel @Inject constructor(
    private val repository: ImageSegmentationRepository,
    private val stickerRepository: StickerRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val segmentationState = repository.segmentationState
    val imageSavingStatus = repository.imageSavingStatus
    val recentCutouts = repository.recentCutouts

    private var stickerJob: Job? = null

    init {
        refreshRecentCutouts()
    }

    // Also called when the pick page comes back, since the sticker editor saves there too.
    fun refreshRecentCutouts() {
        viewModelScope.launch { repository.loadRecentCutouts(appContext) }
    }

    fun segment(context: Context, uri: Uri) {
        viewModelScope.launch {
            repository.segment(context, uri)
        }
    }

    fun resetState() {
        repository.resetState()
    }

    // background is null for a transparent PNG.
    fun saveToGallery(context: Context, bitmap: Bitmap, background: Color?) {
        viewModelScope.launch {
            repository.saveToGallery(context, withBackground(bitmap, background))
        }
    }

    fun share(context: Context, bitmap: Bitmap, background: Color?) {
        viewModelScope.launch {
            val uri = repository.writeShareFile(context, withBackground(bitmap, background))
            if (uri == null) {
                Toast.makeText(context, "Unable to share image.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share cut-out"))
        }
    }

    // Hands the cut-out to the sticker editor as a file and reports its path.
    fun prepareSticker(context: Context, cutout: Bitmap, onReady: (String) -> Unit) {
        if (stickerJob?.isActive == true) return
        stickerJob = viewModelScope.launch {
            val path = stickerRepository.writeSource(context, cutout)
            if (path == null) {
                Toast.makeText(context, "Unable to make a sticker from this cut-out.", Toast.LENGTH_SHORT).show()
            } else {
                onReady(path)
            }
        }
    }

    private suspend fun withBackground(bitmap: Bitmap, background: Color?): Bitmap {
        if (background == null) return bitmap
        return withContext(Dispatchers.Default) {
            val result = createBitmap(bitmap.width, bitmap.height,Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(result).apply {
                drawColor(background.toArgb())
                drawBitmap(bitmap, 0f, 0f, null)
            }
            result
        }
    }

    override fun onCleared() {
        repository.close() // releases native SubjectSegmenter resources
    }
}
