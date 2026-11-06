package com.rkbapps.tooai.ui.screens.doc_scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.rkbapps.tooai.BuildConfig
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.DocumentScans
import com.rkbapps.tooai.ui.composabels.LargeCollapsingTopBar
import com.rkbapps.tooai.ui.composabels.PillSearchField
import com.rkbapps.tooai.ui.theme.GroupedListDefaults
import com.rkbapps.tooai.ui.theme.SurfaceStatusBar
import com.rkbapps.tooai.ui.theme.TooAiTheme
import com.rkbapps.tooai.ui.theme.ToolShapes
import com.rkbapps.tooai.ui.theme.pageContainerColor
import com.rkbapps.tooai.ui.theme.rowContainerColor
import com.rkbapps.tooai.ui.theme.toolPalette
import com.rkbapps.tooai.utils.dateGroupLabel
import com.rkbapps.tooai.utils.formatFileSize
import com.rkbapps.tooai.utils.getActivity
import com.rkbapps.tooai.utils.toDateTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val THUMBNAIL_WIDTH_PX = 156

private data class DocumentInfo(
    val pageCount: Int,
    val sizeBytes: Long,
    val thumbnail: ImageBitmap
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DocScannerScreen(
    backStack: SnapshotStateList<Any>,
    viewModel: DocumentScannerViewModel = hiltViewModel()
) {
    SurfaceStatusBar()

    val context = LocalContext.current
    val activity = context.getActivity() as Activity

    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val docSavingState by viewModel.docSavingState.collectAsStateWithLifecycle()

    var documentToDelete by remember { mutableStateOf<DocumentScans?>(null) }
    var result by rememberSaveable {
        mutableStateOf<GmsDocumentScanningResult?>(null)
    }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredDocuments = remember(documents, searchQuery) {
        val newestFirst = documents.sortedByDescending { it.timeMillis }
        if (searchQuery.isBlank()) newestFirst
        else newestFirst.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        handelScanActivityResult(activityResult, context) {
            Log.d("Result", it.toString())
            result = it
        }
    }

    val startScan = {
        viewModel.startScan(
            activity = activity,
            onScanResult = { intentSenderRequest ->
                scannerLauncher.launch(intentSenderRequest)
            },
            onScanError = { error ->
                Toast.makeText(context, error.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    LaunchedEffect(docSavingState.error) {
        if (docSavingState.error != null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, docSavingState.error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(docSavingState.data) {
        if (docSavingState.data != null) {
            result = null
        }
    }

    result?.let { scan ->
        ScanPreview(
            scan = scan,
            isSaving = docSavingState.isLoading,
            onClose = { result = null },
            onSavePdf = { uri -> viewModel.saveDocument(context = context, uri = uri) },
            onSaveImages = { uris ->
                viewModel.savePagesAsImages(context, uris) { saved ->
                    val pages = if (saved == 1) "1 page" else "$saved pages"
                    Toast.makeText(context, "$pages saved to Pictures", Toast.LENGTH_SHORT).show()
                    result = null
                }
            }
        )
        return
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val accent = MaterialTheme.toolPalette.docs

    documentToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Document?") },
            text = { Text("Are you sure you want to delete \"${doc.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(context, doc)
                        documentToDelete = null
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
                OutlinedButton(onClick = { documentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeCollapsingTopBar(
                title = "Documents",
                scrollBehavior = scrollBehavior,
                onNavigationIconClick = { backStack.removeLastOrNull() },
                subtitle = {
                    Text(if (documents.size == 1) "1 scan" else "${documents.size} scans")
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearching = !isSearching
                            searchQuery = ""
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = "Search documents"
                        )
                    }
                }
            )
        },
        containerColor = pageContainerColor(),
        floatingActionButton = {
            MediumExtendedFloatingActionButton(
                text = {
                    Text(
                        text = "Scan",
                        style = MaterialTheme.typography.titleLargeEmphasized
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.photo_camera),
                        contentDescription = null
                    )
                },
                onClick = startScan,
                containerColor = accent.container,
                contentColor = accent.onContainer
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(visible = isSearching) {
                PillSearchField(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    query = searchQuery,
                    placeholder = "Search documents",
                    focusOnShow = true,
                    onQueryChange = { searchQuery = it }
                )
            }

            if (filteredDocuments.isEmpty()) {
                DocumentsEmptyState(isSearching = searchQuery.isNotBlank(), onScan = startScan)
            } else {
                val groups = remember(filteredDocuments) {
                    filteredDocuments.groupBy { dateGroupLabel(it.timeMillis) }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.Gap)
                ) {
                    groups.forEach { (group, groupDocuments) ->
                        item(key = group) {
                            Text(
                                text = group,
                                modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.titleSmallEmphasized,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        itemsIndexed(groupDocuments, key = { _, doc -> doc.id }) { index, doc ->
                            DocumentItem(
                                shape = GroupedListDefaults.shape(index, groupDocuments.size),
                                documentScans = doc,
                                onDelete = { documentToDelete = doc },
                                onShare = { sharePdf(context, doc.path) },
                                onClick = {
                                    try {
                                        Log.d("Opening URI", doc.path)
                                        openPdf(context, doc.path)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                                        Log.e("Opening URI", e.localizedMessage ?: "", e)
                                    }
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(112.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DocumentsEmptyState(isSearching: Boolean, onScan: () -> Unit) {
    val accent = MaterialTheme.toolPalette.docs
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
                .background(accent.container, ToolShapes.Docs.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.document_scanner),
                contentDescription = null,
                tint = accent.onContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearching) "No matching documents" else "No scanned documents",
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isSearching) "Try a different name."
            else "Scan receipts, documents, or notes to save them as high quality PDFs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!isSearching) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onScan,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.action,
                    contentColor = accent.onAction
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.photo_camera),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Scan document")
            }
        }
    }
}

@Composable
private fun rememberDocumentInfo(path: String): DocumentInfo? {
    val context = LocalContext.current
    val info by produceState<DocumentInfo?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { loadDocumentInfo(context, path) }
    }
    return info
}

// Returns null when the PDF was removed outside the app or can't be read.
private fun loadDocumentInfo(context: Context, path: String): DocumentInfo? = try {
    context.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use { descriptor ->
        val sizeBytes = descriptor.statSize
        PdfRenderer(descriptor).use { renderer ->
            renderer.openPage(0).use { page ->
                val height = THUMBNAIL_WIDTH_PX * page.height / page.width
                val bitmap = Bitmap.createBitmap(THUMBNAIL_WIDTH_PX, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(AndroidColor.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                DocumentInfo(renderer.pageCount, sizeBytes, bitmap.asImageBitmap())
            }
        }
    }
} catch (e: Exception) {
    Log.e("Document info", e.localizedMessage ?: "", e)
    null
}

@Composable
private fun DocumentThumbnail(thumbnail: ImageBitmap?) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 68.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.pdf),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DocumentItem(
    shape: Shape,
    documentScans: DocumentScans,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onClick: () -> Unit
) {
    val info = rememberDocumentInfo(documentScans.path)
    val details = listOfNotNull(
        info?.pageCount?.let { if (it == 1) "1 page" else "$it pages" },
        "PDF",
        info?.sizeBytes?.let { formatFileSize(it) }
    ).joinToString(" · ")
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    var isMenuOpen by remember { mutableStateOf(false) }

    // Deleting asks for confirmation first, so the row always springs back.
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.clip(shape),
        enableDismissFromStartToEnd = false,
        onDismiss = {
            onDelete()
            scope.launch { dismissState.reset() }
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color.Transparent
                        }
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError
                    )
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            color = rowContainerColor()
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DocumentThumbnail(thumbnail = info?.thumbnail)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = documentScans.title,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis
                    )
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = documentScans.timeMillis.toDateTimeString(pattern = "dd MMM yyyy, hh:mm a"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onShare) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = "Share PDF",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { isMenuOpen = true }) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                isMenuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScanPreview(
    scan: GmsDocumentScanningResult,
    isSaving: Boolean,
    onClose: () -> Unit,
    onSavePdf: (Uri) -> Unit,
    onSaveImages: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val pages = remember(scan) {
        scan.pages.orEmpty().mapNotNull { page ->
            page.imageUri.path?.let { getExternalUri(context, it) }
        }
    }
    val pdfUri = remember(scan) { scan.pdf?.uri?.path?.let { getExternalUri(context, it) } }
    val carouselState = rememberCarouselState { pages.size }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(enabled = !isSaving, onBack = onClose)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeCollapsingTopBar(
                title = "Scan preview",
                scrollBehavior = scrollBehavior,
                onNavigationIconClick = { if (!isSaving) onClose() },
                navigationIcon = R.drawable.close,
                navigationContentDescription = "Close",
                subtitle = {
                    Text(if (pages.size == 1) "1 page · just scanned" else "${pages.size} pages · just scanned")
                }
            )
        },
        bottomBar = {
            ScanPreviewActions(
                isSaving = isSaving,
                onShare = pdfUri?.let { uri -> { sharePdf(context, uri.toString()) } },
                onSavePdf = pdfUri?.let { uri -> { onSavePdf(uri) } },
                onSaveImages = { onSaveImages(pages) }
            )
        },
        containerColor = pageContainerColor()
    ) { paddingValues ->
        val pagesModifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(top = 8.dp, bottom = 16.dp)

        if (pages.size == 1) {
            ScanPage(
                uri = pages.first(),
                index = 0,
                pageCount = 1,
                modifier = pagesModifier
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
            )
        } else {
            HorizontalMultiBrowseCarousel(
                state = carouselState,
                preferredItemWidth = 262.dp,
                modifier = pagesModifier,
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) { index ->
                ScanPage(
                    uri = pages[index],
                    index = index,
                    pageCount = pages.size,
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScanPage(uri: Uri, index: Int, pageCount: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        AsyncImage(
            model = uri,
            contentDescription = "Scanned page ${index + 1}",
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentScale = ContentScale.Fit
        )
        Surface(
            modifier = Modifier.padding(14.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.inverseSurface
        ) {
            Text(
                text = "${index + 1} / $pageCount",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMediumEmphasized
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScanPreviewActions(
    isSaving: Boolean,
    onShare: (() -> Unit)?,
    onSavePdf: (() -> Unit)?,
    onSaveImages: () -> Unit
) {
    val accent = MaterialTheme.toolPalette.docs
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = accent.action,
        contentColor = accent.onAction
    )
    var isMenuOpen by remember { mutableStateOf(false) }

    Surface(color = pageContainerColor()) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            onShare?.let {
                FilledTonalIconButton(
                    onClick = it,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                ) {
                    Icon(painter = painterResource(R.drawable.share), "Share")
                }
            }
            // SplitButtonLayout sizes to its content, and this one has to fill the row.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(SplitButtonDefaults.Spacing)
            ) {
                SplitButtonDefaults.LeadingButton(
                    onClick = { onSavePdf?.invoke() },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    enabled = !isSaving && onSavePdf != null,
                    colors = buttonColors
                ) {
                    if (isSaving) {
                        LoadingIndicator(modifier = Modifier.size(24.dp), color = accent.onAction)
                    } else {
                        Text(
                            text = "Save PDF",
                            style = MaterialTheme.typography.titleMediumEmphasized
                        )
                    }
                }
                Box {
                    SplitButtonDefaults.TrailingButton(
                        checked = isMenuOpen,
                        onCheckedChange = { isMenuOpen = it },
                        modifier = Modifier
                            .width(56.dp)
                            .fillMaxHeight(),
                        enabled = !isSaving,
                        colors = buttonColors
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_drop_down),
                            contentDescription = "More save options"
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        DropdownMenuItem(
                            text = { Text("Save as images") },
                            onClick = {
                                isMenuOpen = false
                                onSaveImages()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun handelScanActivityResult(
    activityResult: ActivityResult,
    context: Context,
    onResult: (GmsDocumentScanningResult?) -> Unit
) {
    try {
        val resultCode = activityResult.resultCode
        val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        when (resultCode) {
            Activity.RESULT_OK if result != null -> {
                onResult(result)
            }
            Activity.RESULT_CANCELED -> {
                // Canceled by user.
            }
            else -> {
                Toast.makeText(context, "Failed to scan.", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Log.e("Scan Error", e.localizedMessage ?: "", e)
        Toast.makeText(context, "Failed to scan.", Toast.LENGTH_SHORT).show()
    }
}

fun openPdf(context: Context, path: String) {
    val uri = Uri.parse(path)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}

fun sharePdf(context: Context, path: String) {
    try {
        val uri = Uri.parse(path)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share document: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun getExternalUri(context: Context, path: String): Uri? {
    return FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.provider",
        File(path)
    )
}

@Preview
@Composable
fun DocumentItemPreview() {
    TooAiTheme {
        DocumentItem(
            shape = GroupedListDefaults.shape(0, 1),
            documentScans = DocumentScans(
                id = 1,
                path = "",
                title = "Scanned Receipt - Invoice #1024.pdf",
                timeMillis = System.currentTimeMillis()
            ),
            onDelete = {},
            onShare = {},
            onClick = {}
        )
    }
}
