package com.rkbapps.tools.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.rkbapps.tools.db.QrScan
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


        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "QR Scanner") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navigator!!.pop() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "navigation up",
                                tint = MaterialTheme.colorScheme.onPrimary

                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "History")
                if (qrScanHistoryList.value.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
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
                            viewModel.addNewQrScan(
                                QrScan(
                                    id = 0,
                                    displayValue = result.displayValue,
                                    rawVale = result.rawValue,
                                    format = result.format,
                                    valueType = result.valueType,
                                    timeMillis = System.currentTimeMillis()
                                )
                            )
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
        }


    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HistoryItems(
        item: QrScan,
        onClick: () -> Unit
    ) {
        OutlinedCard(onClick = {
            onClick()
        },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (item.valueType) {
                        Barcode.TYPE_WIFI -> {
                            Icons.Default.Wifi
                        }

                        Barcode.TYPE_URL -> {
                            Icons.Default.Link
                        }

                        Barcode.TYPE_SMS -> {
                            Icons.Default.Sms
                        }

                        Barcode.TYPE_PHONE -> {
                            Icons.Default.PhoneAndroid
                        }

                        Barcode.TYPE_GEO -> {
                            Icons.Default.LocationOn
                        }

                        Barcode.TYPE_CONTACT_INFO -> {
                            Icons.Default.Contacts
                        }

                        else -> {
                            Icons.Default.QrCode
                        }
                    }, contentDescription = "",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Text(text = item.displayValue ?: "", modifier = Modifier.weight(1f))
            }
        }
    }


    private fun getSuccessfulMessage(barcode: Barcode): String {
        return String.format(
            Locale.getDefault(),
            "\bDisplay Value: %s\nRaw Value: %s\nFormat: %s\nValue Type: %s",
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