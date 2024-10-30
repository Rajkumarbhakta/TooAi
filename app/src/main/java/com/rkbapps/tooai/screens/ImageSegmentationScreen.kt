package com.rkbapps.tooai.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.rkbapps.tooai.utils.ProgressDialog
import com.rkbapps.tooai.utils.saveImage
import java.io.IOException


class ImageSegmentationScreen() : Screen {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.current

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

        val bitmap = remember {
            mutableStateOf<Bitmap?>(null)
        }

        val isDialogVisible = remember {
            mutableStateOf(false)
        }

        val isProcessing = remember {
            mutableStateOf(false)
        }

        LaunchedEffect(key1 = Unit) {
            galleryLauncher.launch("image/*")
        }

        Scaffold(
            topBar = {
                TopBar(title = "Image Segmentation") {
                    navigator!!.pop()
                }
            }
        ) { paddingValues ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (imageUri.value == null) {
                    TextButton(onClick = {
                        galleryLauncher.launch("image/*")
                    }) {
                        Text(
                            text = "Select an image.",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    AsyncImage(
                        model = imageUri.value,
                        contentDescription = "",
                        modifier = Modifier.weight(1f).padding(8.dp).clickable {
                                galleryLauncher.launch("image/*")
                            }
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        onClick = {
                            isProcessing.value = true
                            try {
                                val inputImage = InputImage.fromFilePath(context, imageUri.value!!)
                                val options = SubjectSegmenterOptions.Builder()
                                    .enableForegroundBitmap()
                                val segmenter = SubjectSegmentation.getClient(options.build())
                                segmenter.process(inputImage)
                                    .addOnSuccessListener { result ->
                                        val foregroundBitmap = result.foregroundBitmap
                                        bitmap.value = foregroundBitmap
                                        isDialogVisible.value = true
                                        isProcessing.value = false
                                    }.addOnFailureListener {
                                        Toast.makeText(context, "${it.message}", Toast.LENGTH_SHORT)
                                            .show()
                                        isProcessing.value = false
                                    }
                            } catch (e: IOException) {
                                e.printStackTrace()
                                isProcessing.value = false
                            }
                        }) {
                        Text(text = "Detect")
                    }
                }

                if (isProcessing.value) {
                    ProgressDialog(isVisible = isProcessing, dismissible = false)
                }

                if (isDialogVisible.value && bitmap.value != null) {

                    Dialog(onDismissRequest = { isDialogVisible.value = false }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f).
                            background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "Result", modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            AsyncImage(
                                model = bitmap.value,
                                contentDescription = "",
                                modifier = Modifier.weight(1f).padding(8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = { isDialogVisible.value = false }) {
                                    Text(text = "Dismiss")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(modifier = Modifier.weight(1f), onClick = {
                                    isProcessing.value = true
                                    if (context.saveImage(bitmap.value!!, "")) {
                                        isDialogVisible.value = false
                                        Toast.makeText(
                                            context,
                                            "Saved in pictures folder.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isProcessing.value = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Something went wrong.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isProcessing.value = false
                                    }

                                }) {
                                    Text(text = "Save")
                                }
                            }
                        }
                    }


                }
            }

        }


    }


}