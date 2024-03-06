package com.rkbapps.tools.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.screen.Screen
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.util.Locale

class BarcodeScanScreen() : Screen {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val barcodeResult = remember {
            mutableStateOf("")

        }

        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Button(onClick = {

                val options = GmsBarcodeScannerOptions.Builder()
                    .enableAutoZoom()

                val scanner = GmsBarcodeScanning.getClient(context, options.build())
                scanner.startScan().addOnSuccessListener { result ->

//                    result.valueType
                        barcodeResult.value = getSuccessfulMessage(result)

                    }.addOnFailureListener {
                        Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()

                    }.addOnCanceledListener {
                        Toast.makeText(context, "Canceled by user.", Toast.LENGTH_SHORT).show()
                    }

            }) {
                Text(text = "Scan")
            }

            Text(text = barcodeResult.value)

        }

    }


    private fun getSuccessfulMessage(barcode: Barcode): String {
        val barcodeValue =
            String.format(
                Locale.US,
                "Display Value: %s\nRaw Value: %s\nFormat: %s\nValue Type: %s",
                barcode.displayValue,
                barcode.rawValue,
                barcode.format,
                barcode.valueType
            )
        return barcodeValue
    }
}