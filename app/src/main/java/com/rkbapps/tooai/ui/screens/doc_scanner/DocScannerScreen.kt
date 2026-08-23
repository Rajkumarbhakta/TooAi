package com.rkbapps.tooai.ui.screens.doc_scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.rkbapps.tooai.ui.composabels.TopBar
import com.rkbapps.tooai.ui.theme.TooAiTheme
import com.rkbapps.tooai.utils.getActivity
import com.rkbapps.tooai.utils.toDateTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DocScannerScreen(
    backStack: SnapshotStateList<Any>,
    viewModel: DocumentScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context.getActivity() as Activity

    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val docSavingState by viewModel.docSavingState.collectAsStateWithLifecycle()

    var documentToDelete by remember { mutableStateOf<DocumentScans?>(null) }
    var result by rememberSaveable {
        mutableStateOf<GmsDocumentScanningResult?>(null)
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        handelScanActivityResult(activityResult, context) {
            Log.d("Result", it.toString())
            result = it
        }
    }

    LaunchedEffect(docSavingState.error) {
        if (docSavingState.error != null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, docSavingState.error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = "Document Scanner") {
                backStack.removeLastOrNull()
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = result == null) {
                FloatingActionButton(
                    onClick = {
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
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.photo_camera),
                            contentDescription = "Scan Document"
                        )
                        Text("Scan")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            when {
                result != null -> {
                    LaunchedEffect(docSavingState.data) {
                        if (docSavingState.data != null) {
                            result = null
                        }
                    }

                    Text(
                        text = "Final Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val pages = result?.pages ?: emptyList()
                    if (pages.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(count = pages.size) { position ->
                                val uri = pages[position].imageUri
                                uri.path?.let { path ->
                                    val externalUri = getExternalUri(context, path)
                                    if (externalUri != null) {
                                        AsyncImage(
                                            model = externalUri,
                                            contentDescription = "Scanned Page ${position + 1}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                    val pdf = result?.pdf
                    if (pdf != null) {
                        val uri = pdf.uri
                        uri.path?.let { path ->
                            val externalUri = getExternalUri(context, path)
                            if (externalUri != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        enabled = !docSavingState.isLoading,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            viewModel.saveDocument(context = context, uri = externalUri)
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = "Save PDF")
                                            if (docSavingState.isLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                    OutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = { result = null }
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }
                }

                documents.isEmpty() -> {
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
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.document_scanner),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Scanned Documents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scan receipts, documents, or notes to save them as high quality PDFs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
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
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.photo_camera),
                                    contentDescription = null
                                )
                                Text("Scan Document")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                        items(documents, key = { it.id }) { doc ->
                            DocumentItem(
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
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentItem(
    documentScans: DocumentScans,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.pdf),
                    contentDescription = "PDF Document",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = documentScans.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.access_time),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = documentScans.timeMillis.toDateTimeString(pattern = "dd MMM yyyy, hh:mm a"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = "Share PDF",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = "Delete PDF",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
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