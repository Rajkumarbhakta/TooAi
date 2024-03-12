package com.rkbapps.tools.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.rkbapps.tools.R
import com.rkbapps.tools.db.entity.QrScan
import com.rkbapps.tools.viewmodels.BarcodeViewModel
import java.util.Locale

class BarcodeScanScreen() : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.current
        val viewModel: BarcodeViewModel = getViewModel()

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
                    navigator!!.pop()
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
                            .fillMaxHeight(0.5f)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            ), horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Result", modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        SelectionContainer(
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = currentQrScan.value!!.getData(),
                                modifier = Modifier.fillMaxSize()
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

                            if (currentQrScan.value!!.id == 0L) {
                                Button(modifier = Modifier.weight(1f), onClick = {
                                    val clipboard: ClipboardManager =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText(
                                        "label",
                                        currentQrScan.value!!.getData()
                                    )
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text(text = "Copy")
                                }
                            } else {
                                Button(modifier = Modifier.weight(1f), onClick = {
                                    viewModel.deleteQrScan(currentQrScan.value!!)
                                    isDialogVisible.value = false
                                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
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

    @OptIn(ExperimentalMaterial3Api::class)
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

    private fun barcodeAction(valueType: Int, context: Context, rawValue: String) {
        try {
            when (valueType) {
                Barcode.TYPE_EMAIL -> {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse(rawValue)
                    context.startActivity(intent)
                }

                Barcode.TYPE_GEO -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(rawValue)
                    context.startActivity(intent)
                }

                Barcode.TYPE_PHONE -> {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse(rawValue)
                    context.startActivity(intent)
                }

                Barcode.TYPE_URL -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(rawValue)
                    context.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


}