package com.rkbapps.tooai.ui.screens.image_segmentation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rkbapps.tooai.BuildConfig
import com.rkbapps.tooai.R
import com.rkbapps.tooai.navigation.NavigationEntry
import com.rkbapps.tooai.ui.composabels.LargeCollapsingTopBar
import com.rkbapps.tooai.ui.composabels.checkerboard
import com.rkbapps.tooai.ui.composabels.drawCheckerboard
import com.rkbapps.tooai.ui.theme.LightToolPalette
import com.rkbapps.tooai.ui.theme.SurfaceStatusBar
import com.rkbapps.tooai.ui.theme.ToolShapes
import com.rkbapps.tooai.ui.theme.pageContainerColor
import com.rkbapps.tooai.ui.theme.toolPalette
import java.io.File
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

// Fixed colors, so a saved cut-out doesn't depend on the app's theme. Null is transparent.
private data class BackgroundOption(val label: String, val color: Color?)

private val backgroundOptions = listOf(
    BackgroundOption("Transparent", null),
    BackgroundOption("White", Color.White),
    BackgroundOption("Pink", LightToolPalette.background.container),
    BackgroundOption("Blue", LightToolPalette.text.container),
)

@Composable
fun ImageSegmentationScreen(
    backStack: SnapshotStateList<Any>,
    viewModel: ImageSegmentationViewModel = hiltViewModel()
) {
    SurfaceStatusBar()

    val context = LocalContext.current
    val segState by viewModel.segmentationState.collectAsStateWithLifecycle()
    val imageSavingStatus by viewModel.imageSavingStatus.collectAsStateWithLifecycle()
    val recentCutouts by viewModel.recentCutouts.collectAsStateWithLifecycle()

    // Shown while the photo is processed. The result page draws the decoded bitmap instead.
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    // URI for camera capture temp file
    var cameraImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val onImagePicked = { uri: Uri ->
        imageUri = uri
        viewModel.segment(context, uri)
    }

    // The system photo picker needs no storage permission.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onImagePicked) }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) cameraImageUri?.let(onImagePicked)
        }
    )

    val openGallery = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val openCamera = {
        val tmpFile = File.createTempFile("seg_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    LaunchedEffect(segState.error) {
        segState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    LaunchedEffect(imageSavingStatus.error, imageSavingStatus.data) {
        imageSavingStatus.error?.let {
            Toast.makeText(context, "Unable to save image.", Toast.LENGTH_SHORT).show()
        }
        imageSavingStatus.data?.let { success ->
            val msg = if (success) "Saved to Pictures/TooAi" else "Failed to save image"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val result = segState.data
    if (segState.isLoading || result != null) {
        CutoutResult(
            result = result,
            pendingImage = imageUri,
            isSaving = imageSavingStatus.isLoading,
            onClose = viewModel::resetState,
            onSave = { bitmap, background -> viewModel.saveToGallery(context, bitmap, background) },
            onShare = { bitmap, background -> viewModel.share(context, bitmap, background) },
            onMakeSticker = { cutout ->
                viewModel.prepareSticker(context, cutout) { path ->
                    backStack.add(NavigationEntry.StickerEditor(path))
                }
            }
        )
        return
    }

    // The sticker editor saves under Pictures/TooAi too, so the strip is reloaded on the way back.
    LaunchedEffect(Unit) { viewModel.refreshRecentCutouts() }

    PickImage(
        recentCutouts = recentCutouts,
        onBack = { backStack.removeLastOrNull() },
        onGallery = openGallery,
        onCamera = openCamera
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PickImage(
    recentCutouts: List<Uri>,
    onBack: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit
) {
    val context = LocalContext.current
    val accent = MaterialTheme.toolPalette.background
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeCollapsingTopBar(
                title = "Background Remover",
                scrollBehavior = scrollBehavior,
                onNavigationIconClick = onBack
            )
        },
        containerColor = pageContainerColor()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(200.dp)
                    .background(accent.container, ToolShapes.Background.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.image_segmentation),
                    contentDescription = null,
                    tint = accent.action,
                    modifier = Modifier.size(96.dp)
                )
            }
            Text(
                text = "Pick a photo with a clear subject. It is processed on this device and never uploaded.",
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 20.dp)
                    .widthIn(max = 320.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SourceTile(
                    title = "Gallery",
                    subtitle = "Choose a photo",
                    icon = R.drawable.image,
                    containerColor = accent.container,
                    contentColor = accent.onContainer,
                    iconContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    iconShape = RoundedCornerShape(18.dp),
                    onClick = onGallery,
                    modifier = Modifier.weight(1f)
                )
                SourceTile(
                    title = "Camera",
                    subtitle = "Take a new photo",
                    icon = R.drawable.photo_camera,
                    containerColor = accent.action,
                    contentColor = accent.onAction,
                    iconContainerColor = accent.onAction.copy(alpha = 0.16f),
                    iconShape = CircleShape,
                    onClick = onCamera,
                    modifier = Modifier.weight(1f)
                )
            }

            if (recentCutouts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent cut-outs",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                    Text(
                        text = "Saved to Pictures/TooAi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentCutouts, key = { it.toString() }) { uri ->
                        Surface(
                            onClick = { openImage(context, uri) },
                            modifier = Modifier.size(112.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Saved cut-out",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .checkerboard(8.dp)
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceTile(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    containerColor: Color,
    contentColor: Color,
    iconContainerColor: Color,
    iconShape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(32.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(iconContainerColor, iconShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLargeEmphasized)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CutoutResult(
    result: SegmentationResult?,
    pendingImage: Uri?,
    isSaving: Boolean,
    onClose: () -> Unit,
    onSave: (Bitmap, Color?) -> Unit,
    onShare: (Bitmap, Color?) -> Unit,
    onMakeSticker: (Bitmap) -> Unit
) {
    // -1 is every subject together.
    var subjectIndex by rememberSaveable(result) { mutableIntStateOf(-1) }
    var backgroundIndex by rememberSaveable { mutableIntStateOf(0) }
    val background = backgroundOptions[backgroundIndex].color
    val subject = result?.subjects?.getOrNull(subjectIndex)
    val cutout = subject?.bitmap ?: result?.foregroundBitmap
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val accent = MaterialTheme.toolPalette.background

    BackHandler(onBack = onClose)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeCollapsingTopBar(
                title = if (result == null) "Removing background" else "Background removed",
                scrollBehavior = scrollBehavior,
                onNavigationIconClick = onClose,
                subtitle = {
                    Text(
                        if (result == null) {
                            "Working on this device…"
                        } else {
                            String.format(
                                Locale.getDefault(),
                                "Done in %.1f s · %d × %d",
                                result.durationMillis / 1000f,
                                result.source.width,
                                result.source.height
                            )
                        }
                    )
                }
            )
        },
        bottomBar = {
            CutoutActions(
                enabled = cutout != null && !isSaving,
                isSaving = isSaving,
                onTryAnother = onClose,
                onShare = { cutout?.let { onShare(it, background) } },
                onSave = { cutout?.let { onSave(it, background) } }
            )
        },
        containerColor = pageContainerColor()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            if (result == null) {
                ProcessingPreview(pendingImage)
            } else {
                CompareView(
                    source = result.source,
                    cutout = cutout ?: result.foregroundBitmap,
                    cutoutOffset = subject?.let { IntOffset(it.startX, it.startY) } ?: IntOffset.Zero,
                    background = background
                )

                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Background",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmallEmphasized
                    )
                    backgroundOptions.forEachIndexed { index, option ->
                        BackgroundSwatch(
                            option = option,
                            selected = index == backgroundIndex,
                            onClick = { backgroundIndex = index }
                        )
                    }
                }

                // The segmenter finds each subject separately, so one can be saved on its own.
                if (result.subjects.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubjectChip("All subjects", subjectIndex == -1) { subjectIndex = -1 }
                        result.subjects.indices.forEach { index ->
                            SubjectChip("Subject ${index + 1}", subjectIndex == index) { subjectIndex = index }
                        }
                    }
                }

                // Opens the sticker editor with whichever cut-out is showing.
                Surface(
                    onClick = { cutout?.let(onMakeSticker) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = accent.container,
                    contentColor = accent.onContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sticker),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Make sticker", style = MaterialTheme.typography.titleMediumEmphasized)
                            Text(
                                text = "Add text, emoji, drawings and an outline",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(painter = painterResource(R.drawable.arrow_forward), contentDescription = null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProcessingPreview(pendingImage: Uri?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = pendingImage,
            contentDescription = "Selected photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            alpha = 0.5f
        )
        LoadingIndicator(modifier = Modifier.size(96.dp))
    }
}

// Before on the left of the divider, the cut-out on the chosen background on the right. Both
// halves are drawn on one canvas with the same fit, so they always line up.
@Composable
private fun CompareView(
    source: Bitmap,
    cutout: Bitmap,
    cutoutOffset: IntOffset,
    background: Color?
) {
    val sourceImage = remember(source) { source.asImageBitmap() }
    val cutoutImage = remember(cutout) { cutout.asImageBitmap() }
    var split by rememberSaveable { mutableFloatStateOf(0.5f) }
    val checkerLight = MaterialTheme.colorScheme.surfaceContainerLowest
    val checkerDark = MaterialTheme.colorScheme.surfaceContainerHighest

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio((source.width.toFloat() / source.height).coerceIn(0.6f, 1.8f))
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    split = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { split = (it.x / size.width).coerceIn(0f, 1f) }
            }
    ) {
        val handleSize = 48.dp
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = min(size.width / source.width, size.height / source.height)
            val imageSize = Size(source.width * scale, source.height * scale)
            val imageTopLeft = Offset((size.width - imageSize.width) / 2, (size.height - imageSize.height) / 2)
            val splitX = size.width * split

            clipRect(right = splitX) {
                drawImage(
                    image = sourceImage,
                    dstOffset = imageTopLeft.round(),
                    dstSize = imageSize.round(),
                    filterQuality = FilterQuality.Medium
                )
            }
            clipRect(left = splitX) {
                if (background == null) {
                    drawCheckerboard(imageTopLeft, imageSize, 10.dp.toPx(), checkerLight, checkerDark)
                } else {
                    drawRect(background, topLeft = imageTopLeft, size = imageSize)
                }
                drawImage(
                    image = cutoutImage,
                    dstOffset = Offset(
                        imageTopLeft.x + cutoutOffset.x * scale,
                        imageTopLeft.y + cutoutOffset.y * scale
                    ).round(),
                    dstSize = Size(cutout.width * scale, cutout.height * scale).round(),
                    filterQuality = FilterQuality.Medium
                )
            }
            val lineWidth = 4.dp.toPx()
            drawRect(Color.White, topLeft = Offset(splitX - lineWidth / 2, 0f), size = Size(lineWidth, size.height))
        }

        Surface(
            modifier = Modifier
                .offset(x = maxWidth * split - handleSize / 2, y = (maxHeight - handleSize) / 2)
                .size(handleSize),
            shape = CircleShape,
            color = Color.White,
            contentColor = Color(0xFF1C1A22),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(R.drawable.compare_handle), contentDescription = "Drag to compare")
            }
        }
        CompareLabel(
            text = "Before",
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        )
        CompareLabel(
            text = "After",
            containerColor = MaterialTheme.toolPalette.background.action,
            contentColor = MaterialTheme.toolPalette.background.onAction,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
        )
    }
}

@Composable
private fun CompareLabel(text: String, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = containerColor, contentColor = contentColor) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMediumEmphasized
        )
    }
}

private fun Offset.round() = IntOffset(x.roundToInt(), y.roundToInt())

private fun Size.round() = IntSize(width.roundToInt(), height.roundToInt())

@Composable
private fun BackgroundSwatch(option: BackgroundOption, selected: Boolean, onClick: () -> Unit) {
    val shape = if (option.color == null) RoundedCornerShape(14.dp) else CircleShape
    val ring = MaterialTheme.toolPalette.background.action
    Box(
        modifier = Modifier
            .size(48.dp)
            .then(if (selected) Modifier.border(2.dp, ring, shape) else Modifier)
            .padding(4.dp)
            .clip(shape)
            .then(
                if (option.color == null) Modifier.checkerboard(5.dp)
                else Modifier.background(option.color)
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = "${option.label} background" }
    )
}

@Composable
private fun SubjectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.toolPalette.background
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent.container,
            selectedLabelColor = accent.onContainer
        )
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CutoutActions(
    enabled: Boolean,
    isSaving: Boolean,
    onTryAnother: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    val accent = MaterialTheme.toolPalette.background
    Row(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onTryAnother) {
                    Icon(painter = painterResource(R.drawable.refresh), contentDescription = "Try another image")
                }
                IconButton(onClick = onShare, enabled = enabled) {
                    Icon(painter = painterResource(R.drawable.share), contentDescription = "Share")
                }
            }
        }
        Button(
            onClick = onSave,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent.action,
                contentColor = accent.onAction
            )
        ) {
            if (isSaving) {
                LoadingIndicator(modifier = Modifier.size(28.dp), color = accent.onAction)
            } else {
                Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Save PNG", style = MaterialTheme.typography.titleMediumEmphasized)
            }
        }
    }
}

private fun openImage(context: Context, uri: Uri) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    } catch (e: Exception) {
        Toast.makeText(context, "No app can open this image", Toast.LENGTH_SHORT).show()
    }
}
