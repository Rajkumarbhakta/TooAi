package com.rkbapps.tooai.ui.screens.text_recognitation

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
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.RecognizedText
import com.rkbapps.tooai.ui.composabels.TopBar
import com.rkbapps.tooai.utils.copyText

@Composable
fun TextReorganizationScreen(
    backStack: SnapshotStateList<Any>,
    viewModel: RecognizedTextViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val recognizedTextList by viewModel.recognizedTextList.collectAsStateWithLifecycle()
    val recognitionState by viewModel.recognitionState.collectAsStateWithLifecycle()
    val pendingImage by viewModel.pendingImage.collectAsStateWithLifecycle()
    val selectedHistoryItem by viewModel.selectedHistoryItem.collectAsStateWithLifecycle()

    // A history row wins over a fresh scan — tapping history is always an explicit action.
    val displayedText: RecognizedText? = selectedHistoryItem ?: recognitionState.data
    val isFromHistory = selectedHistoryItem != null

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.onImagePicked(it) }
        }
    )

    LaunchedEffect(recognitionState.error) {
        recognitionState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = "Text Recognition",
                actions = {
                    FilledIconButton(
                        onClick = {
                            uriHandler.openUri("https://developers.google.com/ml-kit/vision/text-recognition/v2/languages")
                        }
                    ) {
                        Icon(painter = painterResource(R.drawable.info), contentDescription = "Supported languages")
                    }
                }
                ) {
                backStack.removeLastOrNull()
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
            if (pendingImage == null) {
                if (recognizedTextList.isEmpty()) {
                    LaunchedEffect(key1 = Unit) {
                        galleryLauncher.launch("image/*")
                    }
                    TextButton(onClick = {
                        galleryLauncher.launch("image/*")
                    }) {
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
                                count = recognizedTextList.size,
                                key = { key ->
                                    recognizedTextList[key].id
                                }
                            ) { position ->
                                RecognizedTextItem(
                                    item = recognizedTextList[position],
                                    onIconClick = { text ->
                                        context.copyText(text)
                                    }) {
                                    viewModel.onHistoryItemSelected(recognizedTextList[position])
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
                    model = pendingImage,
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
                        enabled = !recognitionState.isLoading,
                        onClick = {
                            viewModel.clearPendingImage()
                        }) {
                        Text(text = "Cancel")
                    }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        enabled = !recognitionState.isLoading,
                        onClick = {
                            pendingImage?.let { viewModel.recognize(context, it) }
                        }) {
                        Text(text = if (recognitionState.isLoading) "Reading…" else "Start")
                    }
                }


            }

            if (displayedText != null) {
                Dialog(onDismissRequest = { viewModel.dismissResult() }) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Recognized Text",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                        )
                        SelectionContainer(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = displayedText.content,
                                textAlign = TextAlign.Justify, modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                modifier = Modifier.weight(1f), onClick = { viewModel.dismissResult() }) {
                                Text(text = "Dismiss")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (!isFromHistory) {
                                Button(modifier = Modifier.weight(1f), onClick = {
                                    context.copyText(displayedText.content)
                                    viewModel.dismissResult()
                                }) {
                                    Text(text = "Copy")
                                }
                            } else {
                                OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                    viewModel.delete(displayedText)
                                    viewModel.dismissResult()
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

@Composable
fun RecognizedTextItem(
    item: RecognizedText,
    onIconClick: (text: String) -> Unit,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = { onClick() },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                text = item.content,
                modifier = Modifier.weight(1f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                )
        }
    }

}