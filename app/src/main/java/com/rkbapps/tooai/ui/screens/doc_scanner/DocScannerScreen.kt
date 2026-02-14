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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.rkbapps.tooai.utils.getActivity
import com.rkbapps.tooai.utils.toDateTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DocScannerScreen(backStack: SnapshotStateList<Any>,viewModel: DocumentScannerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val activity =  context.getActivity() as Activity

    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val docSavingState by viewModel.docSavingState.collectAsStateWithLifecycle()


    var documentToDelete by remember { mutableStateOf<DocumentScans?>(null) }
    var result by rememberSaveable {
        mutableStateOf<GmsDocumentScanningResult?>(null)
    }

    val scannerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            handelScanActivityResult(activityResult,context){
                Log.d("Result",it.toString())
                result = it
            }
        }


    LaunchedEffect(docSavingState.error) {
        if (docSavingState.error != null) {
            withContext(Dispatchers.Main){
                Toast.makeText(context, docSavingState.error, Toast.LENGTH_SHORT)
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
            AnimatedVisibility(
                visible = result==null
            ) {
                FloatingActionButton(
                    onClick = {
                        viewModel.startScan(
                            activity = activity,
                            onScanResult = { intentSenderRequest ->
                                scannerLauncher.launch(intentSenderRequest)
                            },
                            onScanError = {error->
                                Toast.makeText(context,error.localizedMessage,Toast.LENGTH_SHORT)
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
                            contentDescription = "Scan"
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


            documentToDelete?.let {
                AlertDialog(
                    onDismissRequest = {
                        documentToDelete = null
                    },
                    text = {
                        Text("Are you sure you want to delete ${it.title}?")
                    },
                    title = {
                        Text("Delete Document")
                    },
                    confirmButton = {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteDocument(context,it)
                                documentToDelete = null
                            }
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                documentToDelete = null
                            }
                        ){
                            Text("Cancel")
                        }
                    }
                )
            }

            when{

                result != null->{

                    LaunchedEffect(docSavingState.data) {
                        if (docSavingState.data!=null){
                            result = null
                        }
                    }

                    Text(
                        text = "Final Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium
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
                                uri.path.let { path ->
                                    if (path != null) {
                                        val externalUri = getExternalUri(context, path)
                                        if (externalUri != null) {
                                            AsyncImage(
                                                model = externalUri,
                                                contentDescription = "",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                }

                            }
                        }
                    }
                    val pdf = result?.pdf
                    if (pdf != null) {
                        val uri = pdf.uri
                        uri.path.let { path ->
                            if (path != null) {
                                val externalUri = getExternalUri(context, path)
                                if (externalUri != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            enabled = !docSavingState.isLoading,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            onClick = {
                                                viewModel.saveDocument(context = context, uri = externalUri)
                                            }) {

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(text = "Save PDF")
                                                if (docSavingState.isLoading){
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                }
                                            }
                                        }
                                        OutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                result = null
                                            }
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                documents.isEmpty() ->{

                    Text("No documents found")
                    OutlinedButton(
                        onClick = {
                            viewModel.startScan(
                                activity = activity,
                                onScanResult = { intentSenderRequest ->
                                    scannerLauncher.launch(intentSenderRequest)
                                },
                                onScanError = {error->
                                    Toast.makeText(context,error.localizedMessage,Toast.LENGTH_SHORT)
                                }
                            )
                        }
                    ) {
                        Text("Scan document")
                    }
                }
                else ->{
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(documents){doc->
                            DocumentItem(doc, onDelete = {
                                documentToDelete = doc
                            }) {
                                try {
                                    Log.d("Opening URI",doc.path,)
                                    openPdf(context,doc.path)
                                }catch (e: Exception){
                                    Toast.makeText(context,e.localizedMessage,Toast.LENGTH_SHORT).show()
                                    Log.e("Opening URI",e.localizedMessage,e)
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
private fun DocumentItem(
    documentScans: DocumentScans,
    onDelete:()-> Unit,
    onClick:()-> Unit
){

    OutlinedCard(
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.pdf),
                contentDescription = "PDF icon",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(documentScans.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = documentScans.timeMillis.toDateTimeString(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    "Delete PDF"
                )
            }
        }
    }
    
    

}




private fun handelScanActivityResult(
    activityResult: ActivityResult,
    context: Context,
    onResult:(GmsDocumentScanningResult?)-> Unit
) {
    val resultCode = activityResult.resultCode
    val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
    when (resultCode) {
        Activity.RESULT_OK if result != null -> {
            onResult(result)
        }
        Activity.RESULT_CANCELED -> {
            //Toast.makeText(context, "Canceled by user.", Toast.LENGTH_SHORT).show()
        }
        else -> {
            Toast.makeText(context, "Failed to scan.", Toast.LENGTH_SHORT).show()
        }
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

private fun getExternalUri(context: Context, path: String): Uri? {
    return FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.provider",
        File(path)
    )
}