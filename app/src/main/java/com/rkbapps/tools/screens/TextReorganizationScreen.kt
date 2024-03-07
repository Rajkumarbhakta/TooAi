package com.rkbapps.tools.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

class TextReorganizationScreen : Screen {
    @Composable
    override fun Content() {
        val context = LocalContext.current
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

        val resultText = remember {
            mutableStateOf("")
        }

        val isDialogVisible = remember {
            mutableStateOf(false)
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
                    model = imageUri.value,
                    contentDescription = ""
                )
                Button(onClick = {
                    try {
                        val inputImage = InputImage.fromFilePath(context, imageUri.value!!)
                        val recognizer =
                            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        val result = recognizer.process(inputImage)
                            .addOnSuccessListener { text ->

                                resultText.value = text.text
                                isDialogVisible.value = true

                            }.addOnFailureListener {

                            }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }) {
                    Text(text = "Detect")
                }


                if (isDialogVisible.value) {
                    Dialog(onDismissRequest = { isDialogVisible.value = false }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.7f)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                )
                        ) {
                            Text(
                                text = "Recognized Text",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,

                                textAlign = TextAlign.Center
                            )
                            SelectionContainer(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = resultText.value, textAlign = TextAlign.Justify,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                    val clipboard: ClipboardManager =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("label", resultText.value)
                                    clipboard.setPrimaryClip(clip)
                                }) {
                                    Text(text = "Copy")
                                }

                            }
                        }


                    }
                }

            }

        }

    }


}