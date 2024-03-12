package com.rkbapps.tools.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.rkbapps.tools.R
import com.rkbapps.tools.db.entity.RecognizedText
import com.rkbapps.tools.utils.ProgressDialog
import com.rkbapps.tools.utils.copyText
import com.rkbapps.tools.viewmodels.RecognizedTextViewModel
import java.io.IOException

class TextReorganizationScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.current
        val viewModel: RecognizedTextViewModel = getViewModel()
        val recognizedTextList = viewModel.recognizedTextList.collectAsState()
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

        val currentRecognizedText = remember {
            mutableStateOf<RecognizedText?>(null)
        }

        val isDialogVisible = remember {
            mutableStateOf(false)
        }

        val isProcessing = remember {
            mutableStateOf(false)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Text Recognition") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navigator!!.pop() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "navigation up",
                                tint = MaterialTheme.colorScheme.onPrimary

                            )
                        }
                    }
                )
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
                    if (recognizedTextList.value.isEmpty()) {
                        TextButton(onClick = {
                            galleryLauncher.launch("image/*")
                        }) {
                            LaunchedEffect(key1 = Unit) {
                                galleryLauncher.launch("image/*")
                            }
                            Text(
                                text = "Select an image.",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "History",
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleLarge
                            )
                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                items(
                                    count = recognizedTextList.value.size,
                                    key = { key ->
                                        recognizedTextList.value[key].id
                                    }
                                ) { position ->
                                    RecognizedTextItem(
                                        item = recognizedTextList.value[position],
                                        onIconClick = { text ->
                                            context.copyText(text)
                                        }) {
                                        currentRecognizedText.value =
                                            recognizedTextList.value[position]
                                        isDialogVisible.value = true
                                    }
                                }
                            }

                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                onClick = {
                                    galleryLauncher.launch("image/*")
                                }) {
                                Text(text = "Choose Image")
                            }
                        }

                    }
                } else {
                    AsyncImage(
                        model = imageUri.value,
                        contentDescription = "", modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .clickable {
                                galleryLauncher.launch("image/*")
                            }
                    )
                    Row {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            onClick = {
                                imageUri.value = null
                            }) {
                            Text(text = "Cancel")
                        }
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            onClick = {
                                isProcessing.value = true
                                try {
                                    val inputImage =
                                        InputImage.fromFilePath(context, imageUri.value!!)
                                    val recognizer =
                                        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                    val result = recognizer.process(inputImage)
                                        .addOnSuccessListener { text ->
//                                        resultText.value = text.text
                                            val recognizedText = RecognizedText(0, text.text)
                                            currentRecognizedText.value = recognizedText
                                            isProcessing.value = false
                                            isDialogVisible.value = true
                                            viewModel.insert(recognizedText = recognizedText)
                                        }.addOnFailureListener {
                                            Toast.makeText(
                                                context,
                                                "Error: ${it.localizedMessage}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isProcessing.value = false
                                        }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                    isProcessing.value = false
                                }
                            }) {
                            Text(text = "Start")
                        }
                    }



                }

                if (isProcessing.value) {
                    ProgressDialog(isVisible = isProcessing)
                }

                if (isDialogVisible.value && currentRecognizedText.value != null) {
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
                                    text = currentRecognizedText.value!!.content,
                                    textAlign = TextAlign.Justify,
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
                                if (currentRecognizedText.value!!.id == 0L) {
                                    Button(modifier = Modifier.weight(1f), onClick = {
                                        context.copyText(currentRecognizedText.value!!.content)
                                        isDialogVisible.value = false
                                    }) {
                                        Text(text = "Copy")
                                    }
                                } else {
                                    Button(modifier = Modifier.weight(1f), onClick = {
                                        viewModel.delete(currentRecognizedText.value!!)
                                        isDialogVisible.value = false
                                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT)
                                            .show()
                                    }) {
                                        Text(text = "Delete")
                                    }
                                }

                            }
                        }


                    }
                }
            }
        }

    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RecognizedTextItem(
        item: RecognizedText,
        onIconClick: (text: String) -> Unit,
        onClick: () -> Unit
    ) {
        OutlinedCard(
            onClick = {
                onClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    onIconClick(item.content)
                }) {
                    Icon(
                        painter = painterResource(
                            id = R.drawable.content_copy
                        ), contentDescription = "",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.content ?: "",
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,

                    )
            }
        }

    }


}