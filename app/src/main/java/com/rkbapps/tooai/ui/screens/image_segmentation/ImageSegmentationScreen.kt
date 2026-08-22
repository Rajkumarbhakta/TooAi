package com.rkbapps.tooai.ui.screens.image_segmentation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rkbapps.tooai.BuildConfig
import com.rkbapps.tooai.R
import com.rkbapps.tooai.ui.composabels.TopBar
import com.rkbapps.tooai.utils.UiState
import com.rkbapps.tooai.utils.saveImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ImageSegmentationScreen(
    backStack: SnapshotStateList<Any>,
    viewModel: ImageSegmentationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val segState by viewModel.segmentationState.collectAsStateWithLifecycle()
    val imageSavingStatus by viewModel.imageSavingStatus.collectAsStateWithLifecycle()

    // The currently displayed source URI
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Which result bitmap the user has selected to save (foreground or a subject)
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // URI for camera capture temp file
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                imageUri = it
                viewModel.resetState()
                selectedBitmap = null
            }
        }
    )

    // Camera capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                cameraImageUri?.let {
                    imageUri = it
                    viewModel.resetState()
                    selectedBitmap = null
                }
            }
        }
    )

    // Auto-open gallery when first entering the screen
//    LaunchedEffect(Unit) {
//        galleryLauncher.launch("image/*")
//    }

    // When a new result arrives, default the selected bitmap to the foreground
    LaunchedEffect(segState.data) {
        segState.data?.let { result ->
            selectedBitmap = result.foregroundBitmap
        }
    }

    // Show error as Toast
    LaunchedEffect(segState.error) {
        withContext(Dispatchers.Main){
            segState.error?.let { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(imageSavingStatus.error,imageSavingStatus.data) {
        withContext(Dispatchers.Main){
            imageSavingStatus.error?.let {
                Toast.makeText(context, "Unable to save image.", Toast.LENGTH_SHORT).show()
            }
            imageSavingStatus.data?.let {success->
                val msg = if (success) "Saved to Pictures/TooAi" else "Failed to save image"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = "Image Segmentation") {
                backStack.removeLastOrNull()
            }
        }
    ) { paddingValues ->

        val modifier = if (imageUri!=null){
            Modifier.verticalScroll(rememberScrollState())
        }else{
            Modifier
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (imageUri == null) {
                // — Empty state —
                EmptyState(
                    onGallery = { galleryLauncher.launch("image/*") },
                    onCamera = {
                        val tmpFile = File.createTempFile("seg_", ".jpg", context.cacheDir)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${BuildConfig.APPLICATION_ID}.provider",
                            tmpFile
                        )
                        cameraImageUri = uri
                        cameraLauncher.launch(uri)
                    }
                )
            } else {
                // — Source image —
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { galleryLauncher.launch("image/*") }
                )

                // — Action buttons —
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { galleryLauncher.launch("image/*") }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.image_segmentation),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val tmpFile = File.createTempFile("seg_", ".jpg", context.cacheDir)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${BuildConfig.APPLICATION_ID}.provider",
                                tmpFile
                            )
                            cameraImageUri = uri
                            cameraLauncher.launch(uri)
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.photo_camera),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = { imageUri?.let { viewModel.segment(context, it) } },
                    enabled = !segState.isLoading
                ) {
                    if (segState.isLoading) {
                        Text("Processing…")
                    } else {
                        Text("Detect Subjects")
                    }
                }

                // — Results section —
                AnimatedVisibility(
                    visible = segState.data != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    segState.data?.let { result ->
                        ResultSection(
                            result = result,
                            imageSavingStatus = imageSavingStatus,
                            selectedBitmap = selectedBitmap,
                            onBitmapSelected = { selectedBitmap = it },
                            onSave = { bitmap ->
                                viewModel.saveToGallery(
                                    context=context,
                                    bitmap = bitmap
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(onGallery: () -> Unit, onCamera: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Select or capture an image to segment subjects",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onGallery) {
                Icon(
                    painter = painterResource(R.drawable.image_segmentation),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Choose from Gallery")
            }
            FilledTonalButton(onClick = onCamera) {
                Icon(
                    painter = painterResource(R.drawable.photo_camera),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Take Photo")
            }
        }
    }
}

@Composable
private fun ResultSection(
    result: SegmentationResult,
    imageSavingStatus: UiState<Boolean>,
    selectedBitmap: Bitmap?,
    onBitmapSelected: (Bitmap) -> Unit,
    onSave: (Bitmap) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Segmentation Result",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main selected result preview
        selectedBitmap?.let { bmp ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = bmp,
                    contentDescription = "Segmented result",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }

        // Subject thumbnails — only shown when > 1 subject detected
        // Each subject.bitmap is CROPPED to its bounding box (not full-image-sized)
        if (result.subjects.size > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${result.subjects.size} subjects detected — tap to preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All" thumbnail — the combined foreground composite
                result.foregroundBitmap?.let { fg ->
                    SubjectThumbnail(
                        bitmap = fg,
                        label = "All",
                        isSelected = selectedBitmap == fg,
                        onClick = { onBitmapSelected(fg) }
                    )
                }
                // Individual per-subject cropped thumbnails
                result.subjects.forEachIndexed { index, subject ->
                    SubjectThumbnail(
                        bitmap = subject.bitmap,
                        label = "Subject ${index + 1}",
                        isSelected = selectedBitmap == subject.bitmap,
                        onClick = { onBitmapSelected(subject.bitmap) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        selectedBitmap?.let { bmp ->
            Button(
                enabled = !imageSavingStatus.isLoading,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSave(bmp) }
            ) {
                Text(if (imageSavingStatus.isLoading) "Saving Image..." else  "Save to Gallery")
            }
        }
    }
}

@Composable
private fun SubjectThumbnail(
    bitmap: Bitmap,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = bitmap,
            contentDescription = label,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    border = BorderStroke(2.dp, borderColor),
                    shape = RoundedCornerShape(8.dp)
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}

