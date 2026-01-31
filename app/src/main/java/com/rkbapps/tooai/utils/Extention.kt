package com.rkbapps.tooai.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.rkbapps.tooai.models.PdfDoc
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

fun Context.copyText(text: String) {
    val clipboard: ClipboardManager =
        this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(
        "label",
        text
    )
    clipboard.setPrimaryClip(clip)
    Toast.makeText(this, "Copied", Toast.LENGTH_SHORT)
        .show()
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

fun savePdfToDocuments(context: Context, sourceUri: Uri,destinationPath: String): PdfDoc? {
    val resolver = context.contentResolver
    val time = System.currentTimeMillis()
    val fileName = "TooAi Scan - $time.pdf"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + separator+destinationPath)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

    val collection = MediaStore.Files.getContentUri("external")

    val itemUri = resolver.insert(collection, contentValues) ?: return null

    try {
        resolver.openOutputStream(itemUri)?.use { output ->
            resolver.openInputStream(sourceUri)?.use { input ->
                input.copyTo(output)
            }
        }

        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(itemUri, contentValues, null, null)

        return PdfDoc(
            title = fileName,
            path = itemUri.toString(),
            timeMillis = time
        )

    } catch (e: Exception) {
        resolver.delete(itemUri, null, null)
        Log.e("SavePdf", "Failed", e)
        return null
    }
}


fun Long.toDateTimeString(
    pattern: String = "dd MMM yyyy HH:mm:ss",
    locale: Locale = Locale.getDefault()
): String {
    return if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val instant = Instant.ofEpochMilli(this)
        val zdt = instant.atZone(ZoneId.systemDefault())
        zdt.format(DateTimeFormatter.ofPattern(pattern).withLocale(locale))
    } else {
        val sdf = java.text.SimpleDateFormat(pattern, locale)
        sdf.timeZone = TimeZone.getDefault()
        sdf.format(Date(this))
    }
}







