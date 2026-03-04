package com.rkbapps.tooai.ui.screens.barcode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.QrScan
import com.rkbapps.tooai.ui.composabels.TopBar
import java.util.Locale

@Composable
fun BarcodeScanScreen(backStack: SnapshotStateList<Any>,
                      viewModel: BarcodeViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val barcodeResult = remember {
        mutableStateOf("")
    }
    val qrScanHistoryList = viewModel.qrScanList.collectAsState()

    val isDialogVisible = remember {
        mutableStateOf(false)
    }

    val currentQrScan = remember {
        mutableStateOf<QrScan?>(null)
    }


    Scaffold(
        topBar = {
            TopBar(title = "QR Scanner") {
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
            Text(
                text = "History",
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge
            )
            if (qrScanHistoryList.value.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), contentAlignment = Alignment.Center
                ) {
                    Text(text = "Nothing here.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(
                        count = qrScanHistoryList.value.size,
                        key = { key ->
                            qrScanHistoryList.value[key].id
                        }
                    ) { position ->
                        HistoryItems(item = qrScanHistoryList.value[position]) {
                            currentQrScan.value = qrScanHistoryList.value[position]
                            isDialogVisible.value = true
                        }
                    }
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = {
                    val options = GmsBarcodeScannerOptions.Builder()
                        .enableAutoZoom()

                    val scanner = GmsBarcodeScanning.getClient(context, options.build())
                    scanner.startScan().addOnSuccessListener { result ->
                        //result.valueType
                        barcodeResult.value = getSuccessfulMessage(result)
                        val qrScan = QrScan(
                            id = 0,
                            displayValue = result.displayValue,
                            rawVale = result.rawValue,
                            format = result.format,
                            valueType = result.valueType,
                            timeMillis = System.currentTimeMillis()
                        )
                        currentQrScan.value = qrScan
                        isDialogVisible.value = true
                        viewModel.addNewQrScan(qrScan)
//                            barcodeAction(result.valueType, context, result.rawValue!!)
                    }.addOnFailureListener {
                        Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()
                        Log.e("BARCODESCANNER", "Barcode error", it)
                    }.addOnCanceledListener {
                        Toast.makeText(context, "Canceled by user.", Toast.LENGTH_SHORT).show()
                    }
                }) {
                Text(text = "Scan")
            }
//                Text(text = barcodeResult.value)
        }

        if (isDialogVisible.value && currentQrScan.value != null) {
            Dialog(onDismissRequest = { isDialogVisible.value = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Scan Result",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // DISPLAY VALUE
                    Text(
                        text = "DISPLAY VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    SelectionContainer {
                        Text(
                            text = currentQrScan.value?.displayValue ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }

                    // RAW VALUE
                    Text(
                        text = "RAW VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp)
                            .background(color = MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = currentQrScan.value?.rawVale ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FORMAT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = getFormatLabel(currentQrScan.value?.format ?: -1),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "VALUE TYPE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = getValueTypeLabel(currentQrScan.value?.valueType ?: -1),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { isDialogVisible.value = false },
                        ) {
                            Text(text = "Dismiss")
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (currentQrScan.value?.id == 0L) {
                                    // Copy logic
                                    val clipboard: ClipboardManager =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText(
                                        "label",
                                        currentQrScan.value?.displayValue ?: ""
                                    )
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Delete logic
                                    viewModel.deleteQrScan(currentQrScan.value!!)
                                    isDialogVisible.value = false
                                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Text(text = if (currentQrScan.value?.id == 0L) "Copy" else "Delete")
                        }
                    }
                }
            }
        }


    }
}


@Composable
fun HistoryItems(
    item: QrScan,
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
            Icon(
                painter = painterResource(
                    id = when (item.valueType) {
                        Barcode.TYPE_WIFI -> {
                            R.drawable.wifi
                        }

                        Barcode.TYPE_URL -> {
                            R.drawable.link
                        }

                        Barcode.TYPE_SMS -> {
                            R.drawable.sms
                        }

                        Barcode.TYPE_PHONE -> {
                            R.drawable.smartphone
                        }

                        Barcode.TYPE_GEO -> {
                            R.drawable.location
                        }

                        Barcode.TYPE_CONTACT_INFO -> {
                            R.drawable.contact
                        }

                        else -> {
                            R.drawable.qr_code
                        }
                    }
                ), contentDescription = "",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = item.displayValue ?: "", modifier = Modifier.weight(1f))
        }
    }
}

private fun getSuccessfulMessage(barcode: Barcode): String {
    return String.format(
        Locale.getDefault(),
        "Display Value: %s\nRaw Value: %s\nFormat: %s\nValue Type: %s",
        barcode.displayValue,
        barcode.rawValue,
        barcode.format,
        barcode.valueType
    )
}

private fun getFormatLabel(format: Int): String {
    return when (format) {
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        else -> "UNKNOWN"
    }
}

private fun getValueTypeLabel(type: Int): String {
    return when (type) {
        Barcode.TYPE_CONTACT_INFO -> "CONTACT_INFO"
        Barcode.TYPE_EMAIL -> "EMAIL"
        Barcode.TYPE_ISBN -> "ISBN"
        Barcode.TYPE_PHONE -> "PHONE"
        Barcode.TYPE_PRODUCT -> "PRODUCT"
        Barcode.TYPE_SMS -> "SMS"
        Barcode.TYPE_TEXT -> "TEXT"
        Barcode.TYPE_URL -> "URL"
        Barcode.TYPE_WIFI -> "WIFI"
        Barcode.TYPE_GEO -> "GEO"
        Barcode.TYPE_CALENDAR_EVENT -> "CALENDAR_EVENT"
        Barcode.TYPE_DRIVER_LICENSE -> "DRIVER_LICENSE"
        else -> "UNKNOWN"
    }
}
