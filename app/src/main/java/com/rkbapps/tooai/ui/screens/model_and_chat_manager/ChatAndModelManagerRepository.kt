package com.rkbapps.tooai.ui.screens.model_and_chat_manager

import android.content.Context
import android.net.Uri
import android.util.Log
import com.rkbapps.tooai.db.dao.LlmModelDao
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.ModelConfigs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class ChatAndModelManagerRepository @Inject constructor(
    private val llmModelDao: LlmModelDao
) {

    val llmModels = llmModelDao.getAllLlmModels()


    fun importModel(
        context: Context,
        scope: CoroutineScope,
        fileSize: Long,
        fileName:String,
        uri: Uri,
        onDone:(path: String)-> Unit,
        onProgress:(progress: Float)-> Unit,
        onError:(message: String)-> Unit
    ) {
        scope.launch(Dispatchers.IO) {

            val decodeUri = URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8.name())
            Log.d("Importing File", "importing model from $decodeUri. File name: $fileName. File size: $fileSize")
            val importDir = File(context.getExternalFilesDir(null), ModelConfigs.IMPORT_DIR)
            if (!importDir.exists()) {
                importDir.mkdir()
            }
            val outputFile =
                File(context.getExternalFilesDir(null), "${ModelConfigs.IMPORT_DIR}/$fileName")
            Log.d("Importing File", "importing model to ${outputFile.absolutePath}")
            Log.d("Importing File", "importing model to ${importDir.absolutePath}")
            val outputStream = FileOutputStream(outputFile)
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead: Int
            var lastSetProgressTs: Long = 0
            var importedBytes = 0L
            val inputStream = context.contentResolver.openInputStream(uri)

            try {
                if (inputStream != null) {
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        importedBytes += bytesRead
                        val curTs = System.currentTimeMillis()
                        if (curTs - lastSetProgressTs > 200) {
                            lastSetProgressTs = curTs
                            if (fileSize != 0L) {
                                onProgress(importedBytes.toFloat() / fileSize.toFloat())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to import")
                return@launch;
            } finally {
                val path = outputFile.absolutePath
                inputStream?.close()
                outputStream.close()
                onProgress(1f)
                onDone(path)
            }
        }
    }

    suspend fun addNewModel(model: LlmModel){
        try {
            llmModelDao.insertLlmModel(model)
        }catch (e: Exception){
            Log.e("Importing File","Failed to save in database",e)
        }
    }

    suspend fun updateNewModel(model: LlmModel){
        try {
            llmModelDao.updateLlmModel(model)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


}