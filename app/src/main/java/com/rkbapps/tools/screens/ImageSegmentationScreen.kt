package com.rkbapps.tools.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.screen.Screen
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.IOException


class ImageSegmentationScreen() : Screen {
    @Composable
    override fun Content() {
        val imageUri = remember {
            mutableStateOf<Uri?>(null)
        }
        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri ->
                uri?.let {
                    imageUri.value = it
                }
            }
        )
        val context = LocalContext.current
        val bitmap = remember {
            mutableStateOf<Bitmap?>(null)
        }
        Column(
            Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (imageUri.value == null) {
                TextButton(onClick = {
                    galleryLauncher.launch("image/*")
                }) {
                    Text(text = "Select an image.")
                }
            } else {
                AsyncImage(
                    model = if (bitmap.value != null) bitmap.value else imageUri.value,
                    contentDescription = ""
                )
                Button(onClick = {
                    try {
                        val inputImage = InputImage.fromFilePath(context, imageUri.value!!)
                        val options = SubjectSegmenterOptions.Builder()
                            .enableForegroundBitmap()
                        val segmenter = SubjectSegmentation.getClient(options.build())
                        segmenter.process(inputImage)
                            .addOnSuccessListener { result ->
                                val foregroundBitmap = result.foregroundBitmap
                                bitmap.value = foregroundBitmap
                            }.addOnFailureListener {
                                Toast.makeText(context, "${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }) {
                    Text(text = "Detect")
                }
            }
        }


    }

}