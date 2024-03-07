package com.rkbapps.tools.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.rkbapps.tools.BuildConfig
import com.rkbapps.tools.constants.ScanModes
import com.rkbapps.tools.utils.getActivity
import java.io.File

class HomeScreen() : Screen {
    @Composable
    override fun Content() {
        val scanMode = rememberSaveable {
            mutableIntStateOf(ScanModes.FULL_MODE)
        }
        val resultInfo = rememberSaveable {
            mutableStateOf("")
        }
        val context = LocalContext.current
        val navigator = LocalNavigator.current
        val scannerLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
                handelScanActivityResult(activityResult, resultInfo, context)
            }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                try {
                    onScanButtonClick(
                        activity = context.getActivity() as Activity,
                        scannerLauncher = scannerLauncher,
                        resultInfo = resultInfo
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "Something went wrong.", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text(text = "Scan")
            }

            Button(onClick = {
                navigator!!.push(ImageSegmentationScreen())
            }) {
                Text(text = "Subject Segmentation")
            }

            Button(onClick = {
                navigator!!.push(BarcodeScanScreen())
            }) {
                Text(text = "Scan Barcode")
            }


            Text(
                text = resultInfo.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    private fun onScanButtonClick(
        activity: Activity,
        scanMode: MutableIntState = mutableIntStateOf(ScanModes.FULL_MODE),
        scannerLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
        resultInfo: MutableState<String>
    ) {
        val options =
            GmsDocumentScannerOptions.Builder()
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
                )
                .setGalleryImportAllowed(true)
                .setScannerMode(scanMode.intValue)

        GmsDocumentScanning.getClient(options.build())
            .getStartScanIntent(activity)
            .addOnSuccessListener { intentSender: IntentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener() { e: Exception ->
                resultInfo.value = e.message.toString()
            }
    }

    private fun handelScanActivityResult(
        activityResult: ActivityResult,
        resultInfo: MutableState<String>,
        context: Context
    ) {
        val resultCode = activityResult.resultCode
        val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        if (resultCode == Activity.RESULT_OK && result != null) {
            resultInfo.value = result.toString()
            val pages = result.pages
            result.pdf?.uri?.path?.let { path ->
                val externalUri = FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.provider",
                    File(path)
                )
                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_STREAM, externalUri)
                        type = "application/pdf"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                context.startActivity(Intent.createChooser(shareIntent, "share pdf"))
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            resultInfo.value = "Canceled by user."
        } else {
            resultInfo.value = "Failed to scan."
        }
    }
}














