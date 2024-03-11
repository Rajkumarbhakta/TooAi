package com.rkbapps.tools.utils

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import com.google.mlkit.vision.barcode.common.Barcode
import com.rkbapps.tools.db.QrScan
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.OutputStream

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}



fun Context.saveImage(bitmap: Bitmap, folderName: String): Boolean {
    return if (Build.VERSION.SDK_INT >= 29) {
        val values = contentValues()
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
        values.put(MediaStore.Images.Media.IS_PENDING, true)
        // RELATIVE_PATH and IS_PENDING are introduced in API 29.
        val uri: Uri? =
            this.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            saveImageToStream(bitmap, this.contentResolver.openOutputStream(uri))
            values.put(MediaStore.Images.Media.IS_PENDING, false)
            this.contentResolver.update(uri, values, null, null)
            true
        } else {
            false
        }
    } else {
        val directory =
            File(Environment.getExternalStorageDirectory().toString() + separator + folderName)
        // getExternalStorageDirectory is deprecated in API 29
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val fileName = System.currentTimeMillis().toString() + ".png"
        val file = File(directory, fileName)
        saveImageToStream(bitmap, FileOutputStream(file))
        if (file.absolutePath != null) {
            val values = contentValues()
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            // .DATA is deprecated in API 29
            this.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            true
        } else {
            false
        }
    }
}

private fun contentValues(): ContentValues {
    val values = ContentValues()
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
    values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
    return values
}

private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
    if (outputStream != null) {
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}








