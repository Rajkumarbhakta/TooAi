package com.rkbapps.tooai.constants

import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions

object ScanModes {
    const val FULL_MODE = GmsDocumentScannerOptions.SCANNER_MODE_FULL
    const val BASE_MODE = GmsDocumentScannerOptions.SCANNER_MODE_BASE
    const val BASE_MODE_WITH_FILTER = GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER
}