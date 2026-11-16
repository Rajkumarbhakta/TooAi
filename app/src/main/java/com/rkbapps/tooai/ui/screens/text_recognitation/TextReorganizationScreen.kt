package com.rkbapps.tooai.ui.screens.text_recognitation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rkbapps.tooai.BuildConfig
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.RecognizedText
import com.rkbapps.tooai.ui.composabels.LargeCollapsingTopBar
import com.rkbapps.tooai.ui.composabels.PillSearchField
import com.rkbapps.tooai.ui.theme.GroupedListDefaults
import com.rkbapps.tooai.ui.theme.SurfaceStatusBar
import com.rkbapps.tooai.ui.theme.ToolShapes
import com.rkbapps.tooai.ui.theme.pageContainerColor
import com.rkbapps.tooai.ui.theme.rowContainerColor
import com.rkbapps.tooai.ui.theme.toolPalette
import com.rkbapps.tooai.utils.copyText
import com.rkbapps.tooai.utils.dateGroupLabel
import com.rkbapps.tooai.utils.toDateTimeString
import java.io.File
import kotlin.math.min

private const val SUPPORTED_LANGUAGES_URL =
    "https://developers.google.com/ml-kit/vision/text-recognition/v2/languages"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TextReorganizationScreen(
    backStack: SnapshotStateList<Any>,
    viewModel: RecognizedTextViewModel = hiltViewModel()
) {
    SurfaceStatusBar()

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val recognizedTextList by viewModel.recognizedTextList.collectAsStateWithLifecycle()
    val recognitionState by viewModel.recognitionState.collectAsStateWithLifecycle()
    val pendingImage by viewModel.pendingImage.collectAsStateWithLifecycle()

    var selectedTextId by rememberSaveable { mutableStateOf<Long?>(null) }
    var textToDelete by remember { mutableStateOf<RecognizedText?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var cameraImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.recognize(context, it) } }
    )
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) cameraImageUri?.let { viewModel.recognize(context, it) }
        }
    )
    val chooseImage = {
        isMenuExpanded = false
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val takePhoto = {
        isMenuExpanded = false
        val tmpFile = File.createTempFile("ocr_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    LaunchedEffect(recognitionState.error) {
        recognitionState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.dismissResult()
        }
    }

    textToDelete?.let { text ->
        AlertDialog(
            onDismissRequest = { textToDelete = null },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete recognized text?") },
            text = { Text("It will be removed from your history.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(text)
                        if (recognitionState.data?.text?.id == text.id) viewModel.dismissResult()
                        if (selectedTextId == text.id) selectedTextId = null
                        textToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { textToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // A fresh scan opens straight into its result. History rows are looked up by id, so a
    // deleted row closes its page.
    val freshResult = recognitionState.data
    val selectedText = recognizedTextList.find { it.id == selectedTextId }
    if (recognitionState.isLoading || freshResult != null || selectedText != null) {
        RecognizedTextDetail(
            text = freshResult?.text ?: selectedText,
            result = freshResult,
            pendingImage = pendingImage,
            onClose = {
                if (freshResult != null || recognitionState.isLoading) viewModel.dismissResult()
                selectedTextId = null
            },
            onDelete = { (freshResult?.text ?: selectedText)?.let { textToDelete = it } }
        )
        return
    }

    val filteredTexts = remember(recognizedTextList, searchQuery) {
        if (searchQuery.isBlank()) recognizedTextList
        else recognizedTextList.filter { it.content.contains(searchQuery, ignoreCase = true) }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(enabled = isMenuExpanded) { isMenuExpanded = false }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeCollapsingTopBar(
                    title = "Text Recognition",
                    scrollBehavior = scrollBehavior,
                    onNavigationIconClick = { backStack.removeLastOrNull() },
                    subtitle = if (recognizedTextList.isEmpty()) null else {
                        {
                            Text(
                                if (recognizedTextList.size == 1) "1 saved scan"
                                else "${recognizedTextList.size} saved scans"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { uriHandler.openUri(SUPPORTED_LANGUAGES_URL) }) {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = "Supported languages"
                            )
                        }
                    }
                )
            },
            containerColor = pageContainerColor()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (recognizedTextList.isEmpty()) {
                    TextEmptyState(onChooseImage = chooseImage)
                    return@Column
                }
                PillSearchField(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    query = searchQuery,
                    placeholder = "Search recognized text",
                    onQueryChange = { searchQuery = it }
                )
                if (filteredTexts.isEmpty()) {
                    Text(
                        text = "No matching text",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.Gap)
                    ) {
                        item { Spacer(modifier = Modifier.height(5.dp)) }
                        itemsIndexed(filteredTexts, key = { _, text -> text.id }) { index, text ->
                            RecognizedTextItem(
                                shape = GroupedListDefaults.shape(index, filteredTexts.size),
                                item = text,
                                onClick = { selectedTextId = text.id }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(112.dp)) }
                    }
                }
            }
        }

        AnimatedVisibility(visible = isMenuExpanded, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isMenuExpanded = false }
            )
        }

        ScanMenu(
            expanded = isMenuExpanded,
            onExpandedChange = { isMenuExpanded = it },
            onTakePhoto = takePhoto,
            onChooseImage = chooseImage,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScanMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTakePhoto: () -> Unit,
    onChooseImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.toolPalette.text
    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = onExpandedChange,
                containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                    initialColor = accent.container,
                    finalColor = accent.action
                )
            ) {
                val iconColor = ToggleFloatingActionButtonDefaults.iconColor(
                    initialColor = accent.onContainer,
                    finalColor = accent.onAction
                )
                Icon(
                    painter = painterResource(if (checkedProgress > 0.5f) R.drawable.close else R.drawable.add),
                    contentDescription = if (expanded) "Close menu" else "Recognize text",
                    tint = iconColor(checkedProgress)
                )
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = onTakePhoto,
            text = { Text("Take photo") },
            icon = { Icon(painter = painterResource(R.drawable.photo_camera), contentDescription = null) },
            containerColor = accent.container,
            contentColor = accent.onContainer
        )
        FloatingActionButtonMenuItem(
            onClick = onChooseImage,
            text = { Text("Choose image") },
            icon = { Icon(painter = painterResource(R.drawable.image), contentDescription = null) },
            containerColor = accent.container,
            contentColor = accent.onContainer
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TextEmptyState(onChooseImage: () -> Unit) {
    val accent = MaterialTheme.toolPalette.text
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(accent.container, ToolShapes.Text.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.text_recognation),
                contentDescription = null,
                tint = accent.onContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No recognized text yet",
            style = MaterialTheme.typography.titleMediumEmphasized
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Photograph a sign, a page or a receipt to copy its text. It's read on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onChooseImage,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent.action,
                contentColor = accent.onAction
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.image),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text("Choose image")
        }
    }
}

private fun RecognizedText.wordCount() = content.split(Regex("\\s+")).count { it.isNotBlank() }

private fun RecognizedText.dateLabel(): String? {
    if (timeMillis <= 0) return null
    return when (dateGroupLabel(timeMillis)) {
        "Today" -> "Today, " + timeMillis.toDateTimeString(pattern = "h:mm a")
        "Yesterday" -> "Yesterday"
        else -> timeMillis.toDateTimeString(pattern = "d MMM")
    }
}

private fun RecognizedText.summary(): String {
    val words = wordCount()
    return listOfNotNull(if (words == 1) "1 word" else "$words words", dateLabel()).joinToString(" · ")
}

@Composable
private fun RecognizedTextItem(shape: Shape, item: RecognizedText, onClick: () -> Unit) {
    // A page glyph tinted by a fixed accent per row, so neighbouring rows are told apart.
    val palette = MaterialTheme.toolPalette
    val accents = listOf(palette.text, palette.docs, palette.aiChat, palette.qr)
    val accent = accents[(item.id % accents.size).toInt()]

    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = shape, color = rowContainerColor()) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .size(width = 56.dp, height = 72.dp)
                    .background(accent.container, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                listOf(1f, 1f, 1f, 0.6f).forEachIndexed { index, fraction ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(if (index == 0) 4.dp else 3.dp)
                            .background(
                                accent.onContainer.copy(alpha = if (index == 0) 0.8f else 0.35f),
                                CircleShape
                            )
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.content.replace(Regex("\\s+"), " ").trim(),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.summary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecognizedTextDetail(
    text: RecognizedText?,
    result: RecognitionResult?,
    pendingImage: Uri?,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val accent = MaterialTheme.toolPalette.text
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(onBack = onClose)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeCollapsingTopBar(
                title = if (text == null) "Reading text" else "Recognized text",
                scrollBehavior = scrollBehavior,
                onNavigationIconClick = onClose,
                actions = {
                    if (text != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (text != null) {
                TextActions(
                    onCopy = { context.copyText(text.content) },
                    onShare = { shareText(context, text.content) }
                )
            }
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
            // Only a fresh scan has its photo. History keeps just the text.
            val image = result?.imageUri ?: pendingImage.takeIf { text == null }
            if (image != null) {
                ScannedImage(image = image, result = result)
                Spacer(modifier = Modifier.height(14.dp))
            }
            if (text == null) return@Column

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val words = text.wordCount()
                InfoChip(
                    label = if (words == 1) "1 word" else "$words words",
                    containerColor = accent.container,
                    contentColor = accent.onContainer
                )
                text.dateLabel()?.let {
                    InfoChip(
                        label = it,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = rowContainerColor()
            ) {
                SelectionContainer {
                    Text(
                        text = text.content,
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScannedImage(image: Uri, result: RecognitionResult?) {
    val highlight = MaterialTheme.toolPalette.text.action
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.inverseSurface),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = image,
            contentDescription = "Scanned image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            alpha = if (result == null) 0.5f else 1f
        )
        if (result == null) {
            LoadingIndicator(modifier = Modifier.size(72.dp))
            return@BoxWithConstraints
        }
        // Same fit as the image above, so each box lands on its block.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = min(size.width / result.imageWidth, size.height / result.imageHeight)
            val left = (size.width - result.imageWidth * scale) / 2
            val top = (size.height - result.imageHeight * scale) / 2
            val corner = CornerRadius(3.dp.toPx())
            result.blocks.forEach { block ->
                val topLeft = Offset(left + block.left * scale, top + block.top * scale)
                val blockSize = Size(block.width() * scale, block.height() * scale)
                drawRoundRect(highlight.copy(alpha = 0.22f), topLeft, blockSize, corner)
                drawRoundRect(highlight, topLeft, blockSize, corner, style = Stroke(1.5.dp.toPx()))
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            shape = CircleShape,
            color = Color(0xBF1C1A22),
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(painter = painterResource(R.drawable.check), contentDescription = null, modifier = Modifier.size(14.dp))
                Text(
                    text = if (result.blocks.size == 1) "1 block found" else "${result.blocks.size} blocks found",
                    style = MaterialTheme.typography.labelMediumEmphasized
                )
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, containerColor: Color, contentColor: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = containerColor, contentColor = contentColor) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLargeEmphasized
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TextActions(onCopy: () -> Unit, onShare: () -> Unit) {
    val accent = MaterialTheme.toolPalette.text
    val height = ButtonDefaults.MediumContainerHeight
    Row(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onCopy,
            modifier = Modifier
                .weight(1f)
                .height(height),
            shapes = ButtonDefaults.shapesFor(height),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent.action,
                contentColor = accent.onAction
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.content_copy),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(height))
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(height)))
            Text(text = "Copy", style = MaterialTheme.typography.titleMediumEmphasized)
        }
        OutlinedIconButton(
            onClick = onShare,
            modifier = Modifier.size(height),
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(painter = painterResource(R.drawable.share), contentDescription = "Share")
        }
    }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share text"))
}
