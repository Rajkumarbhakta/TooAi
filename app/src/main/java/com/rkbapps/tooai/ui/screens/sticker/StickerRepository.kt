package com.rkbapps.tooai.ui.screens.sticker

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.rkbapps.tooai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val SAVE_PATH = "Pictures/TooAi/Stickers"
private const val STICKER_CACHE_COUNT = 4

class StickerRepository @Inject constructor() {

    // A bitmap can't travel in a navigation route, so the cut-out is handed to the editor as a
    // file. Returns its path, or null when the cut-out has nothing visible in it.
    suspend fun writeSource(context: Context, cutout: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val content = prepareContent(cutout) ?: return@withContext null
            val directory = File(context.cacheDir, "sticker_source").apply { mkdirs() }
            directory.listFiles()?.forEach { it.delete() }
            val file = File(directory, "source_${System.currentTimeMillis()}.png")
            file.outputStream().use { content.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun loadSource(path: String): StickerContent? = withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeFile(path)?.let(::StickerContent)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // The clipboard can still point at an earlier sticker, so the last few files stay.
    suspend fun writeStickerFile(context: Context, webp: ByteArray): Uri? = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.cacheDir, "stickers").apply { mkdirs() }
            directory.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?.drop(STICKER_CACHE_COUNT - 1)
                ?.forEach { it.delete() }
            val file = File(directory, "sticker_${System.currentTimeMillis()}.webp")
            file.writeBytes(webp)
            FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveToGallery(context: Context, webp: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "sticker_${System.currentTimeMillis()}.webp")
                put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, SAVE_PATH)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false
            resolver.openOutputStream(uri)?.use { it.write(webp) } ?: return@withContext false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
